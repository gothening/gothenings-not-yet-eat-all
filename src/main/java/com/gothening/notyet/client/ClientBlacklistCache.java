package com.gothening.notyet.client;

import java.util.List;
import java.util.Set;

/**
 * Client-side mirror of the authoritative personal blacklist, updated only by server sync
 * payloads. The GUI never edits this cache locally.
 */
public final class ClientBlacklistCache {
    private static List<String> entries = List.of();
    private static Set<String> globallyDisabled = Set.of();

    private ClientBlacklistCache() {
    }

    public static void update(List<String> newEntries, List<String> newGloballyDisabled) {
        entries = List.copyOf(newEntries);
        globallyDisabled = Set.copyOf(newGloballyDisabled);
    }

    public static List<String> entries() {
        return entries;
    }

    public static boolean isGloballyDisabled(String itemId) {
        return globallyDisabled.contains(itemId);
    }
}
