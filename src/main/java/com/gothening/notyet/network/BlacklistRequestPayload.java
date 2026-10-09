package com.gothening.notyet.network;

import com.gothening.notyet.GotheningsNotYetEatAll;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: send me the current authoritative personal blacklist.
 */
public record BlacklistRequestPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BlacklistRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GotheningsNotYetEatAll.MOD_ID, "blacklist_request"));
    public static final StreamCodec<ByteBuf, BlacklistRequestPayload> STREAM_CODEC =
            StreamCodec.unit(new BlacklistRequestPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
