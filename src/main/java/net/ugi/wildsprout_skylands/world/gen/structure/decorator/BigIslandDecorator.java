package net.ugi.wildsprout_skylands.world.gen.structure.decorator;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;

import java.util.List;

/**
 * Decoration strategy for BIG islands.
 * Places tree clusters and big vegetation on the surface, and hanging roots on the underside.
 */
public class BigIslandDecorator implements IslandDecorator {
    @Override
    public void decorateSurface(WorldGenLevel world, ChunkGenerator generator, BlockPos.MutableBlockPos pos, 
                                 List<BlockPos> surfaceBlocks, RandomSource random) {
        if (random.nextInt(10) < 3) {
            int treeCount = 4 + random.nextInt(7);
            for (int i = 0; i < treeCount; i++) {
                if (surfaceBlocks.isEmpty()) break;
                int index = random.nextInt(surfaceBlocks.size());
                BlockPos targetPos = surfaceBlocks.remove(index);
                VegetationDecorator.placeSmallTree(world, generator, targetPos.above(), random);
            }
        }
        VegetationDecorator.placeBigVegetation(world, generator, pos, surfaceBlocks, random);
    }

    @Override
    public void decorateUnderside(WorldGenLevel world, BlockPos.MutableBlockPos pos, 
                                   List<BlockPos> bottomBlocks, RandomSource random) {
        HangingRootsDecorator.placeHangingRoots(world, pos, bottomBlocks, random, 0.15f);
    }
}
