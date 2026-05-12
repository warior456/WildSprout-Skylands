package net.ugi.wildsprout_skylands.world.gen;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.ugi.wildsprout_skylands.WildsproutSkylands;

public class ModConfiguredFeatures {
    // Example configured feature key:
    // public static final ResourceKey<ConfiguredFeature<?, ?>> EXAMPLE_KEY = registerKey("example_feature");

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // Example registration:
        // register(context, EXAMPLE_KEY, ModFeatures.EXAMPLE_FEATURE.get(), FeatureConfiguration.NONE);
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(WildsproutSkylands.MODID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                                                                          ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}
