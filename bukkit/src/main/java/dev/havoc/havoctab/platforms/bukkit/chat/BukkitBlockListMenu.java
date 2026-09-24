package dev.havoc.havoctab.platforms.bukkit.chat;

import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.chat.EnumChatFormat;
import dev.havoc.havoctab.shared.config.MessageFile;
import dev.havoc.havoctab.shared.features.chat.ChatManager;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Block list menu for Bukkit-based servers.
 * <p>
 * This is the single entry point HavocTab's chat feature calls; it decides which kind of
 * menu the player actually gets:
 * <ul>
 *     <li><b>Java players</b> get the native Paper dialog when this server has one.</li>
 *     <li><b>Bedrock players</b> get a chest GUI instead. Paper dialogs are a Java-edition
 *     protocol feature, so a Bedrock client either sees nothing or something broken.
 *     Geyser translates container UIs into native Bedrock forms, so a chest menu is the
 *     reliable way to show these players a working list.</li>
 *     <li>Anything else falls through to the clickable chat list.</li>
 * </ul>
 */
public class BukkitBlockListMenu implements ChatManager.BlockListView, Listener {

    /** Slots per chest row */
    private static final int ROW = 9;

    /** Most entries the GUI shows, leaving the bottom row for the close button */
    private static final int MAX_ENTRIES = 45;

    /**
     * Paper dialog opener, registered by the paper_chat module when the server supports
     * dialogs. Static so it survives {@code /havoctab reload}.
     */
    private static final AtomicReference<DialogOpener> DIALOG = new AtomicReference<>();

    /**
     * Registers the native dialog implementation.
     *
     * @param   opener
     *          Dialog opener, or {@code null} to remove it
     */
    public static void setDialogOpener(@Nullable DialogOpener opener) {
        DIALOG.set(opener);
    }

    /**
     * A platform-specific native dialog able to show the block list.
     */
    public interface DialogOpener {

        /**
         * Opens the dialog for the given player.
         *
         * @param   player
         *          Player to open it for
         * @return  {@code true} if it opened, {@code false} to fall back
         */
        boolean open(@NotNull TabPlayer player);
    }

    @Override
    public boolean open(@NotNull TabPlayer player) {
        ChatManager chat = HavocTab.getInstance().getFeatureManager()
                .getFeature(dev.havoc.havoctab.shared.TabConstants.Feature.CHAT);
        if (chat == null) return false;

        String type = chat.getConfiguration().getBlock().getMenuType().toUpperCase(java.util.Locale.US);
        if (type.equals("CHAT")) return false; // Caller prints the chat list

        boolean bedrock = player.isBedrockPlayer();
        boolean wantsDialog = type.equals("DIALOG") || (type.equals("AUTO") && !bedrock);

        if (wantsDialog) {
            DialogOpener opener = DIALOG.get();
            if (opener != null && opener.open(player)) return true;
            // Dialog unavailable or failed - fall through to the GUI rather than giving up
        }
        if (type.equals("DIALOG")) return false; // Explicitly dialog-only, let chat handle it
        return openGui(player);
    }

    /**
     * Opens the chest GUI version of the block list.
     *
     * @param   player
     *          Player to open the menu for
     * @return  {@code true} if the menu opened
     */
    private boolean openGui(@NotNull TabPlayer player) {
        Player bukkit = Bukkit.getPlayer(player.getUniqueId());
        if (bukkit == null) return false;

        MessageFile messages = HavocTab.getInstance().getConfiguration().getMessages();
        Map<UUID, String> blocked = new LinkedHashMap<>(player.chatData.getBlocked());
        // An empty chest is a bad way to say "you have not blocked anyone" - let the
        // caller print the message instead
        if (blocked.isEmpty()) return false;

        int entries = Math.min(blocked.size(), MAX_ENTRIES);
        int rows = Math.max(1, (entries + ROW - 1) / ROW) + 1; // +1 for the close row
        Holder holder = new Holder();
        Inventory inventory = Bukkit.createInventory(holder, rows * ROW,
                EnumChatFormat.color(messages.getBlockListTitle()));
        holder.inventory = inventory;

        int slot = 0;
        for (Map.Entry<UUID, String> entry : blocked.entrySet()) {
            if (slot >= entries) break;
            inventory.setItem(slot, head(entry.getKey(), entry.getValue(), messages));
            holder.slots.put(slot, entry.getKey());
            slot++;
        }

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(EnumChatFormat.color(messages.getBlockListClose()));
            close.setItemMeta(closeMeta);
        }
        inventory.setItem(rows * ROW - 1, close);

        bukkit.openInventory(inventory);
        return true;
    }

    /**
     * Builds the item representing one blocked player.
     * <p>
     * The skull owner is only set for players who are currently online, because resolving
     * an offline player's skin can block the main thread on a Mojang lookup.
     */
    @NotNull
    private ItemStack head(@NotNull UUID uniqueId, @NotNull String name, @NotNull MessageFile messages) {
        Material material = Material.getMaterial("PLAYER_HEAD");
        if (material == null) material = Material.getMaterial("SKULL_ITEM"); // 1.12 and below
        if (material == null) material = Material.PAPER;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(EnumChatFormat.color("&f" + name));
            List<String> lore = new ArrayList<>();
            lore.add(EnumChatFormat.color(messages.getBlockListUnblockTooltip(name)));
            meta.setLore(lore);
            if (meta instanceof SkullMeta) {
                Player online = Bukkit.getPlayer(uniqueId);
                if (online != null) {
                    try {
                        ((SkullMeta) meta).setOwningPlayer(online);
                    } catch (Throwable ignored) {
                        // Older API without setOwningPlayer, the head just stays blank
                    }
                }
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Handles clicks inside the block list GUI.
     *
     * @param   event
     *          Click event
     */
    @EventHandler
    public void onClick(@NotNull InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder)) return;
        event.setCancelled(true); // Never let anything be taken out of the menu

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player bukkit = (Player) event.getWhoClicked();
        Holder holder = (Holder) event.getInventory().getHolder();

        UUID target = holder.slots.get(event.getRawSlot());
        if (target == null) {
            // Close button or an empty slot
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.BARRIER) {
                bukkit.closeInventory();
            }
            return;
        }

        TabPlayer player = HavocTab.getInstance().getPlayer(bukkit.getUniqueId());
        if (player == null) return;
        ChatManager chat = HavocTab.getInstance().getFeatureManager()
                .getFeature(dev.havoc.havoctab.shared.TabConstants.Feature.CHAT);
        if (chat == null) return;

        String name = player.chatData.getBlockedName(target);
        if (chat.unblock(player, target)) {
            player.sendMessage(HavocTab.getInstance().getConfiguration().getMessages()
                    .getChatUnblocked(name == null ? target.toString() : name));
        }

        // Update the open menu in place rather than reopening it. Calling openInventory or
        // closeInventory from inside InventoryClickEvent is unreliable, and doing it this
        // way also avoids needing a scheduler, which would differ on Folia.
        event.getInventory().setItem(event.getRawSlot(), null);
        holder.slots.remove(event.getRawSlot());
    }

    /**
     * Marker holder identifying our inventory and mapping slots back to blocked players.
     * Using a holder rather than comparing titles means a renamed menu still works.
     */
    private static class Holder implements InventoryHolder {

        /** Slot index to blocked player UUID */
        private final Map<Integer, UUID> slots = new HashMap<>();

        /** Backing inventory */
        private Inventory inventory;

        @Override
        @NotNull
        public Inventory getInventory() {
            return inventory;
        }
    }

    /** Valid values of the menu-type config option, for validation messages */
    public static final List<String> MENU_TYPES = Arrays.asList("AUTO", "DIALOG", "GUI", "CHAT");
}
