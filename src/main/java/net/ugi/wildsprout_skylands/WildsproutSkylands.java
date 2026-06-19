package net.ugi.wildsprout_skylands;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.ugi.wildsprout_skylands.world.gen.ModFeatures;
import net.ugi.wildsprout_skylands.world.gen.ModStructurePieceTypes;
import net.ugi.wildsprout_skylands.world.gen.ModStructureTypes;


@Mod(WildsproutSkylands.MODID)
public class WildsproutSkylands {

    public static final String MODID = "wildsprout_skylands";
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public WildsproutSkylands(IEventBus modEventBus, ModContainer modContainer) {

        modEventBus.addListener(this::commonSetup);


        //ModFeatures.FEATURES.register(modEventBus);
        
        LOGGER.info("Registering Mod Structures");
        ModStructureTypes.register(modEventBus);
        ModStructurePieceTypes.register(modEventBus);


        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        //NeoForge.EVENT_BUS.register(this);


        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
    }


//    @SubscribeEvent
//    public void onServerStarting(ServerStartingEvent event) {
//
//
//    }
}
