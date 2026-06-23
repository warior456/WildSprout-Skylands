package net.ugi.wildsprout_skylands.world.gen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;

import java.util.List;

public class SmallIsland extends AbstractIsland {
    public SmallIsland(BlockPos center, int diameter, long shapeSeed, IslandType type) {
        super(center, diameter, shapeSeed, type, 1, 2);
    }

    @Override
    protected void decorate(WorldGenLevel world, BlockPos.MutableBlockPos pos, List<BlockPos> surfaceBlocks, RandomSource random) {
        // Small islands intentionally skip decoration for overhead.
    }
}
