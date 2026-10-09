package com.gothening.notyet.network;

import com.gothening.notyet.blacklist.PersonalFoodBlacklistService;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class BlacklistUpdatePayloadHandler {
    private BlacklistUpdatePayloadHandler() {
    }

    public static void handle(BlacklistUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) {
                return;
            }
            PersonalFoodBlacklistService.handleUpdate(player, payload.itemId(), payload.add());
        });
    }
}
