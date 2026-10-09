package com.gothening.notyet.network;

import com.gothening.notyet.GotheningsNotYetEatAll;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class NetworkHandler {
    private NetworkHandler() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(EatRequestPayload.TYPE, EatRequestPayload.STREAM_CODEC, EatRequestPayloadHandler::handle);
    }
}
