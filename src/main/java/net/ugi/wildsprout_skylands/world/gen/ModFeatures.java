package net.ugi.wildsprout_skylands.world.gen;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ugi.wildsprout_skylands.WildsproutSkylands;

public class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, WildsproutSkylands.MODID);

    // Example feature registration:
    // public static final Supplier<Feature<NoneFeatureConfiguration>> EXAMPLE_FEATURE =
    //         FEATURES.register("example_feature", () -> new ExampleFeature(NoneFeatureConfiguration.CODEC));
}
