package net.ugi.wildsprout_skylands.world.gen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.ugi.wildsprout_skylands.world.gen.ModStructurePieceTypes;

import java.util.ArrayList;
import java.util.List;

public final class FloatingIslandGenerator {
    private FloatingIslandGenerator() {}

    // ═══════════════════════════════════════════════════════════════════
    //  Island Piece
    // ═══════════════════════════════════════════════════════════════════
    public static class Piece extends StructurePiece {

        // ── Toggle flags (set to false to disable in development) ──────
        private static final boolean ENABLE_ISLAND_PLACEMENT = true;
        private static final boolean ENABLE_VEGETATION = true;

        // ── Noise tuning ───────────────────────────────────────────────
        private static final double NOISE_SCALE = 0.09;
        private static final double NOISE_STRENGTH = 0.30;
        private static final double SURFACE_NOISE_SCALE = 0.06;
        private static final double SURFACE_NOISE_AMPLITUDE = 3.5;

        private final int centerX;
        private final int centerY;
        private final int centerZ;
        private final long shapeSeed;
        private final IslandType islandType;
        private final int diameter;

        public enum IslandType {
            BIG, MEDIUM, SMALL;

            public static IslandType fromOrdinal(int ordinal) {
                return values()[Mth.clamp(ordinal, 0, values().length - 1)];
            }
        }

        // ── Constructors ───────────────────────────────────────────────

        public Piece(int centerX, int centerY, int centerZ, long shapeSeed,
                     IslandType islandType, int diameter, BoundingBox boundingBox) {
            super(ModStructurePieceTypes.FLOATING_ISLAND_PIECE.get(), 0, boundingBox);
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.shapeSeed = shapeSeed;
            this.islandType = islandType;
            this.diameter = diameter;
        }

        public Piece(CompoundTag nbt) {
            super(ModStructurePieceTypes.FLOATING_ISLAND_PIECE.get(), nbt);
            this.centerX = nbt.getInt("CenterX");
            this.centerY = nbt.getInt("CenterY");
            this.centerZ = nbt.getInt("CenterZ");
            this.shapeSeed = nbt.getLong("ShapeSeed");
            this.islandType = IslandType.fromOrdinal(nbt.getInt("IslandType"));
            this.diameter = nbt.getInt("Diameter");
        }

        // ── Bounding box ──────────────────────────────────────────────

        public static BoundingBox createBoundingBox(int centerX, int centerY, int centerZ, int diameter) {
            int radius = (diameter / 2) + 4; // a little padding
            int up = Math.max(8, diameter / 3);
            int down = Math.max(10, (int)(diameter * 0.8));
            return new BoundingBox(
                    centerX - radius, centerY - down, centerZ - radius,
                    centerX + radius, centerY + up + 6, centerZ + radius
            );
        }

        // ── NBT ───────────────────────────────────────────────────────

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
            nbt.putInt("CenterX", this.centerX);
            nbt.putInt("CenterY", this.centerY);
            nbt.putInt("CenterZ", this.centerZ);
            nbt.putLong("ShapeSeed", this.shapeSeed);
            nbt.putInt("IslandType", this.islandType.ordinal());
            nbt.putInt("Diameter", this.diameter);
        }

        // ── postProcess ───────────────────────────────────────────────

        @Override
        public void postProcess(
                WorldGenLevel world,
                StructureManager structureManager,
                ChunkGenerator chunkGenerator,
                RandomSource random,
                BoundingBox chunkBox,
                ChunkPos chunkPos,
                BlockPos pivot
        ) {
            if (!ENABLE_ISLAND_PLACEMENT) return;

            BoundingBox islandBox = this.getBoundingBox();
            int minX = Math.max(islandBox.minX(), chunkBox.minX());
            int maxX = Math.min(islandBox.maxX(), chunkBox.maxX());
            int minY = Math.max(islandBox.minY(), chunkBox.minY());
            int maxY = Math.min(islandBox.maxY(), chunkBox.maxY());
            int minZ = Math.max(islandBox.minZ(), chunkBox.minZ());
            int maxZ = Math.min(islandBox.maxZ(), chunkBox.maxZ());

            if (minX > maxX || minY > maxY || minZ > maxZ) return;

            // Create deterministic noise from shapeSeed
            RandomSource shapeRandom = RandomSource.create(this.shapeSeed);
            List<Lobe> lobes = createLobes(shapeRandom, this.diameter, this.islandType);

            RandomSource noiseRandom = RandomSource.create(this.shapeSeed ^ world.getSeed());
            NormalNoise shapeNoise = NormalNoise.create(noiseRandom, -3, new double[]{1.0});

            // Surface height noise (2D, for gentle rolling terrain)
            RandomSource surfaceNoiseRandom = RandomSource.create(this.shapeSeed ^ 0xCAFEBABEL);
            NormalNoise surfaceNoise = NormalNoise.create(surfaceNoiseRandom, -2, new double[]{1.0, 0.5});

            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
            int radius = this.diameter / 2;

            // Track surface blocks for vegetation pass
            List<BlockPos> surfaceBlocks = new ArrayList<>();

            // ── Place island blocks (top-down for correct surface detection) ──
            for (int y = maxY; y >= minY; y--) {
                int localY = y - this.centerY;
                for (int x = minX; x <= maxX; x++) {
                    int localX = x - this.centerX;
                    for (int z = minZ; z <= maxZ; z++) {
                        int localZ = z - this.centerZ;

                        // Quick radius cull
                        double horizDistSq = (localX * localX) + (localZ * localZ);
                        if (horizDistSq > (radius + 4.0) * (radius + 4.0)) continue;

                        if (!shouldFill(x, y, z, localX, localY, localZ, lobes, shapeNoise,
                                surfaceNoise, radius)) {
                            continue;
                        }

                        mutablePos.set(x, y, z);
                        BlockState existing = world.getBlockState(mutablePos);
                        if (!existing.isAir() && !existing.canBeReplaced()) continue;

                        int depth = getDepthFromSurface(x, y, z, localX, localY, localZ,
                                lobes, shapeNoise, surfaceNoise, radius);
                        BlockState state;
                        if (depth == 0) {
                            state = Blocks.GRASS_BLOCK.defaultBlockState();
                            surfaceBlocks.add(new BlockPos(x, y, z));
                        } else if (depth < 5) {
                            state = Blocks.DIRT.defaultBlockState();
                        } else {
                            int stoneVariant = Math.abs(x * 31 + z * 17 + y * 13) % 8;
                            state = stoneVariant == 0
                                    ? Blocks.ANDESITE.defaultBlockState()
                                    : Blocks.STONE.defaultBlockState();
                        }

                        world.setBlock(mutablePos, state, 2);
                    }
                }
            }

            // ── Vegetation pass ───────────────────────────────────────────
            if (ENABLE_VEGETATION && (islandType == IslandType.BIG || islandType == IslandType.MEDIUM)) {
                placeVegetation(world, mutablePos, surfaceBlocks, random, radius);
            } else if (ENABLE_VEGETATION && islandType == IslandType.SMALL) {
                // Small islands get sparse grass only
                placeSparseVegetation(world, mutablePos, surfaceBlocks, random);
            }
        }

        // ── Density / fill logic ──────────────────────────────────────

        private boolean shouldFill(int worldX, int worldY, int worldZ,
                                   int localX, int localY, int localZ,
                                   List<Lobe> lobes, NormalNoise shapeNoise,
                                   NormalNoise surfaceNoise, int radius) {
            // Surface height variation (gentle rolling terrain)
            double surfaceHeight = surfaceNoise.getValue(
                    worldX * SURFACE_NOISE_SCALE, 0, worldZ * SURFACE_NOISE_SCALE
            ) * SURFACE_NOISE_AMPLITUDE;

            // Edge taper: as we approach the rim, surface drops
            double horizDist = Math.sqrt((localX * localX) + (localZ * localZ));
            double edgeFactor = horizDist / Math.max(1.0, radius);
            double taperHeight = 0;
            if (edgeFactor > 0.6) {
                taperHeight = -((edgeFactor - 0.6) / 0.4) * 3.0; // drops up to 3 blocks at the edge
            }

            double effectiveTopY = this.centerY + surfaceHeight + taperHeight;

            // Above the top surface → air
            if (worldY > effectiveTopY + 0.5) return false;

            // Underside density
            double density = getCompositeDensity(localX, localY, localZ, lobes);
            double noiseValue = shapeNoise.getValue(
                    worldX * NOISE_SCALE, worldY * NOISE_SCALE * 0.5, worldZ * NOISE_SCALE
            );
            return density + (noiseValue * NOISE_STRENGTH) > 0.0;
        }

        private int getDepthFromSurface(int worldX, int worldY, int worldZ,
                                        int localX, int localY, int localZ,
                                        List<Lobe> lobes, NormalNoise shapeNoise,
                                        NormalNoise surfaceNoise, int radius) {
            for (int step = 1; step <= 6; step++) {
                if (!shouldFill(worldX, worldY + step, worldZ,
                        localX, localY + step, localZ,
                        lobes, shapeNoise, surfaceNoise, radius)) {
                    return step - 1;
                }
            }
            return 6;
        }

        // ── Lobe generation (scaled to diameter) ──────────────────────

        private static List<Lobe> createLobes(RandomSource random, int diameter, IslandType type) {
            List<Lobe> lobes = new ArrayList<>();
            double scale = diameter / 30.0; // normalize to ~30 block reference diameter

            int lobeCount;
            switch (type) {
                case BIG -> lobeCount = 3 + random.nextInt(4);    // 3-6
                case MEDIUM -> lobeCount = 2 + random.nextInt(3); // 2-4
                default -> lobeCount = 1 + random.nextInt(2);     // 1-2 for small
            }

            double currentX = 0.0;
            double currentY = -2.0 + random.nextInt(5);
            double currentZ = 0.0;
            double maxDrift = (diameter / 2.0) - 4.0;

            for (int i = 0; i < lobeCount; i++) {
                if (i > 0) {
                    double angle = random.nextDouble() * Math.PI * 2.0;
                    double distance = (4.0 + random.nextDouble() * 8.0) * scale;
                    currentX += Math.cos(angle) * distance;
                    currentZ += Math.sin(angle) * distance;
                    currentY += -3.0 + random.nextInt(7);

                    // Clamp to island radius
                    double horizontal = Math.sqrt(currentX * currentX + currentZ * currentZ);
                    if (horizontal > maxDrift) {
                        double clamp = maxDrift / horizontal;
                        currentX *= clamp;
                        currentZ *= clamp;
                    }
                }

                double radiusX = (5.0 + random.nextDouble() * 8.0) * scale;
                double radiusYTop = (3.0 + random.nextDouble() * 3.0) * scale;
                double radiusYBottom = (8.0 + random.nextDouble() * 7.0) * scale;
                double radiusZ = (5.0 + random.nextDouble() * 8.0) * scale;
                lobes.add(new Lobe(currentX, currentY, currentZ, radiusX, radiusYTop, radiusYBottom, radiusZ));

                // Hanging keel lobe for dripping effect
                if (random.nextInt(3) == 0) {
                    double keelY = currentY - (4.0 + random.nextDouble() * 6.0) * scale;
                    double keelRadiusX = radiusX * 0.40;
                    double keelRadiusZ = radiusZ * 0.40;
                    double keelTop = radiusYTop * 0.50;
                    double keelBottom = radiusYBottom * 0.85;
                    lobes.add(new Lobe(currentX, keelY, currentZ, keelRadiusX, keelTop, keelBottom, keelRadiusZ));
                }
            }

            return lobes;
        }

        private static double getCompositeDensity(int x, int y, int z, List<Lobe> lobes) {
            double density = Double.NEGATIVE_INFINITY;

            for (Lobe lobe : lobes) {
                double dx = (x - lobe.offsetX) / lobe.radiusX;
                double yOffset = y - lobe.offsetY;
                double yRadius = yOffset >= 0.0 ? lobe.radiusYTop : lobe.radiusYBottom;
                double dy = yOffset / yRadius;
                double dz = (z - lobe.offsetZ) / lobe.radiusZ;
                double value = 1.0 - (dx * dx + dy * dy + dz * dz);

                // Underside weight for droopy feel
                if (yOffset < 0.0) {
                    value += Math.min(0.26, (-yOffset / lobe.radiusYBottom) * 0.24);
                }

                density = Math.max(density, value);
            }

            // Connectors between adjacent lobes
            for (int i = 1; i < lobes.size(); i++) {
                density = Math.max(density, connectorDensity(x, y, z, lobes.get(i - 1), lobes.get(i)));
            }

            // Global vertical bias: flatten top, favor deep undersides
            if (y > 10) {
                density -= (y - 10) * 0.11;
            }
            if (y < -8) {
                density += Math.min(0.32, (-8 - y) * 0.03);
            }

            return density;
        }

        private static double connectorDensity(int x, int y, int z, Lobe a, Lobe b) {
            double segmentX = b.offsetX - a.offsetX;
            double segmentY = b.offsetY - a.offsetY;
            double segmentZ = b.offsetZ - a.offsetZ;
            double lengthSquared = segmentX * segmentX + segmentY * segmentY + segmentZ * segmentZ;

            if (lengthSquared < 0.0001) return Double.NEGATIVE_INFINITY;

            double pointX = x - a.offsetX;
            double pointY = y - a.offsetY;
            double pointZ = z - a.offsetZ;
            double projection = (pointX * segmentX + pointY * segmentY + pointZ * segmentZ) / lengthSquared;
            double t = Mth.clamp(projection, 0.0, 1.0);

            double nearX = a.offsetX + segmentX * t;
            double nearY = a.offsetY + segmentY * t;
            double nearZ = a.offsetZ + segmentZ * t;

            double radiusX = Mth.lerp(t, a.radiusX, b.radiusX) * 0.42;
            double radiusYTop = Mth.lerp(t, a.radiusYTop, b.radiusYTop) * 0.45;
            double radiusYBottom = Mth.lerp(t, a.radiusYBottom, b.radiusYBottom) * 0.68;
            double radiusZ = Mth.lerp(t, a.radiusZ, b.radiusZ) * 0.42;

            double dx = (x - nearX) / radiusX;
            double yOffset = y - nearY;
            double yRadius = yOffset >= 0.0 ? radiusYTop : radiusYBottom;
            double dy = yOffset / yRadius;
            double dz = (z - nearZ) / radiusZ;

            double connector = 0.92 - (dx * dx + dy * dy + dz * dz);
            if (yOffset < 0.0) {
                connector += Math.min(0.16, (-yOffset / radiusYBottom) * 0.15);
            }

            return connector;
        }

        // ── Vegetation ────────────────────────────────────────────────

        private static void placeVegetation(WorldGenLevel world, BlockPos.MutableBlockPos pos,
                                            List<BlockPos> surfaceBlocks, RandomSource random,
                                            int islandRadius) {
            for (BlockPos surface : surfaceBlocks) {
                // Edge avoidance: skip if at the rim
                int localX = surface.getX();
                int localZ = surface.getZ();
                // Check if the block above is air (necessary for placing vegetation)
                pos.set(surface.getX(), surface.getY() + 1, surface.getZ());
                if (!world.getBlockState(pos).isAir()) continue;

                int roll = random.nextInt(100);
                if (roll < 29) {
                    // 29% — nothing
                    continue;
                } else if (roll < 64) {
                    // 35% — short grass
                    world.setBlock(pos, Blocks.SHORT_GRASS.defaultBlockState(), 2);
                } else if (roll < 76) {
                    // 12% — tall grass (two-block)
                    BlockPos.MutableBlockPos upper = new BlockPos.MutableBlockPos(
                            surface.getX(), surface.getY() + 2, surface.getZ());
                    if (world.getBlockState(upper).isAir()) {
                        world.setBlock(pos, Blocks.TALL_GRASS.defaultBlockState(), 2);
                    }
                } else if (roll < 84) {
                    // 8% — fern
                    world.setBlock(pos, Blocks.FERN.defaultBlockState(), 2);
                } else if (roll < 92) {
                    // 8% — flowers
                    BlockState flower = switch (random.nextInt(4)) {
                        case 0 -> Blocks.POPPY.defaultBlockState();
                        case 1 -> Blocks.DANDELION.defaultBlockState();
                        case 2 -> Blocks.CORNFLOWER.defaultBlockState();
                        default -> Blocks.AZURE_BLUET.defaultBlockState();
                    };
                    world.setBlock(pos, flower, 2);
                } else if (roll < 97) {
                    // 5% — moss carpet
                    world.setBlock(pos, Blocks.MOSS_CARPET.defaultBlockState(), 2);
                } else {
                    // 3% — simple procedural tree (only if space allows)
                    placeSmallTree(world, surface, random);
                }
            }
        }

        private static void placeSparseVegetation(WorldGenLevel world, BlockPos.MutableBlockPos pos,
                                                   List<BlockPos> surfaceBlocks, RandomSource random) {
            for (BlockPos surface : surfaceBlocks) {
                pos.set(surface.getX(), surface.getY() + 1, surface.getZ());
                if (!world.getBlockState(pos).isAir()) continue;

                if (random.nextInt(100) < 30) {
                    world.setBlock(pos, Blocks.SHORT_GRASS.defaultBlockState(), 2);
                }
            }
        }

        private static void placeSmallTree(WorldGenLevel world, BlockPos base, RandomSource random) {
            int trunkHeight = 3 + random.nextInt(3); // 3-5
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            // Check we have enough vertical clearance
            for (int dy = 1; dy <= trunkHeight + 2; dy++) {
                pos.set(base.getX(), base.getY() + dy, base.getZ());
                if (!world.getBlockState(pos).isAir() && !world.getBlockState(pos).canBeReplaced()) return;
            }

            // Place trunk
            for (int dy = 1; dy <= trunkHeight; dy++) {
                pos.set(base.getX(), base.getY() + dy, base.getZ());
                world.setBlock(pos, Blocks.OAK_LOG.defaultBlockState(), 2);
            }

            // Place leaf blob (sphere-ish around the top)
            int leafCenterY = base.getY() + trunkHeight;
            int leafRadius = 2;
            for (int dx = -leafRadius; dx <= leafRadius; dx++) {
                for (int dy = -1; dy <= 2; dy++) {
                    for (int dz = -leafRadius; dz <= leafRadius; dz++) {
                        int distSq = dx * dx + dy * dy + dz * dz;
                        if (distSq > leafRadius * leafRadius + 1) continue;
                        // Don't overwrite trunk
                        if (dx == 0 && dz == 0 && dy <= 0) continue;

                        pos.set(base.getX() + dx, leafCenterY + dy, base.getZ() + dz);
                        BlockState existing = world.getBlockState(pos);
                        if (existing.isAir() || existing.canBeReplaced()) {
                            world.setBlock(pos, Blocks.OAK_LEAVES.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }

        // ── Inner record ──────────────────────────────────────────────

        private static final class Lobe {
            final double offsetX, offsetY, offsetZ;
            final double radiusX, radiusYTop, radiusYBottom, radiusZ;

            Lobe(double offsetX, double offsetY, double offsetZ,
                 double radiusX, double radiusYTop, double radiusYBottom, double radiusZ) {
                this.offsetX = offsetX;
                this.offsetY = offsetY;
                this.offsetZ = offsetZ;
                this.radiusX = radiusX;
                this.radiusYTop = radiusYTop;
                this.radiusYBottom = radiusYBottom;
                this.radiusZ = radiusZ;
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Bridge Piece
    // ═══════════════════════════════════════════════════════════════════
    public static class BridgePiece extends StructurePiece {

        // ── Toggle flag ────────────────────────────────────────────────
        private static final boolean ENABLE_BRIDGES = true;

        private final BlockPos start;
        private final BlockPos end;
        private final int width; // 1, 2, or 3

        // ── Constructors ───────────────────────────────────────────────

        public BridgePiece(BlockPos start, BlockPos end, int width, BoundingBox boundingBox) {
            super(ModStructurePieceTypes.FLOATING_ISLAND_BRIDGE.get(), 0, boundingBox);
            this.start = start;
            this.end = end;
            this.width = Mth.clamp(width, 1, 3);
        }

        public BridgePiece(CompoundTag nbt) {
            super(ModStructurePieceTypes.FLOATING_ISLAND_BRIDGE.get(), nbt);
            this.start = new BlockPos(nbt.getInt("StartX"), nbt.getInt("StartY"), nbt.getInt("StartZ"));
            this.end = new BlockPos(nbt.getInt("EndX"), nbt.getInt("EndY"), nbt.getInt("EndZ"));
            this.width = nbt.getInt("Width");
        }

        // ── Bounding box ──────────────────────────────────────────────

        public static BoundingBox createBoundingBox(BlockPos start, BlockPos end) {
            // Compute sag to ensure the bounding box encompasses the lowest point
            double dx = end.getX() - start.getX();
            double dz = end.getZ() - start.getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            int sagBlocks = Math.max(4, (int)(horizontalDist * 0.15));

            return new BoundingBox(
                    Math.min(start.getX(), end.getX()) - 3,
                    Math.min(start.getY(), end.getY()) - sagBlocks - 6,
                    Math.min(start.getZ(), end.getZ()) - 3,
                    Math.max(start.getX(), end.getX()) + 3,
                    Math.max(start.getY(), end.getY()) + 2,
                    Math.max(start.getZ(), end.getZ()) + 3
            );
        }

        // ── NBT ───────────────────────────────────────────────────────

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
            nbt.putInt("StartX", this.start.getX());
            nbt.putInt("StartY", this.start.getY());
            nbt.putInt("StartZ", this.start.getZ());
            nbt.putInt("EndX", this.end.getX());
            nbt.putInt("EndY", this.end.getY());
            nbt.putInt("EndZ", this.end.getZ());
            nbt.putInt("Width", this.width);
        }

        // ── postProcess ───────────────────────────────────────────────

        @Override
        public void postProcess(
                WorldGenLevel world,
                StructureManager structureManager,
                ChunkGenerator chunkGenerator,
                RandomSource random,
                BoundingBox chunkBox,
                ChunkPos chunkPos,
                BlockPos pivot
        ) {
            if (!ENABLE_BRIDGES) return;

            BoundingBox bridgeBox = this.getBoundingBox();
            int minX = Math.max(bridgeBox.minX(), chunkBox.minX());
            int maxX = Math.min(bridgeBox.maxX(), chunkBox.maxX());
            int minY = Math.max(bridgeBox.minY(), chunkBox.minY());
            int maxY = Math.min(bridgeBox.maxY(), chunkBox.maxY());
            int minZ = Math.max(bridgeBox.minZ(), chunkBox.minZ());
            int maxZ = Math.min(bridgeBox.maxZ(), chunkBox.maxZ());

            if (minX > maxX || minY > maxY || minZ > maxZ) return;

            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
            placeBridge(world, mutablePos, this.start, this.end, this.width,
                    minX, maxX, minY, maxY, minZ, maxZ, random);
        }

        // ── Bridge placement ──────────────────────────────────────────

        private static void placeBridge(
                WorldGenLevel world, BlockPos.MutableBlockPos mutablePos,
                BlockPos start, BlockPos end, int width,
                int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
                RandomSource random
        ) {
            double dx = end.getX() - start.getX();
            double dy = end.getY() - start.getY();
            double dz = end.getZ() - start.getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);

            // Sag proportional to distance (realistic vine/rope droop)
            double sag = horizontalDist * 0.15;

            // Number of steps = max horizontal distance for smooth curve
            int steps = Math.max(1, (int)(horizontalDist * 1.5));

            // Perpendicular direction for bridge width
            double perpX = 0, perpZ = 0;
            if (horizontalDist > 0.01) {
                perpX = -dz / horizontalDist;
                perpZ = dx / horizontalDist;
            }

            // Deterministic random for consistent vine placement across chunk borders
            RandomSource vineRand = RandomSource.create(
                    (long)start.getX() * 73856093L ^ (long)start.getZ() * 19349663L ^ (long)end.getX());

            for (int step = 0; step <= steps; step++) {
                double t = (double) step / (double) steps;

                // Catenary-like curve: y(t) = lerp(t, startY, endY) - sag * 4 * t * (1-t)
                double curveX = Mth.lerp(t, start.getX(), end.getX());
                double curveY = Mth.lerp(t, start.getY(), end.getY()) - sag * 4.0 * t * (1.0 - t);
                double curveZ = Mth.lerp(t, start.getZ(), end.getZ());

                // Place moss blocks along the width
                int halfWidth = (width - 1) / 2;
                for (int w = -halfWidth; w <= halfWidth; w++) {
                    int px = Mth.floor(curveX + perpX * w);
                    int py = Mth.floor(curveY);
                    int pz = Mth.floor(curveZ + perpZ * w);

                    if (px < minX || px > maxX || py < minY || py > maxY || pz < minZ || pz > maxZ) continue;

                    mutablePos.set(px, py, pz);
                    // Only replace air — never overwrite island blocks
                    if (!world.getBlockState(mutablePos).isAir()) continue;

                    world.setBlock(mutablePos, Blocks.MOSS_BLOCK.defaultBlockState(), 2);

                    // Hanging vines below (~40% chance, 1-4 blocks long)
                    if (vineRand.nextInt(100) < 40) {
                        int vineLength = 1 + vineRand.nextInt(4);

                        // Pick a horizontal face for the vine to attach to.
                        // Vines need at least one horizontal face set to true.
                        // We pick based on the bridge direction so vines look natural.
                        BooleanProperty face;
                        if (Math.abs(dx) > Math.abs(dz)) {
                            face = dz >= 0 ? VineBlock.NORTH : VineBlock.SOUTH;
                        } else {
                            face = dx >= 0 ? VineBlock.WEST : VineBlock.EAST;
                        }

                        for (int v = 1; v <= vineLength; v++) {
                            int vy = py - v;
                            if (vy < minY) break;
                            mutablePos.set(px, vy, pz);
                            if (!world.getBlockState(mutablePos).isAir()) break;

                            // Vines chain: each vine block connects to the block above
                            BlockState vineState = Blocks.VINE.defaultBlockState()
                                    .setValue(face, true);
                            world.setBlock(mutablePos, vineState, 2);
                        }
                    }
                }
            }
        }
    }
}
