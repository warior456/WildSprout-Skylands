package net.ugi.wildsprout_skylands;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.ugi.wildsprout_skylands.world.gen.ModFeatures;
import net.ugi.wildsprout_skylands.world.gen.ModStructurePieceTypes;
import net.ugi.wildsprout_skylands.world.gen.ModStructureTypes;


@Mod(WildsproutSkylands.MODID)
public class WildsproutSkylands {

    public static final String MODID = "wildsprout_skylands";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WildsproutSkylands(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        // ModFeatures.FEATURES.register(modEventBus);
        
        LOGGER.info("Registering Mod Structures");
        ModStructureTypes.register(modEventBus);
        ModStructurePieceTypes.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }
}
