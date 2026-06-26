package net.ugi.wildsprout_skylands.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.ugi.wildsprout_skylands.WildsproutSkylands;

public class ModTags {

    public static class Biome {
        public static final TagKey<net.minecraft.world.level.biome.Biome> HAS_FLOATING_ISLAND =
                createTag("has_structure/floating_island");

        private static TagKey<net.minecraft.world.level.biome.Biome> createTag(String name) {
            return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(WildsproutSkylands.MODID, name));
        }
    }
}
