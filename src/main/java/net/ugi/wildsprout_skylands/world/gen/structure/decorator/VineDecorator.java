package net.ugi.wildsprout_skylands.world.gen.structure.decorator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;


public class VineDecorator {
    public static void placeVine(WorldGenLevel world, BlockPos.MutableBlockPos mutablePos, int minY, RandomSource vineRand, double dx, double dz, int py, int px, int pz) {
        int vineLength = 1 + vineRand.nextInt(7);
        BlockState previousVineState = null;
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();

        for (int v = 1; v <= vineLength; v++) {
            int vy = py - v;
            if (vy < minY) break;

            mutablePos.set(px, vy, pz);
            if (!world.getBlockState(mutablePos).isAir()) break;

            BlockState vineState;

            if (previousVineState == null) {
                // --- TOP VINE ---
                vineState = Blocks.VINE.defaultBlockState();
                boolean hasHorizontalSupport = false;

                // 1. Check surrounding blocks for horizontal trunk support
                if (isVineSupport(world, neighborPos.setWithOffset(mutablePos, Direction.NORTH), Direction.SOUTH)) {
                    vineState = vineState.setValue(VineBlock.NORTH, true);
                    hasHorizontalSupport = true;
                }
                if (isVineSupport(world, neighborPos.setWithOffset(mutablePos, Direction.SOUTH), Direction.NORTH)) {
                    vineState = vineState.setValue(VineBlock.SOUTH, true);
                    hasHorizontalSupport = true;
                }
                if (isVineSupport(world, neighborPos.setWithOffset(mutablePos, Direction.EAST), Direction.WEST)) {
                    vineState = vineState.setValue(VineBlock.EAST, true);
                    hasHorizontalSupport = true;
                }
                if (isVineSupport(world, neighborPos.setWithOffset(mutablePos, Direction.WEST), Direction.EAST)) {
                    vineState = vineState.setValue(VineBlock.WEST, true);
                    hasHorizontalSupport = true;
                }

                // 2. Check ceiling support
                boolean hasCeilingSupport = false;
                BlockState aboveState = world.getBlockState(neighborPos.setWithOffset(mutablePos, Direction.UP));
                if (aboveState.is(BlockTags.LEAVES) || aboveState.isFaceSturdy(world, neighborPos, Direction.DOWN)) {
                    vineState = vineState.setValue(VineBlock.UP, true);
                    hasCeilingSupport = true;
                }

                // 3. Fallback for free-hanging corner vines
                if (!hasHorizontalSupport) {
                    if (hasCeilingSupport) {
                        // Force a horizontal face so the game can render the hanging vine
                        if (Math.abs(dx) > Math.abs(dz)) {
                            vineState = vineState.setValue(dx > 0 ? VineBlock.WEST : VineBlock.EAST, true);
                        } else {
                            vineState = vineState.setValue(dz > 0 ? VineBlock.NORTH : VineBlock.SOUTH, true);
                        }
                    } else {
                        break; // No support at all, abort the entire vine column
                    }
                }
            } else {
                // --- LOWER VINES ---
                // Explicitly inherit only the horizontal properties from the vine above.
                vineState = Blocks.VINE.defaultBlockState()
                        .setValue(VineBlock.NORTH, previousVineState.getValue(VineBlock.NORTH))
                        .setValue(VineBlock.SOUTH, previousVineState.getValue(VineBlock.SOUTH))
                        .setValue(VineBlock.EAST, previousVineState.getValue(VineBlock.EAST))
                        .setValue(VineBlock.WEST, previousVineState.getValue(VineBlock.WEST));
            }

            world.setBlock(mutablePos, vineState, 19);
            previousVineState = vineState;
        }
    }

    public static boolean isVineSupport(WorldGenLevel world, BlockPos pos, Direction attachDirection) {
        BlockState state = world.getBlockState(pos);
        return state.isFaceSturdy(world, pos, attachDirection);
    }
}
