package net.ugi.wildsprout_skylands.world.gen.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.ugi.wildsprout_skylands.world.gen.ModStructureTypes;

import java.util.Optional;

public class FloatingIslandStructure extends Structure {
    public static final MapCodec<FloatingIslandStructure> CODEC = simpleCodec(FloatingIslandStructure::new);
    private static final int MIN_Y = 205;
    private static final int MAX_Y = 262;
    private static final int MIN_CHAIN_LENGTH = 3;
    private static final int MAX_CHAIN_LENGTH = 6;

    public FloatingIslandStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int startX = context.chunkPos().getMinBlockX() + context.random().nextInt(16);
        int startZ = context.chunkPos().getMinBlockZ() + context.random().nextInt(16);
        int startY = MIN_Y + context.random().nextInt((MAX_Y - MIN_Y) + 1);
        long baseSeed = context.random().nextLong();
        BlockPos start = new BlockPos(startX, startY, startZ);

        return Optional.of(new GenerationStub(start, builder -> {
            int chainLength = MIN_CHAIN_LENGTH + context.random().nextInt((MAX_CHAIN_LENGTH - MIN_CHAIN_LENGTH) + 1);
            double heading = context.random().nextDouble() * Math.PI * 2.0D;

            int x = startX;
            int y = startY;
            int z = startZ;

            for (int i = 0; i < chainLength; i++) {
                if (i > 0) {
                    heading += (context.random().nextDouble() - 0.5D) * 0.9D;
                    int stepDistance = 26 + context.random().nextInt(18);
                    x += Mth.floor(Math.cos(heading) * stepDistance);
                    z += Mth.floor(Math.sin(heading) * stepDistance);

                    int verticalStep = -11 + context.random().nextInt(23);
                    y = Mth.clamp(y + verticalStep, MIN_Y, MAX_Y + 14);
                }

                long shapeSeed = baseSeed + (341873128712L * i);
                BoundingBox box = FloatingIslandGenerator.Piece.createBoundingBox(x, y, z);
                builder.addPiece(new FloatingIslandGenerator.Piece(x, y, z, shapeSeed, box));
            }
        }));
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.FLOATING_ISLAND.get();
    }
}
