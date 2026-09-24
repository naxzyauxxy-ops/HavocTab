package dev.havoc.havoctab.platforms.velocity.hook;

import de.myzelyam.api.vanish.VelocityVanishAPI;
import dev.havoc.havoctab.api.TabPlayer;
import dev.havoc.havoctab.platforms.velocity.VelocityTabPlayer;
import dev.havoc.havoctab.shared.hook.PremiumVanishHook;
import org.jetbrains.annotations.NotNull;

/**
 * PremiumVanish hook for Velocity.
 */
public class VelocityPremiumVanishHook extends PremiumVanishHook {

    @Override
    public synchronized boolean canSee(@NotNull TabPlayer viewer, @NotNull TabPlayer target) {
        return VelocityVanishAPI.canSee(((VelocityTabPlayer)viewer).getPlayer(), ((VelocityTabPlayer)target).getPlayer());
    }

    @Override
    public boolean isVanished(@NotNull TabPlayer player) {
        return VelocityVanishAPI.isInvisible(((VelocityTabPlayer)player).getPlayer());
    }
}
