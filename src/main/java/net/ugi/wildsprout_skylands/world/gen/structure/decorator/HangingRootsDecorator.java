package net.ugi.wildsprout_skylands.world.gen.structure.decorator;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;

import java.util.List;


public class HangingRootsDecorator {


    public static void placeHangingRoots(WorldGenLevel world, BlockPos.MutableBlockPos mutablePos,
                                         List<BlockPos> bottomBlocks, RandomSource random, float chance) {
        for (BlockPos bottom : bottomBlocks) {
            if (random.nextFloat() >= chance) continue;

            // Place one block below the bottom-surface block
            mutablePos.set(bottom.getX(), bottom.getY() - 1, bottom.getZ());

            // Only place if the space is air (never stack on another root or block)
            if (!world.getBlockState(mutablePos).isAir()) continue;

            world.setBlock(mutablePos, Blocks.HANGING_ROOTS.defaultBlockState(), 2);
        }
    }
}
