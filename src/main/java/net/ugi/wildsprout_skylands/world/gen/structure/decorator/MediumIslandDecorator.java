package net.ugi.wildsprout_skylands.world.gen.structure.decorator;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;

import java.util.List;

/**
 * Decoration strategy for MEDIUM islands.
 * Places medium density vegetation on the surface, and hanging roots on the underside.
 */
public class MediumIslandDecorator implements IslandDecorator {
    @Override
    public void decorateSurface(WorldGenLevel world, ChunkGenerator generator, BlockPos.MutableBlockPos pos, 
                                 List<BlockPos> surfaceBlocks, RandomSource random) {
        VegetationDecorator.placeMediumVegetation(world, pos, surfaceBlocks, random);
    }

    @Override
    public void decorateUnderside(WorldGenLevel world, BlockPos.MutableBlockPos pos, 
                                   List<BlockPos> bottomBlocks, RandomSource random) {
        HangingRootsDecorator.placeHangingRoots(world, pos, bottomBlocks, random, 0.15f);
    }
}
