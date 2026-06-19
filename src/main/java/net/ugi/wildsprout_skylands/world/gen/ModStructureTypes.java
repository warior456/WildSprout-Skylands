package net.ugi.wildsprout_skylands.world.gen;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ugi.wildsprout_skylands.WildsproutSkylands;
import net.ugi.wildsprout_skylands.world.gen.structure.FloatingIslandStructure;

import java.util.function.Supplier;

public class ModStructureTypes {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, WildsproutSkylands.MODID);

    public static final Supplier<StructureType<FloatingIslandStructure>> FLOATING_ISLAND =
            STRUCTURE_TYPES.register("floating_island", () -> () -> FloatingIslandStructure.CODEC);


    public static void register(IEventBus eventBus) {
        STRUCTURE_TYPES.register(eventBus);
    }
}
