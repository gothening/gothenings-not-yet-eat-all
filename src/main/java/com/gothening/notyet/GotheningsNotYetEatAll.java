package com.gothening.notyet;

import com.gothening.notyet.network.NetworkHandler;
import com.gothening.notyet.client.ClientEvents;
import com.gothening.notyet.client.ClientModEvents;
import com.gothening.notyet.config.GYNEConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(GotheningsNotYetEatAll.MOD_ID)
public class GotheningsNotYetEatAll {
    public static final String MOD_ID = "gothenings_not_yet_eat_all";
    public static final String MOD_NAME = "Gothening的一键进食";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public GotheningsNotYetEatAll(IEventBus modEventBus) {
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.COMMON, GYNEConfig.SPEC);
        modEventBus.addListener(NetworkHandler::registerPayloads);
        if (FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(ClientModEvents::registerKeyMappings);
            NeoForge.EVENT_BUS.addListener(ClientEvents::onClientTick);
        }
    }
}
