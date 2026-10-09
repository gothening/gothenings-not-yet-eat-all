package com.gothening.notyet.config;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class GYNEConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue KEY_ENABLED;
    public static final ModConfigSpec.IntValue KEY_CODE;

    public static final ModConfigSpec.BooleanValue ENABLE_PLAYER_INVENTORY;
    public static final ModConfigSpec.BooleanValue ENABLE_VANILLA_CONTAINERS;
    public static final ModConfigSpec.BooleanValue ENABLE_REFINED_STORAGE;
    public static final ModConfigSpec.BooleanValue ENABLE_AE2;
    public static final ModConfigSpec.BooleanValue ENABLE_SOPHISTICATED_BACKPACKS;
    public static final ModConfigSpec.BooleanValue ENABLE_SOPHISTICATED_STORAGE;

    public static final ModConfigSpec.BooleanValue ENABLE_SOL;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FOOD_BLACKLIST;

    static {
        Pair<Values, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Values::new);
        SPEC = pair.getRight();
        Values values = pair.getLeft();

        KEY_ENABLED = values.keyEnabled;
        KEY_CODE = values.keyCode;
        ENABLE_PLAYER_INVENTORY = values.enablePlayerInventory;
        ENABLE_VANILLA_CONTAINERS = values.enableVanillaContainers;
        ENABLE_REFINED_STORAGE = values.enableRefinedStorage;
        ENABLE_AE2 = values.enableAe2;
        ENABLE_SOPHISTICATED_BACKPACKS = values.enableSophisticatedBackpacks;
        ENABLE_SOPHISTICATED_STORAGE = values.enableSophisticatedStorage;
        ENABLE_SOL = values.enableSol;
        FOOD_BLACKLIST = values.foodBlacklist;
    }

    private GYNEConfig() {
    }

    public static boolean isFoodBlacklisted(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return FOOD_BLACKLIST.get().contains(id);
    }

    private static final class Values {
        private final ModConfigSpec.BooleanValue keyEnabled;
        private final ModConfigSpec.IntValue keyCode;
        private final ModConfigSpec.BooleanValue enablePlayerInventory;
        private final ModConfigSpec.BooleanValue enableVanillaContainers;
        private final ModConfigSpec.BooleanValue enableRefinedStorage;
        private final ModConfigSpec.BooleanValue enableAe2;
        private final ModConfigSpec.BooleanValue enableSophisticatedBackpacks;
        private final ModConfigSpec.BooleanValue enableSophisticatedStorage;
        private final ModConfigSpec.BooleanValue enableSol;
        private final ModConfigSpec.ConfigValue<List<? extends String>> foodBlacklist;

        private Values(ModConfigSpec.Builder builder) {
            builder.comment("Key binding settings").push("key");
            keyEnabled = builder.comment("Whether the one-key eat key is enabled").define("enabled", true);
            keyCode = builder.comment("GLFW key code. Default is V (86)").defineInRange("code", 86, 0, 65535);
            builder.pop();

            builder.comment("Provider toggles").push("providers");
            enablePlayerInventory = builder.comment("Allow eating from the player inventory").define("enable_player_inventory", true);
            enableVanillaContainers = builder.comment("Allow eating from nearby vanilla containers").define("enable_vanilla_containers", true);
            enableRefinedStorage = builder.comment("Allow eating from Refined Storage").define("enable_refined_storage", true);
            enableAe2 = builder.comment("Allow eating from Applied Energistics 2").define("enable_ae2", true);
            enableSophisticatedBackpacks = builder.comment("Allow eating from Sophisticated Backpacks").define("enable_sophisticated_backpacks", true);
            enableSophisticatedStorage = builder.comment("Allow eating from Sophisticated Storage").define("enable_sophisticated_storage", true);
            builder.pop();

            builder.comment("Spice of Life integration").push("spice_of_life");
            enableSol = builder.comment("Whether uneaten food preference from Spice of Life is used").define("enabled", true);
            builder.pop();

            foodBlacklist = builder
                    .comment("Food item IDs that will never be eaten automatically, e.g. minecraft:rotten_flesh")
                    .defineListAllowEmpty(
                            List.of("food_blacklist"),
                            () -> List.of(),
                            () -> "",
                            entry -> entry instanceof String);
        }
    }
}
