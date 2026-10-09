package com.gothening.notyet.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.gothening.notyet.GotheningsNotYetEatAll;
import com.gothening.notyet.config.GYNEConfig;
import net.minecraft.client.KeyMapping;

public final class ClientKeybinds {
    public static final KeyMapping EAT_ALL_KEY = new KeyMapping(
            "key." + GotheningsNotYetEatAll.MOD_ID + ".eat_all",
            InputConstants.Type.KEYSYM,
            GYNEConfig.KEY_CODE.get(),
            "key.categories." + GotheningsNotYetEatAll.MOD_ID);

    private ClientKeybinds() {
    }
}
