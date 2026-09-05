package dev.havoc.havoctab.platforms.bukkit.hook;

import de.myzelyam.api.vanish.VanishAPI;
import dev.havoc.havoctab.api.TabPlayer;
import dev.havoc.havoctab.platforms.bukkit.BukkitTabPlayer;
import dev.havoc.havoctab.shared.hook.PremiumVanishHook;
import org.jetbrains.annotations.NotNull;

/**
 * PremiumVanish hook for Bukkit.
 */
public class BukkitPremiumVanishHook extends PremiumVanishHook {

    @Override
    public boolean canSee(@NotNull TabPlayer viewer, @NotNull TabPlayer target) {
        return VanishAPI.canSee(((BukkitTabPlayer)viewer).getPlayer(), ((BukkitTabPlayer)target).getPlayer());
    }

    @Override
    public boolean isVanished(@NotNull TabPlayer player) {
        return VanishAPI.isInvisible(((BukkitTabPlayer)player).getPlayer());
    }
}
