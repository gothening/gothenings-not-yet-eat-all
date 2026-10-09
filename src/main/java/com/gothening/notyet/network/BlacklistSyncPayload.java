package com.gothening.notyet.network;

import com.gothening.notyet.GotheningsNotYetEatAll;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: the authoritative personal blacklist plus the subset that is also disabled
 * by the server global configuration (shown differently in the GUI).
 */
public record BlacklistSyncPayload(List<String> entries, List<String> globallyDisabled) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BlacklistSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GotheningsNotYetEatAll.MOD_ID, "blacklist_sync"));
    public static final StreamCodec<ByteBuf, BlacklistSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.<ByteBuf, String, List<String>>collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
            BlacklistSyncPayload::entries,
            ByteBufCodecs.<ByteBuf, String, List<String>>collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
            BlacklistSyncPayload::globallyDisabled,
            BlacklistSyncPayload::new);

    public BlacklistSyncPayload {
        entries = List.copyOf(entries);
        globallyDisabled = List.copyOf(globallyDisabled);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
