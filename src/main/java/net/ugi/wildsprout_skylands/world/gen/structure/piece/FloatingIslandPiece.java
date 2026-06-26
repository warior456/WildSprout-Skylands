package net.ugi.wildsprout_skylands.world.gen.structure.piece;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.ugi.wildsprout_skylands.world.gen.ModStructurePieceTypes;
import net.ugi.wildsprout_skylands.world.gen.structure.*;

public class FloatingIslandPiece extends StructurePiece {


    private final int centerX;
    private final int centerY;
    private final int centerZ;
    private final long shapeSeed;
    private final IslandType islandType;
    private final int diameter;

    public FloatingIslandPiece(int centerX, int centerY, int centerZ, long shapeSeed,
                               IslandType islandType, int diameter, BoundingBox boundingBox) {
        super(ModStructurePieceTypes.FLOATING_ISLAND_PIECE.get(), 0, boundingBox);
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.shapeSeed = shapeSeed;
        this.islandType = islandType;
        this.diameter = diameter;
    }

    public FloatingIslandPiece(CompoundTag nbt) {
        super(ModStructurePieceTypes.FLOATING_ISLAND_PIECE.get(), nbt);
        this.centerX = nbt.getInt("CenterX");
        this.centerY = nbt.getInt("CenterY");
        this.centerZ = nbt.getInt("CenterZ");
        this.shapeSeed = nbt.getLong("ShapeSeed");
        this.islandType = IslandType.fromOrdinal(nbt.getInt("IslandType"));
        this.diameter = nbt.getInt("Diameter");
    }

    public static BoundingBox createBoundingBox(int centerX, int centerY, int centerZ, int diameter) {
        int radius = (diameter / 2) + 4;
        int up = Math.max(8, diameter / 3);
        int down = Math.max(10, (int) (diameter * 0.8));
        return new BoundingBox(
                centerX - radius, centerY - down, centerZ - radius,
                centerX + radius, centerY + up + 6, centerZ + radius
        );
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
        nbt.putInt("CenterX", this.centerX);
        nbt.putInt("CenterY", this.centerY);
        nbt.putInt("CenterZ", this.centerZ);
        nbt.putLong("ShapeSeed", this.shapeSeed);
        nbt.putInt("IslandType", this.islandType.ordinal());
        nbt.putInt("Diameter", this.diameter);
    }

    @Override
    public void postProcess(WorldGenLevel world, StructureManager structureManager, ChunkGenerator chunkGenerator,
                            RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {


        BoundingBox islandBox = this.getBoundingBox();
        int minX = Math.max(islandBox.minX(), chunkBox.minX());
        int maxX = Math.min(islandBox.maxX(), chunkBox.maxX());
        int minY = Math.max(islandBox.minY(), chunkBox.minY());
        int maxY = Math.min(islandBox.maxY(), chunkBox.maxY());
        int minZ = Math.max(islandBox.minZ(), chunkBox.minZ());
        int maxZ = Math.min(islandBox.maxZ(), chunkBox.maxZ());

        if (minX > maxX || minY > maxY || minZ > maxZ) return;

        AbstractIsland island;
        BlockPos startCenter = new BlockPos(centerX, centerY, centerZ);
        switch (islandType) {
            case BIG:
                island = new BigIsland(startCenter, diameter, shapeSeed, islandType);
                break;
            case MEDIUM:
                island = new MediumIsland(startCenter, diameter, shapeSeed, islandType);
                break;
            case SMALL:
                island = new SmallIsland(startCenter, diameter, shapeSeed, islandType);
                break;
            default:
                throw new IllegalStateException("Unexpected island type: " + islandType);
        }

        RandomSource noiseRandom = RandomSource.create(this.shapeSeed ^ world.getSeed());
        NormalNoise shapeNoise = NormalNoise.create(noiseRandom, -3, new double[]{1.0});
        RandomSource surfaceNoiseRandom = RandomSource.create(this.shapeSeed ^ 69);
        NormalNoise surfaceNoise = NormalNoise.create(surfaceNoiseRandom, -2, new double[]{1.0, 0.5});

        island.generateChunk(world, chunkGenerator, minX, maxX, minY,maxY, minZ, maxZ, shapeNoise, surfaceNoise, random);
    }
}
