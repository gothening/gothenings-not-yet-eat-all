package com.gothening.notyet.network;

import com.gothening.notyet.GotheningsNotYetEatAll;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: incremental change of the personal blacklist (add or remove one item ID).
 */
public record BlacklistUpdatePayload(String itemId, boolean add) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BlacklistUpdatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GotheningsNotYetEatAll.MOD_ID, "blacklist_update"));
    public static final StreamCodec<ByteBuf, BlacklistUpdatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            BlacklistUpdatePayload::itemId,
            ByteBufCodecs.BOOL,
            BlacklistUpdatePayload::add,
            BlacklistUpdatePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
