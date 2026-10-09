package com.gothening.notyet.client;

import com.gothening.notyet.GotheningsNotYetEatAll;
import com.gothening.notyet.config.GYNEConfig;
import com.gothening.notyet.network.EatRequestPayload;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClientEvents {
    private ClientEvents() {
    }

    public static void onClientTick(ClientTickEvent.Pre event) {
        if (GYNEConfig.KEY_ENABLED.get() && ClientKeybinds.EAT_ALL_KEY.consumeClick()) {
            PacketDistributor.sendToServer(new EatRequestPayload());
        }
    }
}
