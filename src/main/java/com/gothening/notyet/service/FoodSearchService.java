package com.gothening.notyet.service;

import com.gothening.notyet.GotheningsNotYetEatAll;
import com.gothening.notyet.config.GYNEConfig;
import com.gothening.notyet.diversity.FoodDiversityProvider;
import com.gothening.notyet.diversity.SOLCarrotFoodDiversityProvider;
import com.gothening.notyet.food.FoodConsumer;
import com.gothening.notyet.food.FoodPreferenceSelector;
import com.gothening.notyet.food.FoodSelector;
import com.gothening.notyet.storage.FoodEntry;
import com.gothening.notyet.storage.FoodStorageProvider;
import com.gothening.notyet.storage.PlayerInventoryProvider;
import com.gothening.notyet.storage.StorageProviderManager;
import com.gothening.notyet.storage.VanillaContainerProvider;
import com.gothening.notyet.storage.RSFoodStorageProvider;
import com.gothening.notyet.storage.AE2FoodStorageProvider;
import com.gothening.notyet.storage.SophisticatedBackpacksFoodStorageProvider;
import com.gothening.notyet.storage.SophisticatedStorageFoodStorageProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public final class FoodSearchService {
    private static final FoodSearchService INSTANCE = new FoodSearchService();

    private final StorageProviderManager providerManager = new StorageProviderManager();
    private final Set<UUID> activeRequests = ConcurrentHashMap.newKeySet();
    private final FoodDiversityProvider diversityProvider = new SOLCarrotFoodDiversityProvider();

    private FoodSearchService() {
        if (GYNEConfig.ENABLE_PLAYER_INVENTORY.get()) {
            providerManager.register(new PlayerInventoryProvider());
        }
        if (GYNEConfig.ENABLE_VANILLA_CONTAINERS.get()) {
            providerManager.register(new VanillaContainerProvider());
        }
        try {
            if (GYNEConfig.ENABLE_REFINED_STORAGE.get() && ModList.get().isLoaded(RSFoodStorageProvider.MOD_ID)) {
                providerManager.register(new RSFoodStorageProvider());
            }
        } catch (LinkageError error) {
            GotheningsNotYetEatAll.LOGGER.warn("Refined Storage provider unavailable", error);
        }
        try {
            if (GYNEConfig.ENABLE_AE2.get() && ModList.get().isLoaded(AE2FoodStorageProvider.MOD_ID)) {
                providerManager.register(new AE2FoodStorageProvider());
            }
        } catch (LinkageError error) {
            GotheningsNotYetEatAll.LOGGER.warn("AE2 provider unavailable", error);
        }
        try {
            if (GYNEConfig.ENABLE_SOPHISTICATED_BACKPACKS.get() && ModList.get().isLoaded(SophisticatedBackpacksFoodStorageProvider.MOD_ID)) {
                providerManager.register(new SophisticatedBackpacksFoodStorageProvider());
            }
        } catch (LinkageError error) {
            GotheningsNotYetEatAll.LOGGER.warn("Sophisticated Backpacks provider unavailable", error);
        }
        try {
            if (GYNEConfig.ENABLE_SOPHISTICATED_STORAGE.get() && ModList.get().isLoaded(SophisticatedStorageFoodStorageProvider.MOD_ID)) {
                providerManager.register(new SophisticatedStorageFoodStorageProvider());
            }
        } catch (LinkageError error) {
            GotheningsNotYetEatAll.LOGGER.warn("Sophisticated Storage provider unavailable", error);
        }
    }

    public static FoodSearchService get() {
        return INSTANCE;
    }

    public void registerProvider(FoodStorageProvider provider) {
        providerManager.register(provider);
    }

    public void handleEatRequest(ServerPlayer player) {
        if (!activeRequests.add(player.getUUID())) {
            GotheningsNotYetEatAll.LOGGER.info("Eat request already in progress for {}", player.getGameProfile().getName());
            return;
        }

        try {
            if (player.isRemoved() || !player.isAlive()) {
                return;
            }

            List<FoodEntry> candidates = new ArrayList<>();
            for (FoodStorageProvider provider : providerManager.getProviders()) {
                if (!provider.isAvailable(player)) {
                    continue;
                }
                try {
                    candidates.addAll(provider.searchFood(player, stack -> FoodSelector.isFood(player, stack)));
                } catch (RuntimeException | LinkageError error) {
                    GotheningsNotYetEatAll.LOGGER.warn("Provider {} failed during search", provider.id(), error);
                }
            }

            if (candidates.isEmpty()) {
                GotheningsNotYetEatAll.LOGGER.info("No edible food found");
                return;
            }

            List<FoodEntry> remaining = new ArrayList<>(candidates);
            while (!remaining.isEmpty()) {
                Optional<FoodEntry> selected = FoodPreferenceSelector.select(player, remaining, diversityProvider);
                if (selected.isEmpty()) {
                    GotheningsNotYetEatAll.LOGGER.info("Player cannot eat right now");
                    return;
                }

                FoodEntry entry = selected.get();
                GotheningsNotYetEatAll.LOGGER.debug(
                        "Food candidate: {} x{}",
                        FoodSelector.itemId(entry.item()),
                        entry.availableCount());
                GotheningsNotYetEatAll.LOGGER.debug("Extracting: {}", FoodSelector.itemId(entry.item()));
                ItemStack extracted;
                try {
                    extracted = providerManager.extract(player, entry, 1);
                } catch (RuntimeException | LinkageError error) {
                    GotheningsNotYetEatAll.LOGGER.warn("Provider {} failed during extraction", entry.provider().id(), error);
                    remaining.remove(entry);
                    continue;
                }
                if (extracted.isEmpty()) {
                    GotheningsNotYetEatAll.LOGGER.debug("Extraction failed: stale or unavailable food");
                    remaining.remove(entry);
                    continue;
                }
                if (!FoodSelector.canEat(player, extracted)) {
                    returnToProviderSafely(player, entry, extracted);
                    remaining.remove(entry);
                    continue;
                }

                GotheningsNotYetEatAll.LOGGER.debug("Eating: {}", FoodSelector.itemId(extracted));
                ItemStack consumedFood = extracted.copy();
                FoodConsumer.ConsumeResult result = FoodConsumer.consume(player, extracted);
                returnToProviderSafely(player, entry, result.remaining());

                if (result.status() == FoodConsumer.Status.EATEN) {
                    GotheningsNotYetEatAll.LOGGER.info("Eat successful: {}", FoodSelector.itemId(consumedFood));
                } else {
                    GotheningsNotYetEatAll.LOGGER.warn("Failed to eat: {}", FoodSelector.itemId(consumedFood));
                }
                return;
            }

            GotheningsNotYetEatAll.LOGGER.info("No edible food found");
        } finally {
            activeRequests.remove(player.getUUID());
        }
    }

    private void returnToProviderSafely(ServerPlayer player, FoodEntry entry, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        try {
            providerManager.returnItems(player, entry, stack);
        } catch (RuntimeException | LinkageError error) {
            GotheningsNotYetEatAll.LOGGER.warn("Provider {} failed to return item", entry.provider().id(), error);
            ItemHandlerHelper.giveItemToPlayer(player, stack);
        }
    }
}
