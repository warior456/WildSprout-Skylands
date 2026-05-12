package net.ugi.wildsprout_skylands.world.gen;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.ugi.wildsprout_skylands.WildsproutSkylands;
import net.ugi.wildsprout_skylands.tags.ModTags;
import net.ugi.wildsprout_skylands.world.gen.structure.FloatingIslandStructure;
import net.ugi.wildsprout_skylands.world.gen.structure.GlassBallClusterStructure;

import java.util.Map;

public class ModStructures {
    public static final ResourceKey<Structure> FLOATING_ISLAND = registerKey("floating_island");
    public static final ResourceKey<Structure> GLASS_BALL_CLUSTER = registerKey("glass_ball_cluster");

    public static void bootstrap(BootstrapContext<Structure> context) {
        var biomeSet = context.lookup(Registries.BIOME);

        context.register(FLOATING_ISLAND, new FloatingIslandStructure(
                new Structure.StructureSettings(
                        biomeSet.getOrThrow(ModTags.Biome.HAS_FLOATING_ISLAND),
                        Map.of(),
                        GenerationStep.Decoration.RAW_GENERATION,
                        TerrainAdjustment.NONE
                )
        ));

        context.register(GLASS_BALL_CLUSTER, new GlassBallClusterStructure(
                new Structure.StructureSettings(
                        biomeSet.getOrThrow(ModTags.Biome.HAS_GLASS_BALL_CLUSTER),
                        Map.of(),
                        GenerationStep.Decoration.RAW_GENERATION,
                        TerrainAdjustment.NONE
                )
        ));
    }

    private static ResourceKey<Structure> registerKey(String name) {
        return ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(WildsproutSkylands.MODID, name));
    }
}
