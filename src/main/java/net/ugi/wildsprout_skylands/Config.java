package net.ugi.wildsprout_skylands;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue FLOATING_ISLANDS_MIN_Y = BUILDER
            .comment(" Minimum Y start level for floating islands generation\n do not set this too close blocks to world limits")
            .defineInRange("floatingIslandsMinY", 205, -2048, 2048);

    public static final ModConfigSpec.IntValue FLOATING_ISLANDS_MAX_Y = BUILDER
            .comment("Maximum Y start level for floating islands generation\n do not set this too close blocks to world limits")
            .defineInRange("floatingIslandsMaxY", 270, -2048, 2048);

    static final ModConfigSpec SPEC = BUILDER.build();
}
