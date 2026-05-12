package net.ugi.wildsprout_skylands.world.gen;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ugi.wildsprout_skylands.WildsproutSkylands;
import net.ugi.wildsprout_skylands.world.gen.structure.FloatingIslandStructure;
import net.ugi.wildsprout_skylands.world.gen.structure.GlassBallClusterStructure;

import java.util.function.Supplier;

public class ModStructureTypes {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, WildsproutSkylands.MODID);

    public static final Supplier<StructureType<FloatingIslandStructure>> FLOATING_ISLAND =
            STRUCTURE_TYPES.register("floating_island", () -> () -> FloatingIslandStructure.CODEC);

    public static final Supplier<StructureType<GlassBallClusterStructure>> GLASS_BALL_CLUSTER =
            STRUCTURE_TYPES.register("glass_ball_cluster", () -> () -> GlassBallClusterStructure.CODEC);
}
