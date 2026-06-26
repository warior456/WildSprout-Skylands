package net.ugi.wildsprout_skylands.world.gen.structure.decorator;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Places hanging roots on the underside of islands.
 * Hanging roots are single blocks that require a solid block directly above them.
 * They are never stacked — only one root per column.
 */
public class HangingRootsDecorator {

    /**
     * Places hanging roots below bottom-surface blocks.
     *
     * @param world        the world to place blocks in
     * @param mutablePos   a reusable mutable block position
     * @param bottomBlocks bottom-surface blocks of the island (solid with air below)
     * @param random       random source for this chunk
     * @param chance       probability (0.0–1.0) that any given bottom block gets a hanging root
     */
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
