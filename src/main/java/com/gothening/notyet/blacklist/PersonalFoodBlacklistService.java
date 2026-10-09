package com.gothening.notyet.blacklist;

import com.gothening.notyet.config.GYNEConfig;
import com.gothening.notyet.network.BlacklistSyncPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server-authoritative personal blacklist operations: validation, persistence marking and sync.
 */
public final class PersonalFoodBlacklistService {
    private static final long MIN_ACTION_INTERVAL_MILLIS = 100L;
    private static final BlacklistRateLimiter RATE_LIMITER =
            new BlacklistRateLimiter(MIN_ACTION_INTERVAL_MILLIS, Util::getMillis);

    private PersonalFoodBlacklistService() {
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncTo(player);
        }
    }

    public static void handleSyncRequest(ServerPlayer player) {
        if (!canUse(player) || !RATE_LIMITER.allow(player.getUUID())) {
            return;
        }
        syncTo(player);
    }

    public static void handleUpdate(ServerPlayer player, String rawItemId, boolean add) {
        if (!canUse(player) || !RATE_LIMITER.allow(player.getUUID())) {
            return;
        }
        PersonalFoodBlacklistData data = PersonalFoodBlacklistData.get(player);
        if (data == null) {
            return;
        }
        String itemId = BlacklistItemValidator.normalizeItemId(rawItemId);
        if (itemId == null) {
            return;
        }

        boolean changed;
        if (add) {
            if (!BlacklistItemValidator.isAcceptableItemId(player.level().enabledFeatures(), player, itemId)) {
                return;
            }
            changed = data.add(player.getUUID(), itemId);
        } else {
            changed = data.remove(player.getUUID(), itemId);
        }

        if (changed) {
            data.setDirty();
        }
        syncTo(player);
    }

    private static boolean canUse(ServerPlayer player) {
        return !player.isRemoved() && player.isAlive();
    }

    private static void syncTo(ServerPlayer player) {
        PersonalFoodBlacklistData data = PersonalFoodBlacklistData.get(player);
        if (data == null) {
            return;
        }
        List<String> entries = data.list(player.getUUID());
        List<String> globallyDisabled = new ArrayList<>();
        for (String itemId : entries) {
            if (GYNEConfig.isFoodBlacklisted(itemId)) {
                globallyDisabled.add(itemId);
            }
        }
        PacketDistributor.sendToPlayer(player, new BlacklistSyncPayload(entries, globallyDisabled));
    }
}
