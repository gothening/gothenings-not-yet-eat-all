package com.gothening.notyet.client;

/**
 * Pure routing rules for the eat key (V) so the shift priority can be unit tested.
 */
public final class EatKeyRouter {
    public enum Context {
        IN_GAME,
        BLACKLIST_SCREEN,
        PLAYER_INVENTORY,
        OTHER_SCREEN
    }

    public enum Action {
        EAT,
        OPEN_BLACKLIST,
        IGNORE
    }

    private EatKeyRouter() {
    }

    public static Action decide(Context context, boolean shiftPressed) {
        return switch (context) {
            case IN_GAME -> shiftPressed ? Action.OPEN_BLACKLIST : Action.EAT;
            case PLAYER_INVENTORY -> shiftPressed ? Action.OPEN_BLACKLIST : Action.IGNORE;
            case BLACKLIST_SCREEN, OTHER_SCREEN -> Action.IGNORE;
        };
    }
}
