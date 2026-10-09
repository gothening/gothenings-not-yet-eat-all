package com.gothening.notyet.blacklist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.LoadingModList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BlacklistItemValidatorTest {
    @BeforeAll
    static void bootstrapMinecraft() throws ReflectiveOperationException {
        installEmptyLoadingModList();
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void acceptsStandardFoodItems() {
        assertTrue(BlacklistItemValidator.isAcceptableItemId(FeatureFlags.VANILLA_SET, null, "minecraft:apple"));
        assertTrue(BlacklistItemValidator.isEdibleCandidate(new ItemStack(Items.APPLE), null));
    }

    @Test
    void rejectsItemsWithoutFoodProperties() {
        assertFalse(BlacklistItemValidator.isAcceptableItemId(FeatureFlags.VANILLA_SET, null, "minecraft:stone"));
        assertFalse(BlacklistItemValidator.isEdibleCandidate(new ItemStack(Items.STONE), null));
    }

    @Test
    void rejectsMalformedUnknownAndOversizedIds() {
        assertFalse(BlacklistItemValidator.isAcceptableItemId(FeatureFlags.VANILLA_SET, null, "not a valid id"));
        assertFalse(BlacklistItemValidator.isAcceptableItemId(FeatureFlags.VANILLA_SET, null, "example:missing_food"));
        assertFalse(BlacklistItemValidator.isAcceptableItemId(FeatureFlags.VANILLA_SET, null, ""));
        assertFalse(BlacklistItemValidator.isAcceptableItemId(FeatureFlags.VANILLA_SET, null, null));
        assertFalse(BlacklistItemValidator.isAcceptableItemId(FeatureFlags.VANILLA_SET, null, "a".repeat(300)));
    }

    @Test
    void unknownButWellFormedIdsStayRemovable() {
        assertEquals("example:missing_food", BlacklistItemValidator.normalizeItemId("example:missing_food"));
        assertNull(BlacklistItemValidator.normalizeItemId("Not Valid!"));
        assertNull(BlacklistItemValidator.normalizeItemId(null));
    }

    /**
     * Vanilla bootstrap reaches NeoForge feature-flag loading, which expects the FML mod loading
     * state that only exists inside the game. Provide an empty mod list for this test JVM so the
     * registry-based validation path can run in a plain unit test.
     */
    @SuppressWarnings("deprecation")
    private static void installEmptyLoadingModList() throws ReflectiveOperationException {
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);

        Class<LoadingModList> type = LoadingModList.class;
        LoadingModList emptyModList = (LoadingModList) unsafe.allocateInstance(type);
        Field modFiles = type.getDeclaredField("modFiles");
        unsafe.putObject(emptyModList, unsafe.objectFieldOffset(modFiles), List.of());
        Field instance = type.getDeclaredField("INSTANCE");
        unsafe.putObject(type, unsafe.staticFieldOffset(instance), emptyModList);
    }
}
