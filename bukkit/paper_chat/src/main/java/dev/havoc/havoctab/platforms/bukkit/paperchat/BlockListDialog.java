package dev.havoc.havoctab.platforms.bukkit.paperchat;

import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.config.MessageFile;
import dev.havoc.havoctab.platforms.bukkit.chat.BukkitBlockListMenu;
import dev.havoc.havoctab.shared.features.chat.ChatManager;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import dev.havoc.havoctab.shared.util.cache.StringToComponentCache;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Native block list menu built on Paper's Dialog API (1.21.6+).
 * <p>
 * Each blocked player is one button; clicking it unblocks them and reopens the menu so
 * the list updates in place. If anything about the dialog fails - an older Paper build
 * with a different signature, for instance - {@link #open} returns {@code false} and
 * HavocTab falls back to the clickable chat list instead of erroring at the player.
 */
public class BlockListDialog implements Listener, BukkitBlockListMenu.DialogOpener {

    /** Namespace used for this menu's click identifiers */
    private static final String NAMESPACE = "havoctab";

    /** Prefix of the unblock button identifier, followed by the target's UUID */
    private static final String UNBLOCK_PREFIX = "unblock/";

    /** Buttons shown at most, to stay within a sane dialog size */
    private static final int MAX_BUTTONS = 40;

    /** Set once if the dialog fails, so a broken API version does not spam the console */
    private volatile boolean dialogBroken;

    /**
     * Returns whether this server has the Dialog API at all.
     *
     * @return  {@code true} if dialogs can be built
     */
    public static boolean isSupported() {
        try {
            Class.forName("io.papermc.paper.dialog.Dialog");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean open(@NotNull TabPlayer player) {
        if (dialogBroken) return false;
        Player bukkit = Bukkit.getPlayer(player.getUniqueId());
        if (bukkit == null) return false;
        try {
            bukkit.showDialog(build(player));
            return true;
        } catch (Throwable t) {
            dialogBroken = true;
            HavocTab.getInstance().getPlatform().logWarn(text(
                    "&cFailed to open the block list dialog, falling back to chat output. " +
                            "This usually means the server's Dialog API differs from the one HavocTab was built against: "
                            + t));
            return false;
        }
    }

    /**
     * Builds the dialog showing the player's current block list.
     *
     * @param   player
     *          Player whose block list should be shown
     * @return  Dialog ready to be shown
     */
    @NotNull
    private Dialog build(@NotNull TabPlayer player) {
        MessageFile messages = HavocTab.getInstance().getConfiguration().getMessages();
        Map<UUID, String> blocked = new LinkedHashMap<>(player.chatData.getBlocked());

        List<DialogBody> body = new ArrayList<>();
        List<ActionButton> buttons = new ArrayList<>();

        if (blocked.isEmpty()) {
            body.add(DialogBody.plainMessage(component(messages.getBlockListEmptyBody())));
        } else {
            body.add(DialogBody.plainMessage(component(messages.getBlockListBody())));
            int shown = 0;
            for (Map.Entry<UUID, String> entry : blocked.entrySet()) {
                if (shown++ >= MAX_BUTTONS) break;
                String name = entry.getValue();
                buttons.add(ActionButton.create(
                        component("&f" + name),
                        component(messages.getBlockListUnblockTooltip(name)),
                        200,
                        DialogAction.customClick(Key.key(NAMESPACE, UNBLOCK_PREFIX + entry.getKey()), null)
                ));
            }
            int hidden = blocked.size() - shown;
            if (hidden > 0) {
                body.add(DialogBody.plainMessage(component(messages.getBlockListMore(hidden))));
            }
        }

        ActionButton close = ActionButton.create(
                component(messages.getBlockListClose()),
                null,
                200,
                null // No action: the dialog simply closes
        );

        DialogBase base = DialogBase.builder(component(messages.getBlockListTitle()))
                .body(body)
                .canCloseWithEscape(true)
                .build();

        return Dialog.create(builder -> builder.empty()
                .base(base)
                .type(DialogType.multiAction(buttons, close, 1)));
    }

    /**
     * Handles clicks on the unblock buttons.
     *
     * @param   event
     *          Custom click event
     */
    @EventHandler
    public void onCustomClick(@NotNull PlayerCustomClickEvent event) {
        Key identifier = event.getIdentifier();
        if (!NAMESPACE.equals(identifier.namespace())) return;
        if (!identifier.value().startsWith(UNBLOCK_PREFIX)) return;
        if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;

        ChatManager chat = PaperChatHook.feature();
        if (chat == null) return;

        TabPlayer player = HavocTab.getInstance().getPlayer(connection.getPlayer().getUniqueId());
        if (player == null) return;

        UUID target;
        try {
            target = UUID.fromString(identifier.value().substring(UNBLOCK_PREFIX.length()));
        } catch (IllegalArgumentException e) {
            return;
        }

        String name = player.chatData.getBlockedName(target);
        if (chat.unblock(player, target)) {
            player.sendMessage(HavocTab.getInstance().getConfiguration().getMessages()
                    .getChatUnblocked(name == null ? target.toString() : name));
        }
        // Reopen through the chat feature so the player gets whichever menu suits their
        // client, and so an emptied list closes instead of showing a blank dialog
        if (player.chatData.getBlockedCount() > 0) chat.openBlockList(player);
    }

    @NotNull
    private static Component component(@NotNull String text) {
        return StringToComponentCache.GLOBAL.get(text).toAdventure();
    }

    @NotNull
    private static dev.havoc.havoctab.shared.chat.component.TabComponent text(@NotNull String message) {
        return StringToComponentCache.GLOBAL.get(message);
    }
}
