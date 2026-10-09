package com.gothening.notyet.network;

import com.gothening.notyet.GotheningsNotYetEatAll;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class NetworkHandler {
    private NetworkHandler() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("2")
                .playToServer(EatRequestPayload.TYPE, EatRequestPayload.STREAM_CODEC, EatRequestPayloadHandler::handle)
                .playToServer(BlacklistRequestPayload.TYPE, BlacklistRequestPayload.STREAM_CODEC, BlacklistRequestPayloadHandler::handle)
                .playToServer(BlacklistUpdatePayload.TYPE, BlacklistUpdatePayload.STREAM_CODEC, BlacklistUpdatePayloadHandler::handle)
                .playToClient(BlacklistSyncPayload.TYPE, BlacklistSyncPayload.STREAM_CODEC, BlacklistSyncPayloadHandler::handle);
    }
}
