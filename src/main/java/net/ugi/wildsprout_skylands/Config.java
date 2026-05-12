package net.ugi.wildsprout_skylands;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue FLOATING_ISLANDS_ENABLED = BUILDER
            .comment("Whether to enable floating islands structure generation")
            .define("floatingIslandsEnabled", true);
            
    public static final ModConfigSpec.BooleanValue GLASS_BALL_CLUSTERS_ENABLED = BUILDER
            .comment("Whether to enable glass ball clusters structure generation")
            .define("glassBallClustersEnabled", true);

    static final ModConfigSpec SPEC = BUILDER.build();
}
