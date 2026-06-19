package net.ugi.wildsprout_skylands.world.gen;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.ugi.wildsprout_skylands.WildsproutSkylands;

public class ModStructureSets {
    public static final ResourceKey<StructureSet> FLOATING_ISLAND = registerKey("floating_island");
    public static final ResourceKey<StructureSet> GLASS_BALL_CLUSTER = registerKey("glass_ball_cluster");

    public static void bootstrap(BootstrapContext<StructureSet> context) {
        HolderGetter<Structure> structureGetter = context.lookup(Registries.STRUCTURE);

        context.register(FLOATING_ISLAND, new StructureSet(
                structureGetter.getOrThrow(ModStructures.FLOATING_ISLAND),
                new RandomSpreadStructurePlacement(
                        24, // spacing
                        8, // separation
                        RandomSpreadType.LINEAR,
                        14357620 // salt
                )
        ));

    }

    private static ResourceKey<StructureSet> registerKey(String name) {
        return ResourceKey.create(Registries.STRUCTURE_SET, ResourceLocation.fromNamespaceAndPath(WildsproutSkylands.MODID, name));
    }
}
