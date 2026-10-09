package com.gothening.notyet.diversity;

import com.gothening.notyet.GotheningsNotYetEatAll;
import com.gothening.notyet.config.GYNEConfig;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public final class SOLCarrotFoodDiversityProvider implements FoodDiversityProvider {
    private static final String SOLCARROT_MOD_ID = "solcarrot";
    private static final String API_CLASS_NAME = "com.cazsius.solcarrot.api.SOLCarrotAPI";
    private static final String CAPABILITY_CLASS_NAME = "com.cazsius.solcarrot.api.FoodCapability";

    private static boolean resolved;
    private static Method getFoodCapabilityMethod;
    private static Method hasEatenMethod;

    @Override
    public boolean isAvailable() {
        return GYNEConfig.ENABLE_SOL.get() && ModList.get().isLoaded(SOLCARROT_MOD_ID) && resolveApi();
    }

    @Override
    public boolean hasEaten(ServerPlayer player, ItemStack food) {
        if (!isAvailable()) {
            return false;
        }
        try {
            Object capability = getFoodCapabilityMethod.invoke(null, (Player) player);
            if (capability == null) {
                return false;
            }
            Object result = hasEatenMethod.invoke(capability, food);
            return Boolean.TRUE.equals(result);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            GotheningsNotYetEatAll.LOGGER.warn("Failed to query Spice of Life food history", exception);
            return false;
        }
    }

    private static synchronized boolean resolveApi() {
        if (resolved) {
            return hasEatenMethod != null;
        }
        resolved = true;
        try {
            Class<?> apiClass = Class.forName(API_CLASS_NAME);
            Class<?> capabilityClass = Class.forName(CAPABILITY_CLASS_NAME);
            getFoodCapabilityMethod = apiClass.getMethod("getFoodCapability", Player.class);
            hasEatenMethod = capabilityClass.getMethod("hasEaten", ItemStack.class);
        } catch (ReflectiveOperationException | LinkageError exception) {
            GotheningsNotYetEatAll.LOGGER.warn("Spice of Life API is not available", exception);
            getFoodCapabilityMethod = null;
            hasEatenMethod = null;
        }
        return hasEatenMethod != null;
    }
}
