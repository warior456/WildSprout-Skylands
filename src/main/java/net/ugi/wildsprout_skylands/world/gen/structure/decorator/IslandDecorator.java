package net.ugi.wildsprout_skylands.world.gen.structure.decorator;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;

import java.util.List;

/**
 * Strategy interface for decorating different types of islands on both top and bottom surfaces.
 */
public interface IslandDecorator {
    void decorateSurface(WorldGenLevel world, ChunkGenerator generator, BlockPos.MutableBlockPos pos, 
                         List<BlockPos> surfaceBlocks, RandomSource random);

    void decorateUnderside(WorldGenLevel world, BlockPos.MutableBlockPos pos, 
                           List<BlockPos> bottomBlocks, RandomSource random);
}
