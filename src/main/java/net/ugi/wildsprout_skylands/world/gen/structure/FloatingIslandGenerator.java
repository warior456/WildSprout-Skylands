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
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.ugi.wildsprout_skylands.world.gen.ModStructurePieceTypes;

import java.util.ArrayList;
import java.util.List;

public final class FloatingIslandGenerator {
    private FloatingIslandGenerator() {
    }

    public static class Piece extends StructurePiece {
        private static final int MAX_HORIZONTAL_RADIUS = 56;
        private static final int MAX_UP = 28;
        private static final int MAX_DOWN = 44;
        private static final double NOISE_SCALE = 0.09D;
        private static final double NOISE_STRENGTH = 0.30D;

        private final int centerX;
        private final int centerY;
        private final int centerZ;
        private final long shapeSeed;

        public Piece(int centerX, int centerY, int centerZ, long shapeSeed, BoundingBox boundingBox) {
            super(ModStructurePieceTypes.FLOATING_ISLAND_PIECE.get(), 0, boundingBox);
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.shapeSeed = shapeSeed;
        }

        public Piece(CompoundTag nbt) {
            super(ModStructurePieceTypes.FLOATING_ISLAND_PIECE.get(), nbt);
            this.centerX = nbt.getInt("CenterX");
            this.centerY = nbt.getInt("CenterY");
            this.centerZ = nbt.getInt("CenterZ");
            this.shapeSeed = nbt.getLong("ShapeSeed");
        }

        public static BoundingBox createBoundingBox(int centerX, int centerY, int centerZ) {
            return new BoundingBox(
                    centerX - MAX_HORIZONTAL_RADIUS,
                    centerY - MAX_DOWN,
                    centerZ - MAX_HORIZONTAL_RADIUS,
                    centerX + MAX_HORIZONTAL_RADIUS,
                    centerY + MAX_UP,
                    centerZ + MAX_HORIZONTAL_RADIUS
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
        public void postProcess(
                WorldGenLevel world,
                StructureManager structureManager,
                ChunkGenerator chunkGenerator,
                RandomSource random,
                BoundingBox chunkBox,
                ChunkPos chunkPos,
                BlockPos pivot
        ) {
            BoundingBox islandBox = this.getBoundingBox();
            int minX = Math.max(islandBox.minX(), chunkBox.minX());
            int maxX = Math.min(islandBox.maxX(), chunkBox.maxX());
            int minY = Math.max(islandBox.minY(), chunkBox.minY());
            int maxY = Math.min(islandBox.maxY(), chunkBox.maxY());
            int minZ = Math.max(islandBox.minZ(), chunkBox.minZ());
            int maxZ = Math.min(islandBox.maxZ(), chunkBox.maxZ());

            if (minX > maxX || minY > maxY || minZ > maxZ) {
                return;
            }

            RandomSource shapeRandom = RandomSource.create(this.shapeSeed);
            List<Lobe> lobes = createLobes(shapeRandom);
            RandomSource noiseRandom = RandomSource.create(this.shapeSeed ^ world.getSeed());
            NormalNoise noise = NormalNoise.create(noiseRandom, -3, new double[]{1.0D});

            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

            for (int y = maxY; y >= minY; y--) {
                int localY = y - this.centerY;
                for (int x = minX; x <= maxX; x++) {
                    int localX = x - this.centerX;
                    for (int z = minZ; z <= maxZ; z++) {
                        int localZ = z - this.centerZ;

                        if ((localX * localX) + (localZ * localZ) > (MAX_HORIZONTAL_RADIUS * MAX_HORIZONTAL_RADIUS)) {
                            continue;
                        }

                        if (!shouldFill(x, y, z, localX, localY, localZ, lobes, noise)) {
                            continue;
                        }

                        mutablePos.set(x, y, z);
                        BlockState existing = world.getBlockState(mutablePos);
                        if (!existing.isAir() && !existing.canBeReplaced()) {
                            continue;
                        }

                        int depth = getDepthFromSurface(x, y, z, localX, localY, localZ, lobes, noise);
                        BlockState state;
                        if (depth == 0) {
                            state = Blocks.GRASS_BLOCK.defaultBlockState();
                        } else if (depth < 5) {
                            state = Blocks.DIRT.defaultBlockState();
                        } else {
                            int stoneVariant = Math.abs(x * 31 + z * 17 + y * 13) % 8;
                            state = stoneVariant == 0 ? Blocks.ANDESITE.defaultBlockState() : Blocks.STONE.defaultBlockState();
                        }

                        world.setBlock(mutablePos, state, 2);
                    }
                }
            }
        }

        private int getDepthFromSurface(
                int worldX,
                int worldY,
                int worldZ,
                int localX,
                int localY,
                int localZ,
                List<Lobe> lobes,
                NormalNoise noise
        ) {
            for (int step = 1; step <= 6; step++) {
                if (!shouldFill(worldX, worldY + step, worldZ, localX, localY + step, localZ, lobes, noise)) {
                    return step - 1;
                }
            }
            return 6;
        }

        private static List<Lobe> createLobes(RandomSource random) {
            List<Lobe> lobes = new ArrayList<>();
            int lobeCount = 3 + random.nextInt(4);

            double currentX = 0.0D;
            double currentY = -2.0D + random.nextInt(9);
            double currentZ = 0.0D;

            for (int i = 0; i < lobeCount; i++) {
                if (i > 0) {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    double distance = 8.0D + random.nextDouble() * 15.0D;
                    currentX += Math.cos(angle) * distance;
                    currentZ += Math.sin(angle) * distance;
                    currentY += -7.0D + random.nextInt(15);

                    double horizontal = Math.sqrt((currentX * currentX) + (currentZ * currentZ));
                    double maxHorizontal = MAX_HORIZONTAL_RADIUS - 8.0D;
                    if (horizontal > maxHorizontal) {
                        double clamp = maxHorizontal / horizontal;
                        currentX *= clamp;
                        currentZ *= clamp;
                    }
                }

                double radiusX = 9.0D + random.nextDouble() * 14.0D;
                double radiusYTop = 4.5D + random.nextDouble() * 4.5D;
                double radiusYBottom = 12.0D + random.nextDouble() * 10.0D;
                double radiusZ = 9.0D + random.nextDouble() * 14.0D;
                lobes.add(new Lobe(currentX, currentY, currentZ, radiusX, radiusYTop, radiusYBottom, radiusZ));

                // Rarely add a hanging keel lobe to exaggerate droopy silhouettes.
                if (random.nextInt(4) == 0) {
                    double keelY = currentY - (6.0D + random.nextDouble() * 8.0D);
                    double keelRadiusX = radiusX * 0.48D;
                    double keelRadiusZ = radiusZ * 0.48D;
                    double keelTop = radiusYTop * 0.55D;
                    double keelBottom = radiusYBottom * 0.9D;
                    lobes.add(new Lobe(currentX, keelY, currentZ, keelRadiusX, keelTop, keelBottom, keelRadiusZ));
                }
            }

            return lobes;
        }

        private static boolean shouldFill(
                int worldX,
                int worldY,
                int worldZ,
                int localX,
                int localY,
                int localZ,
                List<Lobe> lobes,
                NormalNoise noise
        ) {
            double density = getCompositeDensity(localX, localY, localZ, lobes);
            double noiseValue = noise.getValue(worldX * NOISE_SCALE, worldY * NOISE_SCALE, worldZ * NOISE_SCALE);
            return density + (noiseValue * NOISE_STRENGTH) > 0.0D;
        }

        private static double getCompositeDensity(int x, int y, int z, List<Lobe> lobes) {
            double density = Double.NEGATIVE_INFINITY;

            for (Lobe lobe : lobes) {
                double dx = (x - lobe.offsetX) / lobe.radiusX;
                double yOffset = y - lobe.offsetY;
                double yRadius = yOffset >= 0.0D ? lobe.radiusYTop : lobe.radiusYBottom;
                double dy = yOffset / yRadius;
                double dz = (z - lobe.offsetZ) / lobe.radiusZ;
                double value = 1.0D - ((dx * dx) + (dy * dy) + (dz * dz));

                // Add underside weight so islands feel heavier and more droopy.
                if (yOffset < 0.0D) {
                    value += Math.min(0.26D, (-yOffset / lobe.radiusYBottom) * 0.24D);
                }

                density = Math.max(density, value);
            }

            for (int i = 1; i < lobes.size(); i++) {
                density = Math.max(density, connectorDensity(x, y, z, lobes.get(i - 1), lobes.get(i)));
            }

            // Flatten only the highest region and favor deep hanging undersides.
            if (y > 10) {
                density -= (y - 10) * 0.11D;
            }
            if (y < -8) {
                density += Math.min(0.32D, (-8 - y) * 0.03D);
            }

            return density;
        }

        private static double connectorDensity(int x, int y, int z, Lobe a, Lobe b) {
            double segmentX = b.offsetX - a.offsetX;
            double segmentY = b.offsetY - a.offsetY;
            double segmentZ = b.offsetZ - a.offsetZ;
            double lengthSquared = (segmentX * segmentX) + (segmentY * segmentY) + (segmentZ * segmentZ);

            if (lengthSquared < 0.0001D) {
                return Double.NEGATIVE_INFINITY;
            }

            double pointX = x - a.offsetX;
            double pointY = y - a.offsetY;
            double pointZ = z - a.offsetZ;
            double projection = ((pointX * segmentX) + (pointY * segmentY) + (pointZ * segmentZ)) / lengthSquared;
            double t = Mth.clamp(projection, 0.0D, 1.0D);

            double nearX = a.offsetX + (segmentX * t);
            double nearY = a.offsetY + (segmentY * t);
            double nearZ = a.offsetZ + (segmentZ * t);

            double radiusX = Mth.lerp(t, a.radiusX, b.radiusX) * 0.42D;
            double radiusYTop = Mth.lerp(t, a.radiusYTop, b.radiusYTop) * 0.45D;
            double radiusYBottom = Mth.lerp(t, a.radiusYBottom, b.radiusYBottom) * 0.68D;
            double radiusZ = Mth.lerp(t, a.radiusZ, b.radiusZ) * 0.42D;

            double dx = (x - nearX) / radiusX;
            double yOffset = y - nearY;
            double yRadius = yOffset >= 0.0D ? radiusYTop : radiusYBottom;
            double dy = yOffset / yRadius;
            double dz = (z - nearZ) / radiusZ;

            double connector = 0.92D - ((dx * dx) + (dy * dy) + (dz * dz));
            if (yOffset < 0.0D) {
                connector += Math.min(0.16D, (-yOffset / radiusYBottom) * 0.15D);
            }

            return connector;
        }

        private static final class Lobe {
            private final double offsetX;
            private final double offsetY;
            private final double offsetZ;
            private final double radiusX;
            private final double radiusYTop;
            private final double radiusYBottom;
            private final double radiusZ;

            private Lobe(double offsetX, double offsetY, double offsetZ, double radiusX, double radiusYTop, double radiusYBottom, double radiusZ) {
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
}
