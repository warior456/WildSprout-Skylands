package net.ugi.wildsprout_skylands.world.gen.structure;

public enum IslandType {
    BIG,
    MEDIUM,
    SMALL;

    public static IslandType fromOrdinal(int ordinal) {
        IslandType[] values = values();
        int clamped = Math.max(0, Math.min(values.length - 1, ordinal));
        return values[clamped];
    }
}
