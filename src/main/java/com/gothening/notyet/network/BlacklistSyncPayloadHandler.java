package com.gothening.notyet.network;

import com.gothening.notyet.client.ClientBlacklistCache;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class BlacklistSyncPayloadHandler {
    private BlacklistSyncPayloadHandler() {
    }

    public static void handle(BlacklistSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow() != PacketFlow.CLIENTBOUND) {
                return;
            }
            ClientBlacklistCache.update(payload.entries(), payload.globallyDisabled());
        });
    }
}
