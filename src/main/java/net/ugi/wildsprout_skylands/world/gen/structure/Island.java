package net.ugi.wildsprout_skylands.world.gen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete class representing a single floating island structure, carrying its geometry calculations,
 * and delegating decoration to size-specific strategy classes.
 */
public class Island {
    private static final double NOISE_SCALE = 0.09;
    private static final double NOISE_STRENGTH = 0.30;
    private static final double SURFACE_NOISE_SCALE = 0.06;
    private static final double SURFACE_NOISE_AMPLITUDE = 3.5;

    private final BlockPos center;
    private final int diameter;
    private final long shapeSeed;
    private final IslandType type;
    private final List<Lobe> lobes;

    public Island(BlockPos center, int diameter, long shapeSeed, IslandType type) {
        this.center = center;
        this.diameter = diameter;
        this.shapeSeed = shapeSeed;
        this.type = type;
        this.lobes = createLobes(RandomSource.create(shapeSeed), diameter, type.getMinLobes(), type.getMaxLobes());
    }

    public BlockPos getCenter() {
        return center;
    }

    public int getDiameter() {
        return diameter;
    }

    public IslandType getType() {
        return type;
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

    public void generateChunk(WorldGenLevel world, ChunkGenerator generator,
            int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
            NormalNoise shapeNoise, NormalNoise surfaceNoise,
            RandomSource random) {
        BlockPlacementResult result = placeBlocks(world, minX, maxX, minY, maxY, minZ, maxZ, shapeNoise, surfaceNoise,
                random);

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        this.type.getDecorator().decorateSurface(world, generator, mutablePos, result.surfaceBlocks(), random);
        this.type.getDecorator().decorateUnderside(world, mutablePos, result.bottomBlocks(), random);
    }

    private BlockPlacementResult placeBlocks(WorldGenLevel world,
            int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
            NormalNoise shapeNoise, NormalNoise surfaceNoise,
            RandomSource random) {
        List<BlockPos> surfaceBlocks = new ArrayList<>();
        List<BlockPos> bottomBlocks = new ArrayList<>();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int radius = this.diameter / 2;

        for (int y = maxY; y >= minY; y--) {
            int localY = y - this.center.getY();
            for (int x = minX; x <= maxX; x++) {
                int localX = x - this.center.getX();
                for (int z = minZ; z <= maxZ; z++) {
                    int localZ = z - this.center.getZ();

                    double horizDistSq = (localX * localX) + (localZ * localZ);
                    if (horizDistSq > (radius + 4.0) * (radius + 4.0))
                        continue;

                    if (!shouldFill(x, y, z, localX, localY, localZ, shapeNoise, surfaceNoise)) {
                        continue;
                    }

                    mutablePos.set(x, y, z);
                    BlockState existing = world.getBlockState(mutablePos);
                    if (!existing.isAir() && !existing.canBeReplaced())
                        continue;

                    int depth = getDepthFromSurface(x, y, z, localX, localY, localZ, shapeNoise, surfaceNoise);

                    // Check if this is a bottom-surface block (solid with air below)
                    if (!shouldFill(x, y - 1, z, localX, localY - 1, localZ, shapeNoise, surfaceNoise)) {
                        bottomBlocks.add(new BlockPos(x, y, z));
                    }

                    BlockState state;
                    if (depth == 0) {
                        state = Blocks.GRASS_BLOCK.defaultBlockState();
                        surfaceBlocks.add(new BlockPos(x, y, z));
                    } else if (depth < 5) {
                        state = Blocks.DIRT.defaultBlockState();
                    } else {
                        int stoneVariant = Math.abs(x * 31 + z * 17 + y * 13) % 8;
                        BlockState stoneState = stoneVariant == 0 ? Blocks.ANDESITE.defaultBlockState()
                                : Blocks.STONE.defaultBlockState();

                        int oreRoll = random.nextInt(100);
                        if (oreRoll < 5) {
                            int typeRoll = random.nextInt(100);
                            if (typeRoll < 40) {
                                state = Blocks.COAL_ORE.defaultBlockState();
                            } else if (typeRoll < 70) {
                                state = Blocks.IRON_ORE.defaultBlockState();
                            } else if (typeRoll < 85) {
                                state = Blocks.COPPER_ORE.defaultBlockState();
                            } else if (typeRoll < 92) {
                                state = Blocks.LAPIS_ORE.defaultBlockState();
                            } else if (typeRoll < 96) {
                                state = Blocks.GOLD_ORE.defaultBlockState();
                            } else if (typeRoll < 99) {
                                state = Blocks.REDSTONE_ORE.defaultBlockState();
                            } else {
                                state = Blocks.DIAMOND_ORE.defaultBlockState();
                            }
                        } else {
                            state = stoneState;
                        }
                    }

                    world.setBlock(mutablePos, state, 2);
                }
            }
        }
        return new BlockPlacementResult(surfaceBlocks, bottomBlocks);
    }

    /**
     * Holds both the top-surface and bottom-surface block lists from a placement pass.
     */
    private record BlockPlacementResult(List<BlockPos> surfaceBlocks, List<BlockPos> bottomBlocks) {}

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
        if (y > 10)
            density -= (y - 10) * 0.11;
        if (y < -8)
            density += Math.min(0.32, (-8 - y) * 0.03);
        return density;
    }

    private double connectorDensity(int x, int y, int z, Lobe a, Lobe b) {
        double segmentX = b.offsetX - a.offsetX;
        double segmentY = b.offsetY - a.offsetY;
        double segmentZ = b.offsetZ - a.offsetZ;
        double lengthSquared = segmentX * segmentX + segmentY * segmentY + segmentZ * segmentZ;
        if (lengthSquared < 0.0001)
            return Double.NEGATIVE_INFINITY;
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

    protected static List<Lobe> createLobes(RandomSource random, int diameter, int minLobes, int maxLobes) {
        List<Lobe> lobes = new ArrayList<>();
        double scale = diameter / 30.0;
        int lobeCount = minLobes + random.nextInt(maxLobes - minLobes + 1);
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


    private static final class Lobe {
        final double offsetX, offsetY, offsetZ;
        final double radiusX, radiusYTop, radiusYBottom, radiusZ;

        Lobe(double offsetX, double offsetY, double offsetZ, double radiusX, double radiusYTop, double radiusYBottom,
                double radiusZ) {
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
