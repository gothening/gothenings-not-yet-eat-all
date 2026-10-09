package com.gothening.notyet.network;

import com.gothening.notyet.GotheningsNotYetEatAll;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EatRequestPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<EatRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GotheningsNotYetEatAll.MOD_ID, "eat_request"));
    public static final StreamCodec<ByteBuf, EatRequestPayload> STREAM_CODEC =
            StreamCodec.unit(new EatRequestPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
