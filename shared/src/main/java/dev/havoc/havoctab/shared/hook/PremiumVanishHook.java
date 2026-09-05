package dev.havoc.havoctab.shared.hook;

import dev.havoc.havoctab.api.integration.VanishIntegration;

/**
 * Class for hooking into PremiumVanish to get vanish status of players.
 */
public abstract class PremiumVanishHook extends VanishIntegration {
    public PremiumVanishHook() {
        super("PremiumVanish");
    }
}
