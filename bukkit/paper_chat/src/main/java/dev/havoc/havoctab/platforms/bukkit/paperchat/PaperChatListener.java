package dev.havoc.havoctab.platforms.bukkit.paperchat;

import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.features.chat.ChatManager;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Applies HavocTab's chat format per recipient and drops recipients who should not
 * receive the message.
 * <p>
 * Paper renders chat once per viewer, which is what makes this feature possible: the
 * same message can be styled for one player, plain for another, and never delivered to
 * a third who blocked the sender.
 */
public class PaperChatListener implements Listener {

    /** Set once if the viewer set turns out to be immutable, so we only warn a single time */
    private boolean warnedAboutViewers;

    /**
     * Handles a chat message: filters recipients, then installs a per-viewer renderer.
     *
     * @param   event
     *          Chat event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(@NotNull AsyncChatEvent event) {
        ChatManager chat = PaperChatHook.feature();
        if (chat == null) return;

        TabPlayer sender = HavocTab.getInstance().getPlayer(event.getPlayer().getUniqueId());
        if (sender == null) return; // Player not loaded by HavocTab yet

        removeHiddenViewers(event, chat, sender);

        if (!chat.getConfiguration().isFormatEnabled()) return;

        event.renderer((source, sourceDisplayName, message, viewer) ->
                render(chat, sender, message, viewer));
    }

    /**
     * Removes every viewer who blocked the sender or turned public chat off.
     */
    private void removeHiddenViewers(@NotNull AsyncChatEvent event, @NotNull ChatManager chat, @NotNull TabPlayer sender) {
        try {
            event.viewers().removeIf(audience -> {
                TabPlayer viewer = resolve(audience);
                // Console and anything that is not a loaded player always receives the message
                return viewer != null && !chat.shouldReceive(viewer, sender);
            });
        } catch (UnsupportedOperationException e) {
            if (!warnedAboutViewers) {
                warnedAboutViewers = true;
                HavocTab.getInstance().debug("Chat viewer set is immutable on this server build, " +
                        "blocking and the public chat toggle cannot filter recipients.");
            }
        }
    }

    /**
     * Builds the chat line for one viewer.
     * <p>
     * The format and the player's message are assembled as separate components, so text a
     * player types is never run through the placeholder or colour parser.
     */
    @NotNull
    private Component render(@NotNull ChatManager chat, @NotNull TabPlayer sender,
                             @NotNull Component message, @NotNull Audience audience) {
        TabPlayer viewer = resolve(audience);
        if (viewer == null) viewer = sender; // Console: render as the sender would see it

        String template = chat.renderTemplate(sender, viewer);
        int index = template.indexOf("%message%");

        Component line;
        if (index == -1) {
            // Format has no %message% placeholder, append the message at the end
            line = Component.empty()
                    .append(chat.getComponentCache().get(template).toAdventure())
                    .append(message);
        } else {
            String before = template.substring(0, index);
            String after = template.substring(index + "%message%".length());
            line = Component.empty()
                    .append(chat.getComponentCache().get(before).toAdventure())
                    .append(message)
                    .append(chat.getComponentCache().get(after).toAdventure());
        }

        return decorate(chat, sender, viewer, line);
    }

    /**
     * Attaches the hover card and click action, when configured and not opted out of.
     */
    @NotNull
    private Component decorate(@NotNull ChatManager chat, @NotNull TabPlayer sender,
                               @NotNull TabPlayer viewer, @NotNull Component line) {
        List<String> hover = chat.renderHover(sender, viewer);
        if (!hover.isEmpty()) {
            Component tooltip = Component.empty();
            for (int i = 0; i < hover.size(); i++) {
                if (i > 0) tooltip = tooltip.append(Component.newline());
                tooltip = tooltip.append(chat.getComponentCache().get(hover.get(i)).toAdventure());
            }
            line = line.hoverEvent(HoverEvent.showText(tooltip));
        }

        String click = chat.renderClickCommand(sender, viewer);
        if (!click.isEmpty()) {
            line = line.clickEvent(click.startsWith("/")
                    ? ClickEvent.runCommand(click)
                    : ClickEvent.suggestCommand(click));
        }
        return line;
    }

    /**
     * Cancels private messages aimed at someone who blocked the sender.
     *
     * @param   event
     *          Command event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(@NotNull PlayerCommandPreprocessEvent event) {
        ChatManager chat = PaperChatHook.feature();
        if (chat == null) return;

        TabPlayer sender = HavocTab.getInstance().getPlayer(event.getPlayer().getUniqueId());
        if (sender == null) return;

        String raw = event.getMessage();
        if (raw.startsWith("/")) raw = raw.substring(1);
        String[] parts = raw.split(" ");
        if (parts.length == 0) return;

        String command = parts[0];
        int colon = command.indexOf(':'); // Strip plugin: prefixes such as essentials:msg
        if (colon != -1) command = command.substring(colon + 1);

        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        if (chat.isPrivateMessageBlocked(sender, command, args)) {
            event.setCancelled(true);
            sender.sendMessage(HavocTab.getInstance().getConfiguration().getMessages()
                    .getChatMessageBlocked(args.length > 0 ? args[0] : "?"));
        }
    }

    /**
     * Maps an Adventure audience back to a HavocTab player, or {@code null} for console
     * and for players HavocTab has not loaded yet.
     */
    @Nullable
    private TabPlayer resolve(@NotNull Audience audience) {
        if (!(audience instanceof Player)) return null;
        return HavocTab.getInstance().getPlayer(((Player) audience).getUniqueId());
    }
}
