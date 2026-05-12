package net.ugi.wildsprout_skylands.world.gen;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.ugi.wildsprout_skylands.WildsproutSkylands;

public class ModBiomeModifiers {
    // Example biome modifier key:
    // public static final ResourceKey<BiomeModifier> ADD_EXAMPLE_FEATURE = registerKey("add_example_feature");

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        // var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        // var biomes = context.lookup(Registries.BIOME);

        // Example biome modifier (e.g. for placing a feature in plains):
        // context.register(ADD_EXAMPLE_FEATURE, new BiomeModifiers.AddFeaturesBiomeModifier(
        //         HolderSet.direct(biomes.getOrThrow(Biomes.PLAINS)),
        //         HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.EXAMPLE_PLACED_KEY)),
        //         GenerationStep.Decoration.VEGETAL_DECORATION));
        
        // Note: Structures do not use BiomeModifiers in NeoForge. They use biome tags.
        // See ModTags.Biome and the biome tag datagen provider.
    }

    private static ResourceKey<BiomeModifier> registerKey(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(WildsproutSkylands.MODID, name));
    }
}
