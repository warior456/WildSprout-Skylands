package net.ugi.wildsprout_skylands.world.gen.structure.piece;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.ugi.wildsprout_skylands.world.gen.ModStructurePieceTypes;

public class FloatingIslandBridgePiece extends StructurePiece {

    private final BlockPos start;
    private final BlockPos end;
    private final int width;

    public FloatingIslandBridgePiece(BlockPos start, BlockPos end, int width, BoundingBox boundingBox) {
        super(ModStructurePieceTypes.FLOATING_ISLAND_BRIDGE.get(), 0, boundingBox);
        this.start = start;
        this.end = end;
        this.width = Mth.clamp(width, 1, 3);
    }

    public FloatingIslandBridgePiece(CompoundTag nbt) {
        super(ModStructurePieceTypes.FLOATING_ISLAND_BRIDGE.get(), nbt);
        this.start = new BlockPos(nbt.getInt("StartX"), nbt.getInt("StartY"), nbt.getInt("StartZ"));
        this.end = new BlockPos(nbt.getInt("EndX"), nbt.getInt("EndY"), nbt.getInt("EndZ"));
        this.width = nbt.getInt("Width");
    }

    public static BoundingBox createBoundingBox(BlockPos start, BlockPos end) {
        double dx = end.getX() - start.getX();
        double dz = end.getZ() - start.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        int sagBlocks = Math.max(4, (int) (horizontalDist * 0.2));//todo sag variable

        return new BoundingBox(
                Math.min(start.getX(), end.getX()) - 3,
                Math.min(start.getY(), end.getY()) - sagBlocks - 6,
                Math.min(start.getZ(), end.getZ()) - 3,
                Math.max(start.getX(), end.getX()) + 3,
                Math.max(start.getY(), end.getY()) + 2,
                Math.max(start.getZ(), end.getZ()) + 3
        );
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
        nbt.putInt("StartX", this.start.getX());
        nbt.putInt("StartY", this.start.getY());
        nbt.putInt("StartZ", this.start.getZ());
        nbt.putInt("EndX", this.end.getX());
        nbt.putInt("EndY", this.end.getY());
        nbt.putInt("EndZ", this.end.getZ());
        nbt.putInt("Width", this.width);
    }

    @Override
    public void postProcess(WorldGenLevel world, StructureManager structureManager, ChunkGenerator chunkGenerator,
                            RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {


        BoundingBox bridgeBox = this.getBoundingBox();
        int minX = Math.max(bridgeBox.minX(), chunkBox.minX());
        int maxX = Math.min(bridgeBox.maxX(), chunkBox.maxX());
        int minY = Math.max(bridgeBox.minY(), chunkBox.minY());
        int maxY = Math.min(bridgeBox.maxY(), chunkBox.maxY());
        int minZ = Math.max(bridgeBox.minZ(), chunkBox.minZ());
        int maxZ = Math.min(bridgeBox.maxZ(), chunkBox.maxZ());

        if (minX > maxX || minY > maxY || minZ > maxZ) return;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        placeBridge(world, mutablePos, this.start, this.end, this.width, minX, maxX, minY, maxY, minZ, maxZ, random);
    }

    private static void placeBridge(WorldGenLevel world, BlockPos.MutableBlockPos mutablePos,
                                    BlockPos start, BlockPos end, int width,
                                    int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
                                    RandomSource random) {
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double dz = end.getZ() - start.getZ();

        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        double trueDist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double sag = horizontalDist * 0.2;//todo sag variable
        int steps = Math.max(1, (int) ((trueDist + (sag * 2)) * 2.5));

        double perpX = 0, perpZ = 0;
        if (horizontalDist > 0.01) {
            perpX = -dz / horizontalDist;
            perpZ = dx / horizontalDist;
        }


        RandomSource vineRand = RandomSource.create(
                (long) start.getX() * 73856093L ^ (long) start.getZ() * 19349663L ^ (long) end.getX());

        for (int step = 0; step <= steps; step++) {
            double t = (double) step / (double) steps;
            double curveX = Mth.lerp(t, start.getX(), end.getX());
            double curveY = Mth.lerp(t, start.getY(), end.getY()) - sag * 4.0 * t * (1.0 - t);
            double curveZ = Mth.lerp(t, start.getZ(), end.getZ());

            int halfWidth = (width - 1) / 2;
            for (int w = -halfWidth; w <= halfWidth; w++) {
                int px = (int) Math.round(curveX + perpX * w);
                int py = Mth.floor(curveY);
                int pz = (int) Math.round(curveZ + perpZ * w);

                if (px < minX || px > maxX || py < minY || py > maxY || pz < minZ || pz > maxZ) continue;

                mutablePos.set(px, py, pz);
                if (!world.getBlockState(mutablePos).is(BlockTags.REPLACEABLE)) continue;

                world.setBlock(mutablePos, Blocks.MOSS_BLOCK.defaultBlockState(), 2);

                if (vineRand.nextInt(100) < 40) {
                    placeVine(world, mutablePos, minY, vineRand, dx, dz, py, px, pz);
                }
            }
        }
    }

    private static void placeVine(WorldGenLevel world, BlockPos.MutableBlockPos mutablePos, int minY, RandomSource vineRand, double dx, double dz, int py, int px, int pz) {
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

    private static boolean isVineSupport(WorldGenLevel world, BlockPos pos, Direction attachDirection) {
        BlockState state = world.getBlockState(pos);
        return state.isFaceSturdy(world, pos, attachDirection);
    }
}
