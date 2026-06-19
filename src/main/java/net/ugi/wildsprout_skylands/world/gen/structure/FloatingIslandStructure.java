package net.ugi.wildsprout_skylands.world.gen.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.ugi.wildsprout_skylands.world.gen.ModStructureTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FloatingIslandStructure extends Structure {
    public static final MapCodec<FloatingIslandStructure> CODEC = simpleCodec(FloatingIslandStructure::new);
    private static final int MIN_Y = 205;
    private static final int MAX_Y = 262;

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
            RandomSource rand = RandomSource.create(baseSeed);
            // Store island info as [x, y, z, diameter]
            List<int[]> allIslands = new ArrayList<>(); // all placed islands for overlap check
            List<int[]> bigIslands = new ArrayList<>();

            // Generation boundary: 128 block radius, minus 28 block buffer = 100 effective
            final int MAX_RADIUS = 100;
            final int MAX_PLACEMENT_ATTEMPTS = 20;

            // ── Big islands (1-3) ──────────────────────────────────────────
            int bigCount = 1 + rand.nextInt(3);
            for (int i = 0; i < bigCount; i++) {
                int diameter = 25 + rand.nextInt(16); // 25-40
                int ix, iy, iz;

                if (i == 0) {
                    // Center island at origin
                    ix = startX;
                    iy = startY;
                    iz = startZ;
                } else {
                    // Try to place without overlapping existing islands
                    // todo replace with more uniform version
                    int bestX = startX, bestZ = startZ, bestY = startY;
                    boolean placed = false;
                    for (int attempt = 0; attempt < MAX_PLACEMENT_ATTEMPTS; attempt++) {
                        double angle = rand.nextDouble() * Math.PI * 2.0;
                        int dist = 50 + rand.nextInt(31);
                        int cx = startX + Mth.floor(Math.cos(angle) * dist);
                        int cz = startZ + Mth.floor(Math.sin(angle) * dist);
                        int cy = Mth.clamp(startY + (-15 + rand.nextInt(31)), MIN_Y, MAX_Y);

                        // Clamp to boundary
                        cx = clampToBoundary(cx, startX, MAX_RADIUS, diameter);
                        cz = clampToBoundary(cz, startZ, MAX_RADIUS, diameter);

                        if (!overlapsAny(cx, cz, diameter, allIslands)) {
                            bestX = cx;
                            bestZ = cz;
                            bestY = cy;
                            placed = true;
                            break;
                        }
                    }
                    if (!placed)
                        continue; // skip this island if we can't place it
                    ix = bestX;
                    iy = bestY;
                    iz = bestZ;
                }

                long shapeSeed = baseSeed + (341873128712L * i);
                int[] info = new int[] { ix, iy, iz, diameter };
                bigIslands.add(info);
                allIslands.add(info);

                BoundingBox box = FloatingIslandGenerator.Piece.createBoundingBox(ix, iy, iz, diameter);
                builder.addPiece(new FloatingIslandGenerator.Piece(
                        ix, iy, iz, shapeSeed,
                        FloatingIslandGenerator.Piece.IslandType.BIG, diameter, box));
            }

            // ── Bridges between consecutive big islands ────────────────────
            for (int i = 1; i < bigIslands.size(); i++) {
                int[] a = bigIslands.get(i - 1);
                int[] b = bigIslands.get(i);
                int width = 1 + rand.nextInt(3);
                createAndAddBridge(builder, a[0], a[1], a[2], a[3], b[0], b[1], b[2], b[3], width);
            }

            // ── Medium islands (2-5) ───────────────────────────────────────
            int mediumCount = 2 + rand.nextInt(4);
            for (int i = 0; i < mediumCount; i++) {
                int diameter = 12 + rand.nextInt(9); // 12-20
                int[] anchor = bigIslands.get(rand.nextInt(bigIslands.size()));

                int mx = 0, my = 0, mz = 0;
                boolean placed = false;
                for (int attempt = 0; attempt < MAX_PLACEMENT_ATTEMPTS; attempt++) {
                    double angle = rand.nextDouble() * Math.PI * 2.0;
                    int dist = 25 + rand.nextInt(21);
                    mx = anchor[0] + Mth.floor(Math.cos(angle) * dist);
                    mz = anchor[2] + Mth.floor(Math.sin(angle) * dist);
                    my = Mth.clamp(anchor[1] + (-10 + rand.nextInt(21)), MIN_Y, MAX_Y);

                    mx = clampToBoundary(mx, startX, MAX_RADIUS, diameter);
                    mz = clampToBoundary(mz, startZ, MAX_RADIUS, diameter);

                    if (!overlapsAny(mx, mz, diameter, allIslands)) {
                        placed = true;
                        break;
                    }
                }
                if (!placed)
                    continue;

                long shapeSeed = baseSeed + (341873128712L * (bigCount + i));
                allIslands.add(new int[] { mx, my, mz, diameter });

                BoundingBox box = FloatingIslandGenerator.Piece.createBoundingBox(mx, my, mz, diameter);
                builder.addPiece(new FloatingIslandGenerator.Piece(
                        mx, my, mz, shapeSeed,
                        FloatingIslandGenerator.Piece.IslandType.MEDIUM, diameter, box));

                int width = 1 + rand.nextInt(3);
                createAndAddBridge(builder, anchor[0], anchor[1], anchor[2], anchor[3],
                        mx, my, mz, diameter, width);
            }

            // ── Small islands (3-10) — mostly unconnected ─────────────────
            int smallCount = 3 + rand.nextInt(8);
            for (int i = 0; i < smallCount; i++) {
                int diameter = 3 + rand.nextInt(5); // 3-7

                int sx = 0, sz = 0, sy = 0;
                boolean placed = false;
                for (int attempt = 0; attempt < MAX_PLACEMENT_ATTEMPTS; attempt++) {
                    double angle = rand.nextDouble() * Math.PI * 2.0;
                    int dist = 30 + rand.nextInt(71);
                    sx = startX + Mth.floor(Math.cos(angle) * dist);
                    sz = startZ + Mth.floor(Math.sin(angle) * dist);
                    sy = Mth.clamp(startY + (-20 + rand.nextInt(41)), MIN_Y, MAX_Y);

                    sx = clampToBoundary(sx, startX, MAX_RADIUS, diameter);
                    sz = clampToBoundary(sz, startZ, MAX_RADIUS, diameter);

                    if (!overlapsAny(sx, sz, diameter, allIslands)) {
                        placed = true;
                        break;
                    }
                }
                if (!placed)
                    continue;

                long shapeSeed = baseSeed + (341873128712L * (bigCount + mediumCount + i));
                allIslands.add(new int[] { sx, sy, sz, diameter });

                BoundingBox box = FloatingIslandGenerator.Piece.createBoundingBox(sx, sy, sz, diameter);
                builder.addPiece(new FloatingIslandGenerator.Piece(
                        sx, sy, sz, shapeSeed,
                        FloatingIslandGenerator.Piece.IslandType.SMALL, diameter, box));
            }
        }));
    }

    /**
     * Returns true if a candidate island at (cx, cz) with the given diameter
     * would overlap too much with any already-placed island.
     * Minimum allowed distance = 60% of the sum of both radii.
     */
    private static boolean overlapsAny(int cx, int cz, int diameter, List<int[]> placed) {
        int candidateRadius = diameter / 2;
        for (int[] existing : placed) {
            int ex = existing[0], ez = existing[2], eDiameter = existing[3];
            int existingRadius = eDiameter / 2;

            double dx = cx - ex;
            double dz = cz - ez;
            double dist = Math.sqrt(dx * dx + dz * dz);
            double minDist = (candidateRadius + existingRadius) * 0.6;

            if (dist < minDist)
                return true;
        }
        return false;
    }

    /**
     * Clamps a coordinate so the island (including its radius) stays within
     * the allowed generation boundary from the structure center.
     */
    private static int clampToBoundary(int coord, int center, int maxRadius, int diameter) {
        int halfDiameter = diameter / 2;
        int minCoord = center - maxRadius + halfDiameter;
        int maxCoord = center + maxRadius - halfDiameter;
        return Mth.clamp(coord, minCoord, maxCoord);
    }

    /**
     * Computes edge-to-edge bridge endpoints and adds a BridgePiece.
     * Uses StructurePiecesBuilder directly via the lambda capture.
     */
    private static void createAndAddBridge(
            StructurePiecesBuilder builder,
            int fromX, int fromY, int fromZ, int fromDiameter,
            int toX, int toY, int toZ, int toDiameter,
            int width) {
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDist < 1.0)
            return;

        double nx = dx / horizontalDist;
        double nz = dz / horizontalDist;

        // Start bridges from 30% of the radius — well inside the island body
        // (before noise distortion), so they embed into the island rather than
        // floating at the edge.
        int fromOffset = Math.max(2, (int) (fromDiameter * 0.15));
        int toOffset = Math.max(2, (int) (toDiameter * 0.15));

        BlockPos bridgeStart = new BlockPos(
                fromX + Mth.floor(nx * fromOffset),
                fromY,
                fromZ + Mth.floor(nz * fromOffset));
        BlockPos bridgeEnd = new BlockPos(
                toX - Mth.floor(nx * toOffset),
                toY,
                toZ - Mth.floor(nz * toOffset));

        BoundingBox bridgeBox = FloatingIslandGenerator.BridgePiece.createBoundingBox(bridgeStart, bridgeEnd);
        builder.addPiece(new FloatingIslandGenerator.BridgePiece(bridgeStart, bridgeEnd, width, bridgeBox));
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.FLOATING_ISLAND.get();
    }
}
