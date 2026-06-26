package net.ugi.wildsprout_skylands.world.gen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;

import java.util.List;

public class BigIsland extends AbstractIsland {
    public BigIsland(BlockPos center, int diameter, long shapeSeed, IslandType type) {
        super(center, diameter, shapeSeed, type, 3, 6);
    }

    @Override
    protected void placeFeatures(WorldGenLevel world, ChunkGenerator generator, BlockPos.MutableBlockPos pos, List<BlockPos> surfaceBlocks, RandomSource random) {
        if (random.nextInt(10)<3) {

            int treeCount = 3 + random.nextInt(4);

            for (int i = 0; i < treeCount; i++) {
                if (surfaceBlocks.isEmpty()) break;


                int index = random.nextInt(surfaceBlocks.size());
                BlockPos targetPos = surfaceBlocks.remove(index);


                placeSmallTree(world, generator, targetPos.above(), random);
            }
        }
    }

    @Override
    protected void decorate(WorldGenLevel world, ChunkGenerator generator, BlockPos.MutableBlockPos pos, List<BlockPos> surfaceBlocks, RandomSource random) {
        placeFeatures(world, generator, pos, surfaceBlocks, random);
        placeBigVegetation(world, generator, pos, surfaceBlocks, random);
    }
}
