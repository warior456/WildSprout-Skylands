package net.ugi.wildsprout_skylands.world.gen.structure.decorator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.biome.Biomes;

import java.util.List;

/**
 * Handles placing vegetation (grass, ferns, flowers, and trees) on the top surface of floating islands.
 */
public class VegetationDecorator {

    public static void placeBigVegetation(WorldGenLevel world, ChunkGenerator generator, BlockPos.MutableBlockPos pos,
                                          List<BlockPos> surfaceBlocks, RandomSource random) {
        for (BlockPos surface : surfaceBlocks) {
            pos.set(surface.getX(), surface.getY() + 1, surface.getZ());
            if (!world.getBlockState(pos).isAir())
                continue;

            int roll = random.nextInt(100);
            if (roll < 22)
                continue;
            if (roll < 52) {
                world.setBlock(pos, Blocks.SHORT_GRASS.defaultBlockState(), 2);
            } else if (roll < 66) {
                BlockPos.MutableBlockPos upper = new BlockPos.MutableBlockPos(surface.getX(), surface.getY() + 2,
                        surface.getZ());
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
            } else {
                placeSmallTree(world, generator, surface.above(), random);
            }
        }
    }

    public static void placeMediumVegetation(WorldGenLevel world, BlockPos.MutableBlockPos pos,
                                             List<BlockPos> surfaceBlocks, RandomSource random) {
        for (BlockPos surface : surfaceBlocks) {
            pos.set(surface.getX(), surface.getY() + 1, surface.getZ());
            if (!world.getBlockState(pos).isAir())
                continue;

            int roll = random.nextInt(100);
            if (roll < 48)
                continue;
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

    public static void placeSmallTree(WorldGenLevel world, ChunkGenerator generator, BlockPos base, RandomSource random) {
        var biome = world.getBiome(base);
        var biomeKey = biome.unwrapKey().orElse(null);
        // Default spawn chance and tree type
        float spawnChance = 0.5f; // 50% chance to place any tree
        var treeFeature = TreeFeatures.OAK;
        if (biomeKey != null) {
            if (biomeKey.equals(Biomes.PLAINS) || biomeKey.equals(Biomes.DESERT)) {
                spawnChance = 0.1f;
            } else if (biomeKey.equals(Biomes.FOREST)) {
                spawnChance = 0.8f;
                if (random.nextFloat() < 0.8f) treeFeature = TreeFeatures.BIRCH;
            } else if (biomeKey.equals(Biomes.TAIGA)) {
                spawnChance = 0.7f;
                if (random.nextFloat() < 0.7f) treeFeature = TreeFeatures.SPRUCE;
            } else if (biomeKey.equals(Biomes.JUNGLE)) {
                spawnChance = 0.6f;
                if (random.nextFloat() < 0.6f) treeFeature = TreeFeatures.JUNGLE_TREE;
            } else if (biomeKey.equals(Biomes.SAVANNA)) {
                spawnChance = 0.5f;
                if (random.nextFloat() < 0.5f) treeFeature = TreeFeatures.ACACIA;
            } else if (biomeKey.equals(Biomes.DARK_FOREST)) {
                spawnChance = 0.9f;
                if (random.nextFloat() < 0.9f) treeFeature = TreeFeatures.DARK_OAK;
            }
        }
        if (random.nextFloat() > spawnChance) return;
        var selectedFeature = treeFeature;
        world.registryAccess()
                .registry(Registries.CONFIGURED_FEATURE)
                .flatMap(registry -> registry.getHolder(selectedFeature))
                .ifPresent(holder -> holder.value().place(world, generator, random, base));
    }
}
