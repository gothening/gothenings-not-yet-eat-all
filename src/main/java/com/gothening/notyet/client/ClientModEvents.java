package com.gothening.notyet.client;

import com.gothening.notyet.GotheningsNotYetEatAll;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public final class ClientModEvents {
    private ClientModEvents() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ClientKeybinds.EAT_ALL_KEY);
    }
}
