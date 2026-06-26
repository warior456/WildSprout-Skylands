package net.ugi.wildsprout_skylands.world.gen.structure;

import net.ugi.wildsprout_skylands.world.gen.structure.decorator.*;

/**
 * Enumeration representing the different floating island size categories.
 * Encapsulates the specific geometry limits (min/max lobes) and decoration strategy for each category.
 */
public enum IslandType {
    BIG(3, 6, new BigIslandDecorator()),
    MEDIUM(2, 4, new MediumIslandDecorator()),
    SMALL(2, 3, new SmallIslandDecorator());

    private final int minLobes;
    private final int maxLobes;
    private final IslandDecorator decorator;

    IslandType(int minLobes, int maxLobes, IslandDecorator decorator) {
        this.minLobes = minLobes;
        this.maxLobes = maxLobes;
        this.decorator = decorator;
    }

    public int getMinLobes() {
        return minLobes;
    }

    public int getMaxLobes() {
        return maxLobes;
    }

    public IslandDecorator getDecorator() {
        return decorator;
    }

    public static IslandType fromOrdinal(int ordinal) {
        IslandType[] values = values();
        int clamped = Math.max(0, Math.min(values.length - 1, ordinal));
        return values[clamped];
    }
}
