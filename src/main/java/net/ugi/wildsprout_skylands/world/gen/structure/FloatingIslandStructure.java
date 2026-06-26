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



    public FloatingIslandStructure(Structure.StructureSettings settings) {
        super(settings);


    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int currentMinY = Config.FLOATING_ISLANDS_MIN_Y.getAsInt();
        int currentMaxY = Config.FLOATING_ISLANDS_MAX_Y.getAsInt();
        int startX = context.chunkPos().getMinBlockX() + context.random().nextInt(16);
        int startZ = context.chunkPos().getMinBlockZ() + context.random().nextInt(16);
        int startY = currentMinY + context.random().nextInt((currentMaxY - currentMinY) + 1);

        if (context.heightAccessor().isOutsideBuildHeight(startY)) {
            WildsproutSkylands.LOGGER.warn("FloatingIslandStructure: coordinate {} {} {} is outside build height, skipping generation.", startX, startY, startZ);
            WildsproutSkylands.LOGGER.warn("check config for max and minimum height");
            return Optional.empty();
        }

        long baseSeed = context.random().nextLong();
        BlockPos start = new BlockPos(startX, startY, startZ);

        return Optional.of(new GenerationStub(start, builder -> {
            RandomSource rand = RandomSource.create(baseSeed);

            List<Island> allIslands = new ArrayList<>();
            List<Island> connectableIslands = new ArrayList<>(); // Combined Big & Medium islands

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



    private void generateBigIslands(StructurePiecesBuilder builder, RandomSource rand, long baseSeed, int startX, int startY, int startZ, int count, List<Island> allIslands, List<Island> connectableIslands) {
        for (int i = 0; i < count; i++) {
            int diameter = 25 + rand.nextInt(16);
            BlockPos pos;

            if (i == 0) {
                pos = new BlockPos(startX, startY, startZ);
            } else {
                pos = tryFindPosition(rand, startY, startX, startZ, diameter, 15, allIslands);
            }

            if (pos == null) continue;

            long shapeSeed = baseSeed + (341873128712L * i);
            Island info = new Island(pos, diameter, shapeSeed, IslandType.BIG);

            connectableIslands.add(info);
            allIslands.add(info);
            addIslandPiece(builder, pos, diameter, shapeSeed, IslandType.BIG);
        }
    }

    private void generateMediumIslands(StructurePiecesBuilder builder, RandomSource rand, long baseSeed, int startX, int startZ, int bigCount, int count, List<Island> allIslands, List<Island> connectableIslands) {
        if (connectableIslands.isEmpty()) return;

        for (int i = 0; i < count; i++) {
            int diameter = 12 + rand.nextInt(9);
            // Anchor to ANY existing connectable island (Big or Medium) to spread the network
            Island anchor = connectableIslands.get(rand.nextInt(connectableIslands.size()));

            BlockPos pos = tryFindPosition(rand, anchor.getCenter().getY(), startX, startZ, diameter, 25, allIslands);
            if (pos == null) continue;

            long shapeSeed = baseSeed + (341873128712L * (bigCount + i));
            Island info = new Island(pos, diameter, shapeSeed, IslandType.MEDIUM);

            connectableIslands.add(info);
            allIslands.add(info);
            addIslandPiece(builder, pos, diameter, shapeSeed, IslandType.MEDIUM);
        }
    }

    private void generateSmallIslands(StructurePiecesBuilder builder, RandomSource rand, long baseSeed, int startX, int startY, int startZ, int bigCount, int mediumCount, int count, List<Island> allIslands) {
        for (int i = 0; i < count; i++) {
            int diameter = 5 + rand.nextInt(4);
            BlockPos pos = tryFindPosition(rand, startY, startX, startZ, diameter, 30, allIslands);
            if (pos == null) continue;

            long shapeSeed = baseSeed + (341873128712L * (bigCount + mediumCount + i));
            allIslands.add(new Island(pos, diameter, shapeSeed, IslandType.SMALL));
            addIslandPiece(builder, pos, diameter, shapeSeed, IslandType.SMALL);
        }
    }

// ── Bridge Generation Logic ─────────────────────────────────────

    private void generateBridges(StructurePiecesBuilder builder, RandomSource rand, List<Island> connectableIslands, List<Island> allIslands) {
        if (connectableIslands.size() < 2) return;

        List<Island> connected = new ArrayList<>();
        List<Island> unconnected = new ArrayList<>(connectableIslands);

        // Start the network with the first island
        connected.add(unconnected.remove(0));

        // Phase 1: Proximity-Based Minimum Spanning Tree
        while (!unconnected.isEmpty()) {
            Island bestConnected = null;
            Island bestUnconnected = null;
            double minDistanceSq = Double.MAX_VALUE;
            boolean foundIntersectionFree = false;

            // Find the absolute shortest distance between ANY connected and ANY unconnected island
            for (Island u : unconnected) {
                for (Island c : connected) {
                    double distSq = distanceSq(c, u);
                    boolean intersects = doesBridgeIntersect(c, u, allIslands);

                    // Prioritize non-intersecting close connections
                    if (!intersects && distSq < minDistanceSq) {
                        minDistanceSq = distSq;
                        bestConnected = c;
                        bestUnconnected = u;
                        foundIntersectionFree = true;
                    }
                    // Fallback: If no valid path exists yet, track the closest intersecting one
                    else if (!foundIntersectionFree && distSq < minDistanceSq) {
                        minDistanceSq = distSq;
                        bestConnected = c;
                        bestUnconnected = u;
                    }
                }
            }

            int width = 1 + rand.nextInt(3);
            BlockPos cp = bestConnected.getCenter();
            BlockPos up = bestUnconnected.getCenter();
            createAndAddBridge(builder, cp.getX(), cp.getY(), cp.getZ(), bestConnected.getDiameter(),
                    up.getX(), up.getY(), up.getZ(), bestUnconnected.getDiameter(), width);

            connected.add(bestUnconnected);
            unconnected.remove(bestUnconnected);
        }

        // Phase 2: Proximity Webbing (Only connect nearby neighbors for natural loops)
        double maxWebbingDistSq = Math.pow(45, 2); // Max distance for extra bridges (adjust as needed)

        for (int i = 0; i < connectableIslands.size(); i++) {
            for (int j = i + 1; j < connectableIslands.size(); j++) {
                Island a = connectableIslands.get(i);
                Island b = connectableIslands.get(j);

                // 30% chance to connect if they are close together and don't intersect
                if (distanceSq(a, b) < maxWebbingDistSq && rand.nextFloat() < 0.3F) {
                    if (!doesBridgeIntersect(a, b, allIslands)) {
                        int width = 1 + rand.nextInt(2);
                        BlockPos ap = a.getCenter();
                        BlockPos bp = b.getCenter();
                        createAndAddBridge(builder, ap.getX(), ap.getY(), ap.getZ(), a.getDiameter(), 
                                bp.getX(), bp.getY(), bp.getZ(), b.getDiameter(), width);
                    }
                }
            }
        }
    }

// ── Required Helper Method ──────────────────────────────────────

    private double distanceSq(Island a, Island b) {
        BlockPos ap = a.getCenter();
        BlockPos bp = b.getCenter();
        return Math.pow(ap.getX() - bp.getX(), 2) + Math.pow(ap.getY() - bp.getY(), 2) + Math.pow(ap.getZ() - bp.getZ(), 2);
    }

// ── Math & Utility Methods ──────────────────────────────────────

    private boolean doesBridgeIntersect(Island a, Island b, List<Island> allIslands) {
        for (Island island : allIslands) {
            // Don't check collisions against the two islands the bridge is actively connecting
            if (island == a || island == b) continue;

            double radius = (island.getDiameter() / 2.0) + 2.0; // Added 2 block buffer zone

            BlockPos ap = a.getCenter();
            BlockPos bp = b.getCenter();
            BlockPos ip = island.getCenter();

            // 3D Line Segment to Point Distance Calculation
            double dx = bp.getX() - ap.getX();
            double dy = bp.getY() - ap.getY();
            double dz = bp.getZ() - ap.getZ();
            double lenSq = dx * dx + dy * dy + dz * dz;

            if (lenSq == 0) continue;

            double cx = ip.getX() - ap.getX();
            double cy = ip.getY() - ap.getY();
            double cz = ip.getZ() - ap.getZ();

            // Find closest point (t) on the line segment
            double t = (cx * dx + cy * dy + cz * dz) / lenSq;
            t = Math.max(0, Math.min(1, t)); // Clamp between island A (0) and island B (1)

            // Calculate 3D coordinates of that closest point
            double px = ap.getX() + t * dx;
            double py = ap.getY() + t * dy;
            double pz = ap.getZ() + t * dz;

            // Check if that point is inside the island's radius
            double distSq = (ip.getX() - px) * (ip.getX() - px) +
                    (ip.getY() - py) * (ip.getY() - py) +
                    (ip.getZ() - pz) * (ip.getZ() - pz);

            if (distSq < (radius * radius)) {
                return true; // The bridge passes through this island!
            }
        }
        return false;
    }

    private BlockPos tryFindPosition(RandomSource rand, int anchorY, int startX, int startZ, int diameter, int ySpread, List<Island> allIslands) {
        final int MAX_RADIUS = 128;
        final int MAX_ATTEMPTS = 12;

        // Create a safe buffer zone so the island never clips outside the max bounds
        double safeBoundary = MAX_RADIUS - (diameter / 2.0) - 5;

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            double angle = rand.nextDouble() * Math.PI * 2.0;

            // distribution around the center
            double centeringFactor = 0.75; // 0.5 = uniform, 1 = heavily centered
            double dist = safeBoundary * Math.pow(rand.nextDouble(), centeringFactor);

            int cx = startX + Mth.floor(Math.cos(angle) * dist);
            int cz = startZ + Mth.floor(Math.sin(angle) * dist);
            int currentMinY = Config.FLOATING_ISLANDS_MIN_Y.getAsInt();
            int currentMaxY = Config.FLOATING_ISLANDS_MAX_Y.getAsInt();
            int cy = Mth.clamp(anchorY + (-ySpread + rand.nextInt((ySpread * 2) + 1)), currentMinY, currentMaxY);

            if (!overlapsAny(cx, cz, diameter, allIslands)) {
                return new BlockPos(cx, cy, cz);
            }
        }
        return null;
    }

    private void addIslandPiece(StructurePiecesBuilder builder, BlockPos pos, int diameter, long shapeSeed, IslandType type) {
        BoundingBox box = FloatingIslandPiece.createBoundingBox(pos.getX(), pos.getY(), pos.getZ(), diameter);
        builder.addPiece(new FloatingIslandPiece(pos.getX(), pos.getY(), pos.getZ(), shapeSeed, type, diameter, box));
    }

    /**
     * Returns true if a candidate island at (cx, cz) with the given diameter
     * would overlap too much with any already-placed island.
     * Minimum allowed distance = sum of both radii.
     */
    private boolean overlapsAny(int cx, int cz, int diameter, List<Island> allIslands) {
        double radius = diameter / 2.0;

        for (Island island : allIslands) {
            BlockPos pos = island.getCenter();
            int otherX = pos.getX();
            int otherZ = pos.getZ();

            // Calculate squared 2D distance (faster than using Math.sqrt)
            double distanceSq = Math.pow(cx - otherX, 2) + Math.pow(cz - otherZ, 2);

            // Use both island radii for proper check
            double minDistance = radius + (island.getDiameter() / 2.0);

            if (distanceSq < (minDistance * minDistance)) {
                return true; // Overlap detected on the X/Z plane
            }
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
        int fromOffset = Math.max(2, (int) (fromDiameter * 0.25));
        int toOffset = Math.max(2, (int) (toDiameter * 0.25));

        BlockPos bridgeStart = new BlockPos(
                fromX + Mth.floor(nx * fromOffset),
                fromY -1,
                fromZ + Mth.floor(nz * fromOffset));
        BlockPos bridgeEnd = new BlockPos(
                toX - Mth.floor(nx * toOffset),
                toY -1,
                toZ - Mth.floor(nz * toOffset));

        BoundingBox bridgeBox = FloatingIslandBridgePiece.createBoundingBox(bridgeStart, bridgeEnd);
        builder.addPiece(new FloatingIslandBridgePiece(bridgeStart, bridgeEnd, width, bridgeBox));
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.FLOATING_ISLAND.get();
    }
}
