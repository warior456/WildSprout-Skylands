package net.ugi.wildsprout_skylands.world.gen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.ugi.wildsprout_skylands.world.gen.ModStructurePieceTypes;

public final class GlassBallClusterGenerator {
    private GlassBallClusterGenerator() {
    }

    public static class Piece extends StructurePiece {
        private static final int MAX_HORIZONTAL_RADIUS = 28;
        private static final int MAX_UP = 16;
        private static final int MAX_DOWN = 12;
        private static final int BALL_RADIUS = 4;

        private final int centerX;
        private final int centerY;
        private final int centerZ;
        private final long shapeSeed;

        public Piece(int centerX, int centerY, int centerZ, long shapeSeed, BoundingBox boundingBox) {
            super(ModStructurePieceTypes.GLASS_BALL_CLUSTER_PIECE.get(), 0, boundingBox);
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.shapeSeed = shapeSeed;
        }

        public Piece(CompoundTag nbt) {
            super(ModStructurePieceTypes.GLASS_BALL_CLUSTER_PIECE.get(), nbt);
            this.centerX = nbt.getInt("CenterX");
            this.centerY = nbt.getInt("CenterY");
            this.centerZ = nbt.getInt("CenterZ");
            this.shapeSeed = nbt.getLong("ShapeSeed");
        }

        public static BoundingBox createBoundingBox(int centerX, int centerY, int centerZ) {
            return new BoundingBox(
                    centerX - MAX_HORIZONTAL_RADIUS, centerY - MAX_DOWN, centerZ - MAX_HORIZONTAL_RADIUS,
                    centerX + MAX_HORIZONTAL_RADIUS, centerY + MAX_UP, centerZ + MAX_HORIZONTAL_RADIUS
            );
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
            nbt.putInt("CenterX", this.centerX);
            nbt.putInt("CenterY", this.centerY);
            nbt.putInt("CenterZ", this.centerZ);
            nbt.putLong("ShapeSeed", this.shapeSeed);
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureManager structureManager, ChunkGenerator chunkGenerator,
                                RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
            BoundingBox islandBox = this.getBoundingBox();
            int minX = Math.max(islandBox.minX(), chunkBox.minX());
            int maxX = Math.min(islandBox.maxX(), chunkBox.maxX());
            int minY = Math.max(islandBox.minY(), chunkBox.minY());
            int maxY = Math.min(islandBox.maxY(), chunkBox.maxY());
            int minZ = Math.max(islandBox.minZ(), chunkBox.minZ());
            int maxZ = Math.min(islandBox.maxZ(), chunkBox.maxZ());

            if (minX > maxX || minY > maxY || minZ > maxZ) return;

            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
            BlockPos center = new BlockPos(this.centerX, this.centerY, this.centerZ);
            placeSphere(world, mutablePos, center, BALL_RADIUS, minX, maxX, minY, maxY, minZ, maxZ);
        }

        private static void placeSphere(WorldGenLevel world, BlockPos.MutableBlockPos mutablePos, BlockPos center,
                                        int radius, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
            int innerRadius = Math.max(1, radius - 1);
            int radiusSq = radius * radius;
            int innerRadiusSq = innerRadius * innerRadius;

            for (int x = Math.max(minX, center.getX() - radius); x <= Math.min(maxX, center.getX() + radius); x++) {
                int dx = x - center.getX();
                for (int y = Math.max(minY, center.getY() - radius); y <= Math.min(maxY, center.getY() + radius); y++) {
                    int dy = y - center.getY();
                    for (int z = Math.max(minZ, center.getZ() - radius); z <= Math.min(maxZ, center.getZ() + radius); z++) {
                        int dz = z - center.getZ();
                        int distSq = (dx * dx) + (dy * dy) + (dz * dz);
                        if (distSq > radiusSq || distSq < innerRadiusSq) continue;

                        mutablePos.set(x, y, z);
                        BlockState existing = world.getBlockState(mutablePos);
                        if (!existing.isAir() && !existing.canBeReplaced()) continue;

                        world.setBlock(mutablePos, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    public static class BridgePiece extends StructurePiece {
        private final BlockPos start;
        private final BlockPos end;

        public BridgePiece(BlockPos start, BlockPos end, BoundingBox boundingBox) {
            super(ModStructurePieceTypes.GLASS_BALL_CLUSTER_PIECE.get(), 0, boundingBox);
            this.start = start;
            this.end = end;
        }

        public BridgePiece(CompoundTag nbt) {
            super(ModStructurePieceTypes.GLASS_BALL_CLUSTER_PIECE.get(), nbt);
            this.start = new BlockPos(nbt.getInt("StartX"), nbt.getInt("StartY"), nbt.getInt("StartZ"));
            this.end = new BlockPos(nbt.getInt("EndX"), nbt.getInt("EndY"), nbt.getInt("EndZ"));
        }

        public static BoundingBox createBoundingBox(BlockPos start, BlockPos end) {
            return new BoundingBox(
                    Math.min(start.getX(), end.getX()) - 2, Math.min(start.getY(), end.getY()) - 2, Math.min(start.getZ(), end.getZ()) - 2,
                    Math.max(start.getX(), end.getX()) + 2, Math.max(start.getY(), end.getY()) + 2, Math.max(start.getZ(), end.getZ()) + 2
            );
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
            nbt.putInt("StartX", this.start.getX());
            nbt.putInt("StartY", this.start.getY());
            nbt.putInt("StartZ", this.start.getZ());
            nbt.putInt("EndX", this.end.getX());
            nbt.putInt("EndY", this.end.getY());
            nbt.putInt("EndZ", this.end.getZ());
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureManager structureManager, ChunkGenerator chunkGenerator,
                                RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
            BoundingBox bridgeBox = this.getBoundingBox();
            int minX = Math.max(bridgeBox.minX(), chunkBox.minX());
            int maxX = Math.min(bridgeBox.maxX(), chunkBox.maxX());
            int minY = Math.max(bridgeBox.minY(), chunkBox.minY());
            int maxY = Math.min(bridgeBox.maxY(), chunkBox.maxY());
            int minZ = Math.max(bridgeBox.minZ(), chunkBox.minZ());
            int maxZ = Math.min(bridgeBox.maxZ(), chunkBox.maxZ());

            if (minX > maxX || minY > maxY || minZ > maxZ) return;

            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
            placeBridge(world, mutablePos, this.start, this.end, minX, maxX, minY, maxY, minZ, maxZ);
        }
    }

    private static void placeBridge(WorldGenLevel world, BlockPos.MutableBlockPos mutablePos, BlockPos start, BlockPos end,
                                    int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        int steps = Math.max(1, Math.max(Math.max(Math.abs(end.getX() - start.getX()), Math.abs(end.getY() - start.getY())),
                Math.abs(end.getZ() - start.getZ())) * 2);

        for (int step = 0; step <= steps; step++) {
            double t = (double) step / (double) steps;
            int x = Mth.floor(Mth.lerp(t, start.getX(), end.getX()));
            int y = Mth.floor(Mth.lerp(t, start.getY(), end.getY()));
            int z = Mth.floor(Mth.lerp(t, start.getZ(), end.getZ()));

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 1) continue;
                        int px = x + dx, py = y + dy, pz = z + dz;
                        if (px < minX || px > maxX || py < minY || py > maxY || pz < minZ || pz > maxZ) continue;

                        mutablePos.set(px, py, pz);
                        BlockState existing = world.getBlockState(mutablePos);
                        if (!existing.isAir() && !existing.canBeReplaced()) continue;
                        world.setBlock(mutablePos, Blocks.GLASS.defaultBlockState(), 2);
                    }
                }
            }
        }
    }
}
