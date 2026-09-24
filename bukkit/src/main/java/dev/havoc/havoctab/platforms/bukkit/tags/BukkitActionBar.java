package dev.havoc.havoctab.platforms.bukkit.tags;

import dev.havoc.havoctab.shared.chat.EnumChatFormat;
import dev.havoc.havoctab.shared.features.tags.TagManager;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Sends tag feedback above the hotbar on Bukkit and its forks.
 * <p>
 * Uses the Spigot chat API, which has carried action bar support since 1.9. On anything
 * older the call throws and {@link #send} reports failure, so HavocTab falls back to a
 * chat message rather than the player silently getting nothing.
 * <p>
 * The failure is remembered after the first miss, so an old server does not pay for a
 * thrown exception on every single click.
 */
public class BukkitActionBar implements TagManager.ActionBarSender {

    /** Set once the action bar turns out to be unavailable, so it is only attempted once */
    private volatile boolean unsupported;

    @Override
    public boolean send(@NotNull TabPlayer player, @NotNull String message) {
        if (unsupported) return false;

        Player bukkit = Bukkit.getPlayer(player.getUniqueId());
        if (bukkit == null) return false;

        try {
            // fromLegacyText rather than a raw TextComponent, so & colour codes inside the
            // message actually apply instead of being printed
            bukkit.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(EnumChatFormat.color(message)));
            return true;
        } catch (Throwable t) {
            // Pre-1.9, or a fork without the Spigot chat API
            unsupported = true;
            return false;
        }
    }
}
