package dev.havoc.havoctab.platforms.bukkit.paperchat;

import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.features.chat.ChatFilter;
import dev.havoc.havoctab.shared.features.chat.ChatManager;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

        String plain = PlainTextComponentSerializer.plainText().serialize(event.message());

        // Filter first: a blocked message should never reach anyone, and should not cost
        // us the work of resolving viewers or mentions.
        ChatFilter.Result filtered = chat.getFilter().check(sender, plain);
        if (filtered.blocked) {
            event.setCancelled(true);
            if (filtered.notice != null) sender.sendMessage(filtered.notice);
            return;
        }
        if (!filtered.message.equals(plain)) {
            // The filter rewrote it (caps lowered, runs collapsed, a word censored)
            plain = filtered.message;
            event.message(Component.text(plain));
        }

        removeHiddenViewers(event, chat, sender);

        // Mentions are resolved once per message. The pings must not live in the renderer,
        // which runs once per recipient and would ping the same player repeatedly.
        List<ChatManager.MentionSegment> segments = chat.findMentions(plain, sender);
        notifyMentions(chat, sender, segments);

        if (!chat.getConfiguration().isFormatEnabled()) return;

        event.renderer((source, sourceDisplayName, message, viewer) ->
                render(chat, sender, message, segments, viewer));
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
     * Sends the action bar ping and sound to everyone mentioned in the message.
     * <p>
     * Deliberately an action bar rather than a chat message, so a ping never adds a line
     * to anyone's chat.
     */
    private void notifyMentions(@NotNull ChatManager chat, @NotNull TabPlayer sender,
                                @NotNull List<ChatManager.MentionSegment> segments) {
        Set<TabPlayer> pinged = new HashSet<>();
        for (ChatManager.MentionSegment segment : segments) {
            TabPlayer mentioned = segment.mentioned;
            if (mentioned == null) continue;
            if (!pinged.add(mentioned)) continue; // Named twice in one message, ping once
            if (!chat.shouldNotifyMention(mentioned, sender)) continue;

            Player bukkit = Bukkit.getPlayer(mentioned.getUniqueId());
            if (bukkit == null) continue;

            bukkit.sendActionBar(chat.getComponentCache()
                    .get(chat.renderMentionActionBar(sender)).toAdventure());
            playPingSound(chat, bukkit);
        }
    }

    /**
     * Plays the configured mention sound, accepting either a Bukkit enum name or a
     * namespaced sound key so the config works across versions.
     */
    private void playPingSound(@NotNull ChatManager chat, @NotNull Player bukkit) {
        if (!chat.getConfiguration().getMentions().isSoundEnabled()) return;
        String name = chat.getConfiguration().getMentions().getSoundName();
        float volume = chat.getConfiguration().getMentions().getSoundVolume();
        float pitch = chat.getConfiguration().getMentions().getSoundPitch();
        try {
            bukkit.playSound(bukkit.getLocation(), Sound.valueOf(name.toUpperCase(Locale.US)), volume, pitch);
        } catch (IllegalArgumentException e) {
            // Not an enum constant on this version, try it as a raw sound key
            try {
                bukkit.playSound(bukkit.getLocation(), name.toLowerCase(Locale.US), volume, pitch);
            } catch (Throwable ignored) {
                // Invalid sound name, silently skip rather than spamming per mention
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
                             @NotNull Component message, @NotNull List<ChatManager.MentionSegment> segments,
                             @NotNull Audience audience) {
        TabPlayer viewer = resolve(audience);
        if (viewer == null) viewer = sender; // Console: render as the sender would see it

        Component body = buildMessage(chat, message, segments);
        String template = chat.renderTemplate(sender, viewer);
        int index = template.indexOf("%message%");

        Component line;
        if (index == -1) {
            // Format has no %message% placeholder, append the message at the end
            line = Component.empty()
                    .append(chat.getComponentCache().get(template).toAdventure())
                    .append(body);
        } else {
            String before = template.substring(0, index);
            String after = template.substring(index + "%message%".length());
            line = Component.empty()
                    .append(chat.getComponentCache().get(before).toAdventure())
                    .append(body)
                    .append(chat.getComponentCache().get(after).toAdventure());
        }

        return decorate(chat, sender, viewer, line);
    }

    /**
     * Builds the message body, highlighting any mentioned names.
     * <p>
     * When nothing is mentioned the original component is returned untouched, so a message
     * another plugin already styled is only rebuilt when there is a reason to.
     */
    @NotNull
    private Component buildMessage(@NotNull ChatManager chat, @NotNull Component message,
                                   @NotNull List<ChatManager.MentionSegment> segments) {
        boolean hasMention = false;
        for (ChatManager.MentionSegment segment : segments) {
            if (segment.mentioned != null) {
                hasMention = true;
                break;
            }
        }
        if (!hasMention) return message;

        Component body = Component.empty();
        for (ChatManager.MentionSegment segment : segments) {
            if (segment.mentioned == null) {
                body = body.append(Component.text(segment.text));
            } else {
                body = body.append(chat.getComponentCache().get(chat.renderMention(segment)).toAdventure());
            }
        }
        return body;
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
