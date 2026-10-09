package com.gothening.notyet.network;

import com.gothening.notyet.blacklist.PersonalFoodBlacklistService;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class BlacklistRequestPayloadHandler {
    private BlacklistRequestPayloadHandler() {
    }

    public static void handle(BlacklistRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) {
                return;
            }
            PersonalFoodBlacklistService.handleSyncRequest(player);
        });
    }
}
