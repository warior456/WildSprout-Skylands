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

    public static void bootstrap(BootstrapContext<StructureSet> context) {
        HolderGetter<Structure> structureGetter = context.lookup(Registries.STRUCTURE);

        context.register(FLOATING_ISLAND, new StructureSet(
                structureGetter.getOrThrow(ModStructures.FLOATING_ISLAND),
                new RandomSpreadStructurePlacement(
                        64, // spacing
                        16, // separation
                        RandomSpreadType.LINEAR,
                        845621820 // salt
                )
        ));

    }

    private static ResourceKey<StructureSet> registerKey(String name) {
        return ResourceKey.create(Registries.STRUCTURE_SET, ResourceLocation.fromNamespaceAndPath(WildsproutSkylands.MODID, name));
    }
}
