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
        private static final boolean ENABLE_DECORATION = true;

        private final int centerX;
        private final int centerY;
        private final int centerZ;
        private final long shapeSeed;
        private final IslandPiece.IslandType islandType;
        private final int diameter;

        // ── Constructors ───────────────────────────────────────────────

        public Piece(int centerX, int centerY, int centerZ, long shapeSeed,
                     IslandPiece.IslandType islandType, int diameter, BoundingBox boundingBox) {
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
            this.islandType = IslandPiece.IslandType.fromOrdinal(nbt.getInt("IslandType"));
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

            // Use the dedicated IslandPiece logic carrier
            IslandPiece island = new IslandPiece(new BlockPos(centerX, centerY, centerZ), diameter, shapeSeed, islandType);

            RandomSource noiseRandom = RandomSource.create(this.shapeSeed ^ world.getSeed());
            NormalNoise shapeNoise = NormalNoise.create(noiseRandom, -3, new double[]{1.0});

            // Surface height noise (2D, for gentle rolling terrain)
            RandomSource surfaceNoiseRandom = RandomSource.create(this.shapeSeed ^ 69);
            NormalNoise surfaceNoise = NormalNoise.create(surfaceNoiseRandom, -2, new double[]{1.0, 0.5});

            // ── Generate island chunk ──
            island.generateChunk(world, minX, maxX, minY, maxY, minZ, maxZ,
                    shapeNoise, surfaceNoise, random, ENABLE_DECORATION);
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
