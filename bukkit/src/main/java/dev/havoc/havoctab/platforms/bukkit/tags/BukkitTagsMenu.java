package dev.havoc.havoctab.platforms.bukkit.tags;

import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.chat.EnumChatFormat;
import dev.havoc.havoctab.shared.config.MessageFile;
import dev.havoc.havoctab.shared.features.tags.TagDefinition;
import dev.havoc.havoctab.shared.features.tags.TagManager;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Chest GUI for HavocTab's cosmetic tags.
 * <p>
 * Layout mirrors a five row menu: statistics and the limited tags shortcut across the top,
 * owned tags filling the middle, and navigation plus clear, search and favourites along the
 * bottom.
 * <p>
 * Every label comes from messages.yml, so the whole thing can be restyled without touching
 * code, and all of it goes through HavocTab's text parser - legacy {@code &3} codes, hex,
 * gradients, MiniMessage and {@code <font:...>} all work.
 * <p>
 * <b>Opening an inventory from inside {@link InventoryClickEvent} does not work reliably.</b>
 * The client is mid-transaction, so the packet is either dropped or races the close packet,
 * and the player sees a click that appears to do nothing. Clicks that keep the same title
 * therefore repaint the open inventory in place, and clicks that change the title (a page or
 * view change, where the title carries the page number) reopen it on the next tick instead.
 */
public class BukkitTagsMenu implements TagManager.TagMenu, Listener {

    /** Slots per chest row */
    private static final int ROW = 9;

    /** Rows in the menu */
    private static final int ROWS = 5;

    /** First slot tags are placed in */
    private static final int TAG_START = 9;

    /** How many tag slots each page holds (rows 2 to 4) */
    private static final int TAGS_PER_PAGE = 27;

    // Fixed slot positions
    private static final int SLOT_STATS = 3;
    private static final int SLOT_LIMITED = 5;
    private static final int SLOT_PREVIOUS = 37;
    private static final int SLOT_CLEAR = 39;
    private static final int SLOT_SEARCH = 40;
    private static final int SLOT_FAVOURITES = 41;
    private static final int SLOT_NEXT = 43;

    /** Players currently typing a search term in chat, mapped to the view they came from */
    @NotNull
    private final Map<UUID, TagManager.View> awaitingSearch = new HashMap<>();

    /** Plugin instance, needed for scheduling */
    @NotNull
    private final org.bukkit.plugin.java.JavaPlugin plugin;

    /**
     * Constructs new instance.
     *
     * @param   plugin
     *          Plugin instance used for scheduling
     */
    public BukkitTagsMenu(@NotNull org.bukkit.plugin.java.JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean open(@NotNull TabPlayer player, int page, @NotNull TagManager.View view, @Nullable String search) {
        Player bukkit = Bukkit.getPlayer(player.getUniqueId());
        if (bukkit == null) return false;

        TagManager tags = feature();
        if (tags == null) return false;

        MessageFile messages = HavocTab.getInstance().getConfiguration().getMessages();
        int size = tags.ownedTags(player, view, search).size();
        int pages = Math.max(1, (size + TAGS_PER_PAGE - 1) / TAGS_PER_PAGE);
        if (page < 0) page = pages - 1;
        if (page >= pages) page = 0;

        Holder holder = new Holder(page, view, search);
        Inventory inventory = Bukkit.createInventory(holder, ROWS * ROW,
                EnumChatFormat.color(messages.getTagsMenuTitle(page + 1, pages)));
        holder.inventory = inventory;

        render(player, tags, holder);
        bukkit.openInventory(inventory);
        return true;
    }

    /**
     * Fills an already-created inventory with the contents its holder describes.
     * <p>
     * Split out from {@link #open} so a click can repaint the menu the player already has
     * open instead of sending them a new one.
     *
     * @param   player
     *          Player the menu belongs to
     * @param   tags
     *          Tag feature
     * @param   holder
     *          Holder describing what to draw
     */
    private void render(@NotNull TabPlayer player, @NotNull TagManager tags, @NotNull Holder holder) {
        MessageFile messages = HavocTab.getInstance().getConfiguration().getMessages();
        Inventory inventory = holder.inventory;
        inventory.clear();
        holder.slots.clear();
        holder.limitedButton = -1;
        holder.backButton = -1;

        List<TagDefinition> owned = tags.ownedTags(player, holder.view, holder.search);
        int pages = Math.max(1, (owned.size() + TAGS_PER_PAGE - 1) / TAGS_PER_PAGE);

        // Tags for this page
        int from = holder.page * TAGS_PER_PAGE;
        for (int i = 0; i < TAGS_PER_PAGE && from + i < owned.size(); i++) {
            TagDefinition tag = owned.get(from + i);
            inventory.setItem(TAG_START + i, tagItem(player, tag, messages));
            holder.slots.put(TAG_START + i, tag.getId());
        }

        // Top row
        inventory.setItem(SLOT_STATS, item(material("LANTERN", Material.PAPER),
                messages.getTagsStatsName(),
                messages.getTagsStatsLore(
                        tags.ownedCount(player),
                        tags.ownedLimitedCount(player),
                        tags.favouriteCount(player),
                        equippedName(tags, player, messages))));

        if (holder.view == TagManager.View.ALL) {
            inventory.setItem(SLOT_LIMITED, item(material("ENDER_CHEST", Material.CHEST),
                    messages.getTagsLimitedName(),
                    messages.getTagsLimitedLore(tags.ownedLimitedCount(player))));
            holder.limitedButton = SLOT_LIMITED;
        } else {
            inventory.setItem(SLOT_LIMITED, item(material("ENDER_CHEST", Material.CHEST),
                    messages.getTagsBackName(), messages.getTagsBackLore()));
            holder.backButton = SLOT_LIMITED;
        }

        // Bottom row
        if (pages > 1) {
            inventory.setItem(SLOT_PREVIOUS, item(pane("RED"), messages.getTagsPreviousName(),
                    messages.getTagsPreviousLore()));
            inventory.setItem(SLOT_NEXT, item(pane("LIME"), messages.getTagsNextName(),
                    messages.getTagsNextLore()));
        }
        inventory.setItem(SLOT_CLEAR, item(Material.BARRIER, messages.getTagsClearName(),
                messages.getTagsClearLore(equippedName(tags, player, messages))));
        inventory.setItem(SLOT_SEARCH, item(material("OAK_SIGN", material("SIGN", Material.PAPER)),
                messages.getTagsSearchName(), messages.getTagsSearchLore()));
        inventory.setItem(SLOT_FAVOURITES, item(material("SUNFLOWER", Material.GOLD_INGOT),
                messages.getTagsFavouritesName(),
                messages.getTagsFavouritesLore(tags.favouriteCount(player))));
    }

    /**
     * Builds the item for one tag, including its status block.
     */
    @NotNull
    private ItemStack tagItem(@NotNull TabPlayer player, @NotNull TagDefinition tag,
                              @NotNull MessageFile messages) {
        Material material = material(tag.getMaterial().toUpperCase(Locale.US),
                material("NAME_TAG", Material.PAPER));

        boolean equipped = tag.getId().equalsIgnoreCase(player.tagData.equipped);
        boolean favourite = player.tagData.isFavourite(tag.getId());

        List<String> lore = new ArrayList<>(tag.getLore());
        lore.addAll(messages.getTagsItemLore(tag.getDisplay(), tag.getId(), equipped, favourite));
        return item(material, tag.getName(), lore);
    }

    @NotNull
    private String equippedName(@NotNull TagManager tags, @NotNull TabPlayer player, @NotNull MessageFile messages) {
        TagDefinition equipped = tags.equippedTag(player);
        return equipped == null ? messages.getTagsNoneEquipped() : equipped.getName();
    }

    /**
     * Handles clicks inside the tags menu.
     *
     * @param   event
     *          Click event
     */
    @EventHandler
    public void onClick(@NotNull InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player bukkit = (Player) event.getWhoClicked();
        Holder holder = (Holder) event.getInventory().getHolder();
        TabPlayer player = HavocTab.getInstance().getPlayer(bukkit.getUniqueId());
        TagManager tags = feature();
        if (player == null || tags == null) return;

        int slot = event.getRawSlot();
        MessageFile messages = HavocTab.getInstance().getConfiguration().getMessages();

        // --- Clicks that change the title: reopen next tick ---

        if (slot == SLOT_PREVIOUS) {
            reopen(bukkit, player, holder.page - 1, holder.view, holder.search);
            return;
        }
        if (slot == SLOT_NEXT) {
            reopen(bukkit, player, holder.page + 1, holder.view, holder.search);
            return;
        }
        if (slot == holder.limitedButton) {
            reopen(bukkit, player, 0, TagManager.View.LIMITED, null);
            return;
        }
        if (slot == holder.backButton) {
            reopen(bukkit, player, 0, TagManager.View.ALL, null);
            return;
        }
        if (slot == SLOT_FAVOURITES) {
            // Toggle between the favourites list and everything
            TagManager.View target = holder.view == TagManager.View.FAVOURITES
                    ? TagManager.View.ALL : TagManager.View.FAVOURITES;
            reopen(bukkit, player, 0, target, null);
            return;
        }
        if (slot == SLOT_SEARCH) {
            awaitingSearch.put(bukkit.getUniqueId(), holder.view);
            bukkit.closeInventory();
            player.sendMessage(messages.getTagsSearchPrompt());
            return;
        }

        // --- Clicks that keep the title: repaint in place ---

        if (slot == SLOT_CLEAR) {
            tags.equipAndNotify(player, null);
            repaint(bukkit, player, tags, holder);
            return;
        }

        String id = holder.slots.get(slot);
        if (id == null) return;

        if (event.getClick() == ClickType.RIGHT) {
            tags.toggleFavouriteAndNotify(player, id);
            // Favouriting reorders the list, and in the favourites view it adds or removes an
            // entry, so the page count can change - that needs a fresh title
            if (holder.view == TagManager.View.FAVOURITES) {
                reopen(bukkit, player, holder.page, holder.view, holder.search);
            } else {
                repaint(bukkit, player, tags, holder);
            }
            return;
        }

        tags.equipAndNotify(player, id);
        repaint(bukkit, player, tags, holder);
    }

    /**
     * Repaints the inventory the player already has open.
     *
     * @param   bukkit
     *          Bukkit player, so the client can be told to refresh
     * @param   player
     *          HavocTab player
     * @param   tags
     *          Tag feature
     * @param   holder
     *          Holder of the open menu
     */
    private void repaint(@NotNull Player bukkit, @NotNull TabPlayer player,
                         @NotNull TagManager tags, @NotNull Holder holder) {
        render(player, tags, holder);
        bukkit.updateInventory();
    }

    /**
     * Reopens the menu on the next tick with new contents.
     * <p>
     * Deferred by a tick because the client is still processing the click that triggered it.
     *
     * @param   bukkit
     *          Bukkit player
     * @param   player
     *          HavocTab player
     * @param   page
     *          Page to show
     * @param   view
     *          View to show
     * @param   search
     *          Search filter, or {@code null}
     */
    private void reopen(@NotNull Player bukkit, @NotNull TabPlayer player, int page,
                        @NotNull TagManager.View view, @Nullable String search) {
        runNextTick(bukkit, () -> open(player, page, view, search));
    }

    /**
     * Catches the search term a player types after pressing the search button.
     *
     * @param   event
     *          Chat event
     */
    @EventHandler
    public void onChat(@NotNull AsyncPlayerChatEvent event) {
        TagManager.View view = awaitingSearch.remove(event.getPlayer().getUniqueId());
        if (view == null) return;
        event.setCancelled(true);

        TabPlayer player = HavocTab.getInstance().getPlayer(event.getPlayer().getUniqueId());
        if (player == null) return;

        String term = event.getMessage().trim();
        boolean cancel = term.equalsIgnoreCase("cancel");
        // Opening an inventory must happen on the main thread
        runNextTick(event.getPlayer(), () ->
                open(player, 0, view, cancel || term.isEmpty() ? null : term));
    }

    // ------------------
    // Scheduling
    // ------------------

    /**
     * Runs a task on the next tick, on the thread that owns this player.
     * <p>
     * Folia has no global main thread, so {@code Bukkit.getScheduler()} throws there. Paper
     * and Folia both expose a per-entity scheduler; it is reached reflectively so this class
     * still compiles and runs against plain Bukkit, which has neither.
     *
     * @param   player
     *          Player the task concerns
     * @param   task
     *          Task to run
     */
    @SuppressWarnings("unchecked")
    private void runNextTick(@NotNull Player player, @NotNull Runnable task) {
        try {
            Object scheduler = Player.class.getMethod("getScheduler").invoke(player);
            scheduler.getClass()
                    .getMethod("run", org.bukkit.plugin.Plugin.class, Consumer.class, Runnable.class)
                    .invoke(scheduler, plugin, (Consumer<Object>) ignored -> task.run(), null);
            return;
        } catch (Throwable ignored) {
            // Not Paper or Folia - fall through to the global scheduler
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    // ------------------
    // Item helpers
    // ------------------

    @NotNull
    private ItemStack item(@NotNull Material material, @NotNull String name, @NotNull List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(EnumChatFormat.color(name));
            List<String> coloured = new ArrayList<>();
            for (String line : lore) coloured.add(EnumChatFormat.color(line));
            meta.setLore(coloured);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Looks up a material by name, falling back when the version does not have it.
     *
     * @param   name
     *          Material name to try
     * @param   fallback
     *          Material to use when it does not exist
     * @return  Resolved material
     */
    @NotNull
    private Material material(@NotNull String name, @NotNull Material fallback) {
        Material material = Material.getMaterial(name);
        return material == null ? fallback : material;
    }

    @NotNull
    private Material pane(@NotNull String colour) {
        return material(colour + "_STAINED_GLASS_PANE",
                material("STAINED_GLASS_PANE", Material.PAPER)); // 1.12 and below
    }

    @Nullable
    private TagManager feature() {
        HavocTab instance = HavocTab.getInstance();
        if (instance == null) return null;
        return instance.getFeatureManager().getFeature(TabConstants.Feature.TAGS);
    }

    /**
     * Holder identifying the menu and remembering what it is showing.
     */
    private static class Holder implements InventoryHolder {

        private final int page;
        @NotNull private final TagManager.View view;
        @Nullable private final String search;
        private final Map<Integer, String> slots = new HashMap<>();
        private int limitedButton = -1;
        private int backButton = -1;
        private Inventory inventory;

        private Holder(int page, @NotNull TagManager.View view, @Nullable String search) {
            this.page = page;
            this.view = view;
            this.search = search;
        }

        @Override
        @NotNull
        public Inventory getInventory() {
            return inventory;
        }
    }
}
