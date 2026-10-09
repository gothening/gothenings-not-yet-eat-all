package com.gothening.notyet.blacklist;

import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Item ID parsing and food recognition shared by the client GUI and the server validation path.
 */
public final class BlacklistItemValidator {
    private static final int MAX_ITEM_ID_LENGTH = 256;

    private BlacklistItemValidator() {
    }

    /**
     * Parses and normalizes an item ID without touching any registry, so unknown IDs from
     * removed mods can still be removed from an existing list.
     */
    @Nullable
    public static String normalizeItemId(@Nullable String rawItemId) {
        if (rawItemId == null || rawItemId.isEmpty() || rawItemId.length() > MAX_ITEM_ID_LENGTH) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(rawItemId);
        return id == null ? null : id.toString();
    }

    public static boolean isEdibleCandidate(ItemStack stack, @Nullable LivingEntity entity) {
        return !stack.isEmpty()
                && !stack.useOnRelease()
                && stack.getFoodProperties(entity) != null;
    }

    /**
     * Server-side rule for accepting a new personal blacklist entry: the ID must be a valid
     * resource location, exist in the item registry, and describe an item the mod considers edible.
     */
    public static boolean isAcceptableItemId(
            FeatureFlagSet enabledFeatures,
            @Nullable LivingEntity entity,
            @Nullable String rawItemId) {
        String itemId = normalizeItemId(rawItemId);
        if (itemId == null) {
            return false;
        }
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return false;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == null) {
            return false;
        }
        ItemStack stack = new ItemStack(item);
        return stack.isItemEnabled(enabledFeatures) && isEdibleCandidate(stack, entity);
    }
}
