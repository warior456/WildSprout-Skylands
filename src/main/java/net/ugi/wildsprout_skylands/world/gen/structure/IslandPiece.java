package net.ugi.wildsprout_skylands.world.gen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.ArrayList;
import java.util.List;

public class IslandPiece {
    private static final double NOISE_SCALE = 0.09;
    private static final double NOISE_STRENGTH = 0.30;
    private static final double SURFACE_NOISE_SCALE = 0.06;
    private static final double SURFACE_NOISE_AMPLITUDE = 3.5;

    private final BlockPos center;
    private final int diameter;
    private final long shapeSeed;
    private final IslandType type;
    private final List<Lobe> lobes;

    public IslandPiece(BlockPos center, int diameter, long shapeSeed, IslandType type) {
        this.center = center;
        this.diameter = diameter;
        this.shapeSeed = shapeSeed;
        this.type = type;
        this.lobes = createLobes(RandomSource.create(shapeSeed), diameter, type);
    }

    public enum IslandType {
        BIG(true, 3, 6) {
            @Override
            void decorate(IslandPiece island, WorldGenLevel world, BlockPos.MutableBlockPos pos, List<BlockPos> surfaceBlocks, RandomSource random) {
                placeBigVegetation(world, pos, surfaceBlocks, random);
                placeBigFeature(world, pos, surfaceBlocks, random);
            }
        },
        MEDIUM(true, 2, 4) {
            @Override
            void decorate(IslandPiece island, WorldGenLevel world, BlockPos.MutableBlockPos pos, List<BlockPos> surfaceBlocks, RandomSource random) {
                placeMediumVegetation(world, pos, surfaceBlocks, random);
            }
        },
        SMALL(false, 1, 2) {
            @Override
            void decorate(IslandPiece island, WorldGenLevel world, BlockPos.MutableBlockPos pos, List<BlockPos> surfaceBlocks, RandomSource random) {
                // Small islands skip decoration for overhead
            }
        };

        private final boolean hasDecoration;
        private final int minLobes;
        private final int maxLobes;

        IslandType(boolean hasDecoration, int minLobes, int maxLobes) {
            this.hasDecoration = hasDecoration;
            this.minLobes = minLobes;
            this.maxLobes = maxLobes;
        }

        public static IslandType fromOrdinal(int ordinal) {
            return values()[Mth.clamp(ordinal, 0, values().length - 1)];
        }

        int lobeCount(RandomSource random) {
            return this.minLobes + random.nextInt(this.maxLobes - this.minLobes + 1);
        }

        boolean hasDecoration() {
            return this.hasDecoration;
        }

        abstract void decorate(IslandPiece island, WorldGenLevel world, BlockPos.MutableBlockPos pos, List<BlockPos> surfaceBlocks, RandomSource random);

        protected void placeBigVegetation(WorldGenLevel world, BlockPos.MutableBlockPos pos, List<BlockPos> surfaceBlocks, RandomSource random) {
            for (BlockPos surface : surfaceBlocks) {
                pos.set(surface.getX(), surface.getY() + 1, surface.getZ());
                if (!world.getBlockState(pos).isAir()) continue;

                int roll = random.nextInt(100);
                if (roll < 22) continue;
                if (roll < 52) {
                    world.setBlock(pos, Blocks.SHORT_GRASS.defaultBlockState(), 2);
                } else if (roll < 66) {
                    BlockPos.MutableBlockPos upper = new BlockPos.MutableBlockPos(surface.getX(), surface.getY() + 2, surface.getZ());
                    if (world.getBlockState(upper).isAir()) {
                        world.setBlock(pos, Blocks.TALL_GRASS.defaultBlockState(), 2);
                    }
                } else if (roll < 76) {
                    world.setBlock(pos, Blocks.FERN.defaultBlockState(), 2);
                } else if (roll < 88) {
                    BlockState flower = switch (random.nextInt(4)) {
                        case 0 -> Blocks.POPPY.defaultBlockState();
                        case 1 -> Blocks.DANDELION.defaultBlockState();
                        case 2 -> Blocks.CORNFLOWER.defaultBlockState();
                        default -> Blocks.AZURE_BLUET.defaultBlockState();
                    };
                    world.setBlock(pos, flower, 2);
                } else if (roll < 94) {
                    world.setBlock(pos, Blocks.MOSS_CARPET.defaultBlockState(), 2);
                } else {
                    placeSmallTree(world, surface, random);
                }
            }
        }

        protected void placeMediumVegetation(WorldGenLevel world, BlockPos.MutableBlockPos pos, List<BlockPos> surfaceBlocks, RandomSource random) {
            for (BlockPos surface : surfaceBlocks) {
                pos.set(surface.getX(), surface.getY() + 1, surface.getZ());
                if (!world.getBlockState(pos).isAir()) continue;

                int roll = random.nextInt(100);
                if (roll < 48) continue;
                if (roll < 82) {
                    world.setBlock(pos, Blocks.SHORT_GRASS.defaultBlockState(), 2);
                } else if (roll < 90) {
                    world.setBlock(pos, Blocks.FERN.defaultBlockState(), 2);
                } else {
                    BlockState flower = switch (random.nextInt(4)) {
                        case 0 -> Blocks.POPPY.defaultBlockState();
                        case 1 -> Blocks.DANDELION.defaultBlockState();
                        case 2 -> Blocks.CORNFLOWER.defaultBlockState();
                        default -> Blocks.AZURE_BLUET.defaultBlockState();
                    };
                    world.setBlock(pos, flower, 2);
                }
            }
        }

        protected void placeBigFeature(WorldGenLevel world, BlockPos.MutableBlockPos pos, List<BlockPos> surfaceBlocks, RandomSource random) {
            if (surfaceBlocks.isEmpty() || random.nextInt(4) != 0) return;
            BlockPos anchor = surfaceBlocks.get(random.nextInt(surfaceBlocks.size()));
            if (random.nextBoolean()) {
                placeSmallTree(world, anchor, random);
            } else {
                placeRockFeature(world, pos, anchor, random);
            }
        }

        protected void placeRockFeature(WorldGenLevel world, BlockPos.MutableBlockPos pos, BlockPos base, RandomSource random) {
            int radius = 1 + random.nextInt(2);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        int distSq = (dx * dx) + (dz * dz) + (dy * dy * 2);
                        if (distSq > radius * radius + 1) continue;
                        pos.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
                        BlockState existing = world.getBlockState(pos);
                        if (!existing.isAir() && !existing.canBeReplaced()) continue;
                        BlockState rock = random.nextInt(5) == 0 ? Blocks.ANDESITE.defaultBlockState() : Blocks.STONE.defaultBlockState();
                        world.setBlock(pos, rock, 2);
                    }
                }
            }
        }

        protected void placeSmallTree(WorldGenLevel world, BlockPos base, RandomSource random) {
            int trunkHeight = 3 + random.nextInt(3);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int dy = 1; dy <= trunkHeight + 2; dy++) {
                pos.set(base.getX(), base.getY() + dy, base.getZ());
                if (!world.getBlockState(pos).isAir() && !world.getBlockState(pos).canBeReplaced()) return;
            }
            for (int dy = 1; dy <= trunkHeight; dy++) {
                pos.set(base.getX(), base.getY() + dy, base.getZ());
                world.setBlock(pos, Blocks.OAK_LOG.defaultBlockState(), 2);
            }
            int leafCenterY = base.getY() + trunkHeight;
            int leafRadius = 2;
            for (int dx = -leafRadius; dx <= leafRadius; dx++) {
                for (int dy = -1; dy <= 2; dy++) {
                    for (int dz = -leafRadius; dz <= leafRadius; dz++) {
                        int distSq = dx * dx + dy * dy + dz * dz;
                        if (distSq > leafRadius * leafRadius + 1) continue;
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
    }

    public boolean shouldFill(int worldX, int worldY, int worldZ,
                             int localX, int localY, int localZ,
                             NormalNoise shapeNoise, NormalNoise surfaceNoise) {
        int radius = this.diameter / 2;
        double surfaceHeight = surfaceNoise.getValue(
                worldX * SURFACE_NOISE_SCALE, 0, worldZ * SURFACE_NOISE_SCALE
        ) * SURFACE_NOISE_AMPLITUDE;

        double horizDist = Math.sqrt((localX * localX) + (localZ * localZ));
        double edgeFactor = horizDist / Math.max(1.0, radius);
        double taperHeight = 0;
        if (edgeFactor > 0.6) {
            taperHeight = -((edgeFactor - 0.6) / 0.4) * 3.0;
        }

        double effectiveTopY = this.center.getY() + surfaceHeight + taperHeight;
        if (worldY > effectiveTopY + 0.5) return false;

        double density = getCompositeDensity(localX, localY, localZ);
        double noiseValue = shapeNoise.getValue(
                worldX * NOISE_SCALE, worldY * NOISE_SCALE * 0.5, worldZ * NOISE_SCALE
        );
        return density + (noiseValue * NOISE_STRENGTH) > 0.0;
    }

    public int getDepthFromSurface(int worldX, int worldY, int worldZ,
                                  int localX, int localY, int localZ,
                                  NormalNoise shapeNoise, NormalNoise surfaceNoise) {
        for (int step = 1; step <= 6; step++) {
            if (!shouldFill(worldX, worldY + step, worldZ, localX, localY + step, localZ, shapeNoise, surfaceNoise)) {
                return step - 1;
            }
        }
        return 6;
    }

    /**
     * Entry point for generating a chunk's worth of this island.
     * Encapsulates both block placement and decoration.
     */
    public void generateChunk(WorldGenLevel world,
                              int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
                              NormalNoise shapeNoise, NormalNoise surfaceNoise,
                              RandomSource random, boolean shouldDecorate) {
        List<BlockPos> surfaceBlocks = placeBlocks(world, minX, maxX, minY, maxY, minZ, maxZ, shapeNoise, surfaceNoise);

        if (shouldDecorate && this.type.hasDecoration()) {
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
            this.type.decorate(this, world, mutablePos, surfaceBlocks, random);
        }
    }

    private List<BlockPos> placeBlocks(WorldGenLevel world,
                                      int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
                                      NormalNoise shapeNoise, NormalNoise surfaceNoise) {
        List<BlockPos> surfaceBlocks = new ArrayList<>();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int radius = this.diameter / 2;

        for (int y = maxY; y >= minY; y--) {
            int localY = y - this.center.getY();
            for (int x = minX; x <= maxX; x++) {
                int localX = x - this.center.getX();
                for (int z = minZ; z <= maxZ; z++) {
                    int localZ = z - this.center.getZ();

                    // Quick radius cull
                    double horizDistSq = (localX * localX) + (localZ * localZ);
                    if (horizDistSq > (radius + 4.0) * (radius + 4.0)) continue;

                    if (!shouldFill(x, y, z, localX, localY, localZ, shapeNoise, surfaceNoise)) {
                        continue;
                    }

                    mutablePos.set(x, y, z);
                    BlockState existing = world.getBlockState(mutablePos);
                    if (!existing.isAir() && !existing.canBeReplaced()) continue;

                    int depth = getDepthFromSurface(x, y, z, localX, localY, localZ, shapeNoise, surfaceNoise);
                    BlockState state;
                    if (depth == 0) {
                        state = Blocks.GRASS_BLOCK.defaultBlockState();
                        surfaceBlocks.add(new BlockPos(x, y, z));
                    } else if (depth < 5) {
                        state = Blocks.DIRT.defaultBlockState();
                    } else {
                        // Deterministic stone variation
                        int stoneVariant = Math.abs(x * 31 + z * 17 + y * 13) % 8;
                        state = stoneVariant == 0
                                ? Blocks.ANDESITE.defaultBlockState()
                                : Blocks.STONE.defaultBlockState();
                    }

                    world.setBlock(mutablePos, state, 2);
                }
            }
        }
        return surfaceBlocks;
    }

    private double getCompositeDensity(int x, int y, int z) {
        double density = Double.NEGATIVE_INFINITY;
        for (Lobe lobe : lobes) {
            double dx = (x - lobe.offsetX) / lobe.radiusX;
            double yOffset = y - lobe.offsetY;
            double yRadius = yOffset >= 0.0 ? lobe.radiusYTop : lobe.radiusYBottom;
            double dy = yOffset / yRadius;
            double dz = (z - lobe.offsetZ) / lobe.radiusZ;
            double value = 1.0 - (dx * dx + dy * dy + dz * dz);
            if (yOffset < 0.0) {
                value += Math.min(0.26, (-yOffset / lobe.radiusYBottom) * 0.24);
            }
            density = Math.max(density, value);
        }
        for (int i = 1; i < lobes.size(); i++) {
            density = Math.max(density, connectorDensity(x, y, z, lobes.get(i - 1), lobes.get(i)));
        }
        if (y > 10) density -= (y - 10) * 0.11;
        if (y < -8) density += Math.min(0.32, (-8 - y) * 0.03);
        return density;
    }

    private double connectorDensity(int x, int y, int z, Lobe a, Lobe b) {
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

    private static List<Lobe> createLobes(RandomSource random, int diameter, IslandType type) {
        List<Lobe> lobes = new ArrayList<>();
        double scale = diameter / 30.0;
        int lobeCount = type.lobeCount(random);
        double currentX = 0.0, currentY = -2.0 + random.nextInt(5), currentZ = 0.0;
        double maxDrift = (diameter / 2.0) - 4.0;
        for (int i = 0; i < lobeCount; i++) {
            if (i > 0) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double distance = (4.0 + random.nextDouble() * 8.0) * scale;
                currentX += Math.cos(angle) * distance;
                currentZ += Math.sin(angle) * distance;
                currentY += -3.0 + random.nextInt(7);
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
            if (random.nextInt(3) == 0) {
                double keelY = currentY - (4.0 + random.nextDouble() * 6.0) * scale;
                double keelRadiusX = radiusX * 0.40, keelRadiusZ = radiusZ * 0.40;
                double keelTop = radiusYTop * 0.50, keelBottom = radiusYBottom * 0.85;
                lobes.add(new Lobe(currentX, keelY, currentZ, keelRadiusX, keelTop, keelBottom, keelRadiusZ));
            }
        }
        return lobes;
    }

    public void decorate(WorldGenLevel world, BlockPos.MutableBlockPos pos, List<BlockPos> surfaceBlocks, RandomSource random) {
        if (this.type.hasDecoration()) {
            this.type.decorate(this, world, pos, surfaceBlocks, random);
        }
    }

    public int getDiameter() { return diameter; }
    public BlockPos getCenter() { return center; }
    public IslandType getType() { return type; }

    private static final class Lobe {
        final double offsetX, offsetY, offsetZ;
        final double radiusX, radiusYTop, radiusYBottom, radiusZ;
        Lobe(double offsetX, double offsetY, double offsetZ, double radiusX, double radiusYTop, double radiusYBottom, double radiusZ) {
            this.offsetX = offsetX; this.offsetY = offsetY; this.offsetZ = offsetZ;
            this.radiusX = radiusX; this.radiusYTop = radiusYTop; this.radiusYBottom = radiusYBottom; this.radiusZ = radiusZ;
        }
    }
}
