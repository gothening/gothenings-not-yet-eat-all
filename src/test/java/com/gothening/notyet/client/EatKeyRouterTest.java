package com.gothening.notyet.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EatKeyRouterTest {
    @Test
    void plainKeyEatsOnlyInGame() {
        assertEquals(EatKeyRouter.Action.EAT, EatKeyRouter.decide(EatKeyRouter.Context.IN_GAME, false));
        assertEquals(EatKeyRouter.Action.IGNORE, EatKeyRouter.decide(EatKeyRouter.Context.PLAYER_INVENTORY, false));
        assertEquals(EatKeyRouter.Action.IGNORE, EatKeyRouter.decide(EatKeyRouter.Context.BLACKLIST_SCREEN, false));
        assertEquals(EatKeyRouter.Action.IGNORE, EatKeyRouter.decide(EatKeyRouter.Context.OTHER_SCREEN, false));
    }

    @Test
    void shiftKeyOpensTheBlacklistOnlyInGameOrPlayerInventory() {
        assertEquals(EatKeyRouter.Action.OPEN_BLACKLIST, EatKeyRouter.decide(EatKeyRouter.Context.IN_GAME, true));
        assertEquals(EatKeyRouter.Action.OPEN_BLACKLIST, EatKeyRouter.decide(EatKeyRouter.Context.PLAYER_INVENTORY, true));
    }

    @Test
    void openOrForeignScreensNeverReact() {
        assertEquals(EatKeyRouter.Action.IGNORE, EatKeyRouter.decide(EatKeyRouter.Context.BLACKLIST_SCREEN, true));
        assertEquals(EatKeyRouter.Action.IGNORE, EatKeyRouter.decide(EatKeyRouter.Context.OTHER_SCREEN, true));
    }
}
