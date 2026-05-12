package net.ugi.wildsprout_skylands.world.gen.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.ugi.wildsprout_skylands.world.gen.ModStructureTypes;

import java.util.Optional;

public class GlassBallClusterStructure extends Structure {
    public static final MapCodec<GlassBallClusterStructure> CODEC = simpleCodec(GlassBallClusterStructure::new);
    private static final int MIN_Y = 198;
    private static final int MAX_Y = 232;

    public GlassBallClusterStructure(Structure.StructureSettings settings) {
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
            int segmentCount = 3;
            double heading = context.random().nextDouble() * Math.PI * 2.0D;
            int stepDistance = 12;
            int verticalStep = -2;

            int x = startX;
            int y = startY;
            int z = startZ;
            BlockPos previousCenter = null;

            for (int i = 0; i < segmentCount; i++) {
                if (i > 0) {
                    x += Mth.floor(Math.cos(heading) * stepDistance);
                    z += Mth.floor(Math.sin(heading) * stepDistance);
                    y = Mth.clamp(y + verticalStep, MIN_Y, MAX_Y);
                }

                long pieceSeed = baseSeed + (341873128712L * i);
                BlockPos center = new BlockPos(x, y, z);
                BoundingBox box = GlassBallClusterGenerator.Piece.createBoundingBox(x, y, z);
                builder.addPiece(new GlassBallClusterGenerator.Piece(x, y, z, pieceSeed, box));

                if (previousCenter != null) {
                    BoundingBox bridgeBox = GlassBallClusterGenerator.BridgePiece.createBoundingBox(previousCenter, center);
                    builder.addPiece(new GlassBallClusterGenerator.BridgePiece(previousCenter, center, bridgeBox));
                }
                previousCenter = center;
            }
        }));
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.GLASS_BALL_CLUSTER.get();
    }
}
