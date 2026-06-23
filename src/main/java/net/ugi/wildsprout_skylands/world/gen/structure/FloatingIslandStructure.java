package net.ugi.wildsprout_skylands.world.gen.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.ugi.wildsprout_skylands.Config;
import net.ugi.wildsprout_skylands.WildsproutSkylands;
import net.ugi.wildsprout_skylands.world.gen.ModStructureTypes;
import net.ugi.wildsprout_skylands.world.gen.structure.piece.FloatingIslandBridgePiece;
import net.ugi.wildsprout_skylands.world.gen.structure.piece.FloatingIslandPiece;

import java.util.*;

public class FloatingIslandStructure extends Structure {
    public static final MapCodec<FloatingIslandStructure> CODEC = simpleCodec(FloatingIslandStructure::new);
    private static final int MIN_Y = Config.FLOATING_ISLANDS_MIN_Y.getAsInt();
    private static final int MAX_Y = Config.FLOATING_ISLANDS_MAX_Y.getAsInt();

    public FloatingIslandStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int startX = context.chunkPos().getMinBlockX() + context.random().nextInt(16);
        int startZ = context.chunkPos().getMinBlockZ() + context.random().nextInt(16);
        int startY = MIN_Y + context.random().nextInt((MAX_Y - MIN_Y) + 1);

        if (context.heightAccessor().isOutsideBuildHeight(startY)) {
            WildsproutSkylands.LOGGER.warn("FloatingIslandStructure: coordinate {} {} {} is outside build height, skipping generation.", startX, startY, startZ);
            WildsproutSkylands.LOGGER.warn("check config for max and minimum height");
            return Optional.empty();
        }

        long baseSeed = context.random().nextLong();
        BlockPos start = new BlockPos(startX, startY, startZ);

        return Optional.of(new GenerationStub(start, builder -> {
            RandomSource rand = RandomSource.create(baseSeed);

            List<int[]> allIslands = new ArrayList<>();
            List<int[]> connectableIslands = new ArrayList<>(); // Combined Big & Medium islands

            int bigCount = rand.nextIntBetweenInclusive(1,4);
            int mediumCount = rand.nextIntBetweenInclusive(3,9);
            int smallCount = rand.nextIntBetweenInclusive(5,14);

            // 1. Generate core islands
            generateBigIslands(builder, rand, baseSeed, startX, startY, startZ, bigCount, allIslands, connectableIslands);

            // 2. Generate medium islands (they can now anchor to ANY connectable island, not just big ones)
            generateMediumIslands(builder, rand, baseSeed, startX, startZ, bigCount, mediumCount, allIslands, connectableIslands);

            // 3. Wire them together dynamically 
            generateBridges(builder, rand, connectableIslands, allIslands);

            // 4. Generate decorative small islands
            generateSmallIslands(builder, rand, baseSeed, startX, startY, startZ, bigCount, mediumCount, smallCount, allIslands);
        }));
    }

// ── Extraction Methods ──────────────────────────────────────────

    private void generateBigIslands(StructurePiecesBuilder builder, RandomSource rand, long baseSeed, int startX, int startY, int startZ, int count, List<int[]> allIslands, List<int[]> connectableIslands) {
        for (int i = 0; i < count; i++) {
            int diameter = 25 + rand.nextInt(16);
            int[] pos;

            if (i == 0) {
                pos = new int[]{startX, startY, startZ};
            } else {
                pos = tryFindPosition(rand, startX, startY, startZ, startX, startZ, diameter, 50, 80, 15, allIslands);
            }

            if (pos == null) continue;

            long shapeSeed = baseSeed + (341873128712L * i);
            int[] info = new int[]{pos[0], pos[1], pos[2], diameter};

            connectableIslands.add(info);
            allIslands.add(info);
            addIslandPiece(builder, pos, diameter, shapeSeed, IslandType.BIG);
        }
    }

    private void generateMediumIslands(StructurePiecesBuilder builder, RandomSource rand, long baseSeed, int startX, int startZ, int bigCount, int count, List<int[]> allIslands, List<int[]> connectableIslands) {
        if (connectableIslands.isEmpty()) return;

        for (int i = 0; i < count; i++) {
            int diameter = 12 + rand.nextInt(9);
            // Anchor to ANY existing connectable island (Big or Medium) to spread the network
            int[] anchor = connectableIslands.get(rand.nextInt(connectableIslands.size()));

            int[] pos = tryFindPosition(rand, anchor[0], anchor[1], anchor[2], startX, startZ, diameter, 25, 45, 10, allIslands);
            if (pos == null) continue;

            long shapeSeed = baseSeed + (341873128712L * (bigCount + i));
            int[] info = new int[]{pos[0], pos[1], pos[2], diameter};

            connectableIslands.add(info);
            allIslands.add(info);
            addIslandPiece(builder, pos, diameter, shapeSeed, IslandType.MEDIUM);
        }
    }

    private void generateSmallIslands(StructurePiecesBuilder builder, RandomSource rand, long baseSeed, int startX, int startY, int startZ, int bigCount, int mediumCount, int count, List<int[]> allIslands) {
        for (int i = 0; i < count; i++) {
            int diameter = 3 + rand.nextInt(5);
            int[] pos = tryFindPosition(rand, startX, startY, startZ, startX, startZ, diameter, 30, 100, 20, allIslands);
            if (pos == null) continue;

            long shapeSeed = baseSeed + (341873128712L * (bigCount + mediumCount + i));
            allIslands.add(new int[]{pos[0], pos[1], pos[2], diameter});
            addIslandPiece(builder, pos, diameter, shapeSeed, IslandType.SMALL);
        }
    }

// ── Bridge Generation Logic ─────────────────────────────────────

    private void generateBridges(StructurePiecesBuilder builder, RandomSource rand, List<int[]> connectableIslands, List<int[]> allIslands) {
        if (connectableIslands.size() < 2) return;

        List<int[]> connected = new ArrayList<>();
        List<int[]> unconnected = new ArrayList<>(connectableIslands);

        // Start the network with the first island
        connected.add(unconnected.remove(0));

        // Phase 1: Minimum Spanning Tree (Guarantees every island is accessible)
        for (int[] target : unconnected) {
            boolean linked = false;

            // Shuffle connected pool to pick random attachment points instead of hub-and-spoke
            List<int[]> potentialAnchors = new ArrayList<>(connected);
            Collections.shuffle(potentialAnchors, new Random(rand.nextLong()));

            for (int[] anchor : potentialAnchors) {
                if (!doesBridgeIntersect(anchor, target, allIslands)) {
                    int width = 1 + rand.nextInt(3);
                    createAndAddBridge(builder, anchor[0], anchor[1], anchor[2], anchor[3], target[0], target[1], target[2], target[3], width);
                    linked = true;
                    break;
                }
            }

            // Fallback: If all paths clip through other islands, force a connection to the closest anchor
            if (!linked) {
                int[] fallbackAnchor = getClosestIsland(target, connected);
                int width = 1 + rand.nextInt(3);
                createAndAddBridge(builder, fallbackAnchor[0], fallbackAnchor[1], fallbackAnchor[2], fallbackAnchor[3], target[0], target[1], target[2], target[3], width);
            }

            connected.add(target);
        }

        // Phase 2: Extra Webbing (Adds random extra bridges for non-linear exploration)
        int extraBridges = rand.nextInt(connectableIslands.size() / 2 + 1);
        for (int i = 0; i < extraBridges; i++) {
            int[] a = connectableIslands.get(rand.nextInt(connectableIslands.size()));
            int[] b = connectableIslands.get(rand.nextInt(connectableIslands.size()));

            if (a != b && !doesBridgeIntersect(a, b, allIslands)) {
                int width = 1 + rand.nextInt(2);
                createAndAddBridge(builder, a[0], a[1], a[2], a[3], b[0], b[1], b[2], b[3], width);
            }
        }
    }

// ── Math & Utility Methods ──────────────────────────────────────

    private boolean doesBridgeIntersect(int[] a, int[] b, List<int[]> allIslands) {
        for (int[] island : allIslands) {
            // Don't check collisions against the two islands the bridge is actively connecting
            if (island == a || island == b) continue;

            double radius = (island[3] / 2.0) + 2.0; // Added 2 block buffer zone

            // 3D Line Segment to Point Distance Calculation
            double dx = b[0] - a[0];
            double dy = b[1] - a[1];
            double dz = b[2] - a[2];
            double lenSq = dx * dx + dy * dy + dz * dz;

            if (lenSq == 0) continue;

            double cx = island[0] - a[0];
            double cy = island[1] - a[1];
            double cz = island[2] - a[2];

            // Find closest point (t) on the line segment
            double t = (cx * dx + cy * dy + cz * dz) / lenSq;
            t = Math.max(0, Math.min(1, t)); // Clamp between island A (0) and island B (1)

            // Calculate 3D coordinates of that closest point
            double px = a[0] + t * dx;
            double py = a[1] + t * dy;
            double pz = a[2] + t * dz;

            // Check if that point is inside the island's radius
            double distSq = (island[0] - px) * (island[0] - px) +
                    (island[1] - py) * (island[1] - py) +
                    (island[2] - pz) * (island[2] - pz);

            if (distSq < (radius * radius)) {
                return true; // The bridge passes through this island!
            }
        }
        return false;
    }

    private int[] getClosestIsland(int[] target, List<int[]> candidates) {
        int[] closest = candidates.get(0);
        double minDistSq = Double.MAX_VALUE;

        for (int[] candidate : candidates) {
            double distSq = Math.pow(target[0] - candidate[0], 2) +
                    Math.pow(target[1] - candidate[1], 2) +
                    Math.pow(target[2] - candidate[2], 2);
            if (distSq < minDistSq) {
                minDistSq = distSq;
                closest = candidate;
            }
        }
        return closest;
    }

    private int[] tryFindPosition(RandomSource rand, int anchorX, int anchorY, int anchorZ, int startX, int startZ, int diameter, int minRadius, int maxRadius, int ySpread, List<int[]> allIslands) {
        final int MAX_RADIUS = 128;
        final int MAX_ATTEMPTS = 5;

        // Create a safe buffer zone so the island never clips outside the max bounds
        double safeBoundary = MAX_RADIUS - (diameter / 2.0) - 5;

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            double angle = rand.nextDouble() * Math.PI * 2.0;

            // Math.sqrt() ensures a perfectly uniform distribution across the circle's area
            double dist = safeBoundary * Math.sqrt(rand.nextDouble());

            int cx = startX + Mth.floor(Math.cos(angle) * dist);
            int cz = startZ + Mth.floor(Math.sin(angle) * dist);
            int cy = Mth.clamp(anchorY + (-ySpread + rand.nextInt((ySpread * 2) + 1)), MIN_Y, MAX_Y);

            if (!overlapsAny(cx, cz, diameter, allIslands)) {
                return new int[]{cx, cy, cz};
            }
        }
        return null;
    }

    private void addIslandPiece(StructurePiecesBuilder builder, int[] pos, int diameter, long shapeSeed, IslandType type) {
        BoundingBox box = FloatingIslandPiece.createBoundingBox(pos[0], pos[1], pos[2], diameter);
        builder.addPiece(new FloatingIslandPiece(pos[0], pos[1], pos[2], shapeSeed, type, diameter, box));
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

        BoundingBox bridgeBox = FloatingIslandBridgePiece.createBoundingBox(bridgeStart, bridgeEnd);
        builder.addPiece(new FloatingIslandBridgePiece(bridgeStart, bridgeEnd, width, bridgeBox));
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.FLOATING_ISLAND.get();
    }
}
