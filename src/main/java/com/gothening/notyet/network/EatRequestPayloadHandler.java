package com.gothening.notyet.network;

import com.gothening.notyet.GotheningsNotYetEatAll;
import com.gothening.notyet.service.FoodSearchService;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class EatRequestPayloadHandler {
    private EatRequestPayloadHandler() {
    }

    public static void handle(EatRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) {
                return;
            }
            GotheningsNotYetEatAll.LOGGER.debug("Eat request received from {}", player.getGameProfile().getName());
            FoodSearchService.get().handleEatRequest(player);
        });
    }
}
