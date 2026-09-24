package dev.havoc.havoctab.shared.features.tags;

import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.config.file.ConfigurationFile;
import dev.havoc.havoctab.shared.config.file.ConfigurationSection;
import dev.havoc.havoctab.shared.config.MessageFile;
import dev.havoc.havoctab.shared.config.file.YamlConfigurationFile;
import dev.havoc.havoctab.shared.features.PlaceholderManagerImpl;
import dev.havoc.havoctab.shared.features.types.JoinListener;
import dev.havoc.havoctab.shared.features.types.Loadable;
import dev.havoc.havoctab.shared.features.types.UnLoadable;
import dev.havoc.havoctab.shared.features.nametags.NameTag;
import dev.havoc.havoctab.shared.features.playerlist.PlayerList;
import dev.havoc.havoctab.shared.features.types.TabFeature;
import dev.havoc.havoctab.shared.placeholders.types.PlayerPlaceholderImpl;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import dev.havoc.havoctab.shared.util.cache.StringToComponentCache;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Cosmetic player tags, defined in tags.yml and equipped through a menu.
 * <p>
 * Ownership is by permission: a player owns a tag when they have its {@code permission}
 * (by default {@code havoctab.tag.<id>}). That means your store, crates and rank packages
 * grant tags with no extra integration - they already grant permissions.
 * <p>
 * The equipped tag is exposed as {@code %havoctab_tag%}, so it can be dropped into a chat
 * format, tabprefix, nametag or anywhere else HavocTab parses placeholders.
 */
@Getter
public class TagManager extends TabFeature implements Loadable, UnLoadable, JoinListener {

    /** Path in playerdata.yml holding each player's equipped tag */
    private static final String EQUIPPED_PATH = "havoctab-tag-equipped";

    /** Path in playerdata.yml holding each player's favourites */
    private static final String FAVOURITES_PATH = "havoctab-tag-favourites";

    /** tags.yml */
    @NotNull private final ConfigurationFile file;

    /** All defined tags by id, in display order */
    @NotNull private final Map<String, TagDefinition> tags = new LinkedHashMap<>();

    /** Command that opens the menu and equips tags, without the slash */
    @NotNull private final String menuCommand;

    /**
     * Whether a player must unequip their current tag before putting a different one on.
     * <p>
     * With this on, clicking a second tag is refused rather than silently swapping, so the
     * menu never changes something the player did not mean to change.
     */
    private final boolean requireUnequipFirst;

    /** Whether tag feedback goes above the hotbar instead of into chat */
    private final boolean actionBarMessages;

    /** Whether the equipped tag is added to the tab list automatically */
    private final boolean displayInTablist;

    /** Whether the equipped tag is added to the nametag above the head automatically */
    private final boolean displayInNameTag;

    /** Whether the tag goes before the name rather than after it */
    private final boolean displayAsPrefix;

    /** Cache turning tag text into components */
    @NotNull private final StringToComponentCache componentCache = new StringToComponentCache("Tags", 500);

    /** Placeholders exposing the equipped tag */
    @Nullable private PlayerPlaceholderImpl[] placeholders;

    /**
     * Platform menu implementation. Static so it survives {@code /havoctab reload}, which
     * rebuilds features but leaves registered listeners alone.
     */
    private static final AtomicReference<TagMenu> MENU = new AtomicReference<>();

    /**
     * Which list of tags the menu is showing.
     */
    public enum View {

        /** Ordinary, non-limited tags */
        ALL,

        /** Limited tags only */
        LIMITED,

        /** Favourited tags, whether limited or not */
        FAVOURITES
    }

    /**
     * Outcome of an equip attempt, so the caller can send the right message.
     */
    public enum EquipResult {

        /** The tag is now equipped */
        EQUIPPED,

        /** The player's tag was taken off */
        UNEQUIPPED,

        /** No tag exists with that id */
        UNKNOWN,

        /** The player does not have the tag's permission */
        NOT_OWNED,

        /** Another tag is equipped and {@code require-unequip-first} is on */
        ALREADY_EQUIPPED
    }

    /**
     * A platform-specific tags menu.
     */
    public interface TagMenu {

        /**
         * Opens the menu.
         *
         * @param   player
         *          Player to open it for
         * @param   page
         *          Zero-based page to show
         * @param   view
         *          Which list of tags to show
         * @param   search
         *          Search term, or {@code null} for no filter
         * @return  {@code true} if the menu opened
         */
        boolean open(@NotNull TabPlayer player, int page, @NotNull View view, @Nullable String search);
    }

    /**
     * Registers the platform's tags menu.
     *
     * @param   menu
     *          Menu implementation, or {@code null} to remove it
     */
    public static void setMenu(@Nullable TagMenu menu) {
        MENU.set(menu);
    }

    /**
     * A place HavocTab can put the equipped tag without the server owner editing groups.yml.
     */
    public enum Slot {

        /** The player's line in the tab list */
        TABLIST,

        /** The name floating above the player's head */
        NAMETAG
    }

    /**
     * Live instance, so the static {@link #decorate} hook can reach the loaded feature.
     * <p>
     * Static because {@code PlayerList} and {@code NameTag} render formats on hot paths and
     * should not pay a feature-manager lookup per player per viewer.
     */
    private static final AtomicReference<TagManager> INSTANCE = new AtomicReference<>();

    /**
     * Appends (or prepends) the player's equipped tag to a rendered format.
     * <p>
     * This is what makes a tag show up with no config edits: without it the tag only appears
     * where {@code %havoctab_tag%} was manually placed, and because per-group values in
     * groups.yml override rather than combine, "add it to every group" means editing every
     * single group. Returns {@code text} untouched when tags are off, the slot is disabled,
     * or the player has nothing equipped, so the cost when unused is one null check.
     *
     * @param   player
     *          Player whose format is being built
     * @param   text
     *          Format rendered so far
     * @param   slot
     *          Where this text is going
     * @return  Text with the tag added, or the original text
     */
    @NotNull
    public static String decorate(@NotNull TabPlayer player, @NotNull String text, @NotNull Slot slot) {
        TagManager tags = INSTANCE.get();
        if (tags == null) return text;
        return tags.applyTag(player, text, slot);
    }

    /**
     * Returns whether the tag belongs in the given slot.
     * <p>
     * There are two separate ways a name reaches the tab list, and which one a server uses
     * depends on its config:
     * <ul>
     *   <li>{@code tablist-name-formatting} on - the name comes from {@code PlayerList}, and
     *       the tag is appended there.</li>
     *   <li>{@code tablist-name-formatting} off - the tab list falls back to the scoreboard
     *       team prefix and suffix, which Minecraft applies to the tab list <i>and</i> the
     *       name above the head.</li>
     * </ul>
     * So when the player list feature is not loaded, {@code display.tablist} is honoured
     * through the team suffix instead. Without this, turning tablist formatting off would
     * silently make tags stop appearing with no indication why.
     *
     * @param   slot
     *          Slot being rendered
     * @return  {@code true} if the tag should be added here
     */
    private boolean shouldShowIn(@NotNull Slot slot) {
        if (slot == Slot.TABLIST) return displayInTablist;
        if (displayInNameTag) return true;
        // Team suffix is the only route left to the tab list
        return displayInTablist && !isPlayerListLoaded();
    }

    /**
     * Returns whether the tablist formatting feature is active.
     *
     * @return  {@code true} if {@code tablist-name-formatting} is enabled
     */
    private boolean isPlayerListLoaded() {
        return HavocTab.getInstance().getFeatureManager()
                .getFeature(TabConstants.Feature.PLAYER_LIST) != null;
    }

    /**
     * Instance half of {@link #decorate}.
     */
    @NotNull
    private String applyTag(@NotNull TabPlayer player, @NotNull String text, @NotNull Slot slot) {
        if (!shouldShowIn(slot)) return text;

        TagDefinition tag = equippedTag(player);
        if (tag == null) return text;

        String display = tag.getDisplay();
        if (display.isEmpty()) return text;

        return displayAsPrefix ? display + text : text + display;
    }

    /**
     * Sends a short status line above the hotbar rather than into chat.
     */
    public interface ActionBarSender {

        /**
         * Sends an action bar message.
         *
         * @param   player
         *          Player to send to
         * @param   message
         *          Message with {@code &} colour codes
         * @return  {@code true} if it was sent, {@code false} to fall back to chat
         */
        boolean send(@NotNull TabPlayer player, @NotNull String message);
    }

    /**
     * Platform action bar implementation. Static for the same reason as {@link #MENU}.
     */
    private static final AtomicReference<ActionBarSender> ACTION_BAR = new AtomicReference<>();

    /**
     * Registers the platform's action bar sender.
     *
     * @param   sender
     *          Sender implementation, or {@code null} to remove it
     */
    public static void setActionBarSender(@Nullable ActionBarSender sender) {
        ACTION_BAR.set(sender);
    }

    /**
     * Sends tag feedback to a player, above the hotbar when possible.
     * <p>
     * Tag feedback is transient - it says what a click just did and is irrelevant a second
     * later - so it does not belong in chat history. Falls back to chat when the platform
     * has no action bar or the server is too old for one.
     *
     * @param   player
     *          Player to notify
     * @param   message
     *          Message to send
     */
    public void notify(@NotNull TabPlayer player, @NotNull String message) {
        if (actionBarMessages) {
            ActionBarSender sender = ACTION_BAR.get();
            if (sender != null && sender.send(player, message)) return;
        }
        player.sendMessage(message);
    }

    /**
     * Constructs new instance, creating tags.yml with a starter set if it does not exist.
     *
     * @throws  IOException
     *          If the file cannot be created or read
     */
    public TagManager() throws IOException {
        file = new YamlConfigurationFile(
                getClass().getClassLoader().getResourceAsStream("config/tags.yml"),
                new File(HavocTab.getInstance().getDataFolder(), "tags.yml"));

        menuCommand = stripSlash(file.getString("menu-command", "/tags"));
        requireUnequipFirst = file.getBoolean("require-unequip-first", true);
        // Options added after tags.yml first shipped. An existing tags.yml does not gain new
        // keys on its own, so they are written in on load - otherwise the only way to tell
        // whether a running jar even has a setting is to guess, and the server owner cannot
        // change a setting whose key is not in their file.
        addMissing("action-bar-messages", true);
        addMissing("display.tablist", true);
        addMissing("display.nametag", false);
        addMissing("display.position", "SUFFIX");

        actionBarMessages = file.getBoolean("action-bar-messages", true);
        displayInTablist = file.getBoolean("display.tablist", true);
        displayInNameTag = file.getBoolean("display.nametag", false);
        displayAsPrefix = file.getString("display.position", "SUFFIX")
                .equalsIgnoreCase("PREFIX");

        ConfigurationSection section = file.getConfigurationSection("tags");
        List<TagDefinition> loaded = new ArrayList<>();
        for (Object key : section.getKeys()) {
            String id = key.toString();
            loaded.add(TagDefinition.fromSection(id, section.getConfigurationSection(id)));
        }
        loaded.sort(Comparator.comparingInt(TagDefinition::getWeight)
                .thenComparing(TagDefinition::getId, String.CASE_INSENSITIVE_ORDER));
        for (TagDefinition tag : loaded) {
            tags.put(tag.getId().toLowerCase(Locale.US), tag);
        }
    }

    @Override
    public void load() {
        INSTANCE.set(this);
        PlaceholderManagerImpl manager = HavocTab.getInstance().getPlaceholderManager();
        placeholders = new PlayerPlaceholderImpl[]{
                manager.registerPlayerPlaceholder(TabConstants.Placeholder.EQUIPPED_TAG, -1,
                        p -> displayOf((TabPlayer) p)),
                manager.registerPlayerPlaceholder(TabConstants.Placeholder.EQUIPPED_TAG_SHORT, -1,
                        p -> displayOf((TabPlayer) p))
        };

        // One command does everything. A separate /tag would collide with vanilla Minecraft's
        // /tag command, which exists on every version since 1.13.
        HavocTab.getInstance().getPlatform().registerCustomCommand(menuCommand, (player, args) -> {
            if (player != null) handleTagCommand(player, args);
        });

        for (TabPlayer player : HavocTab.getInstance().getOnlinePlayers()) {
            loadPlayer(player);
        }
    }

    @Override
    public void unload() {
        // Cleared so the static render hook stops firing the moment the feature goes away,
        // rather than decorating formats with a dead instance's config after a reload
        INSTANCE.compareAndSet(this, null);
    }

    @Override
    public void onJoin(@NotNull TabPlayer connectedPlayer) {
        loadPlayer(connectedPlayer);
    }

    private void loadPlayer(@NotNull TabPlayer player) {
        ConfigurationFile data = HavocTab.getInstance().getConfiguration().getPlayerData();
        String equipped = data.getString(EQUIPPED_PATH + "." + player.getUniqueId(), null);
        if (equipped != null && tags.containsKey(equipped.toLowerCase(Locale.US))) {
            player.tagData.equipped = equipped.toLowerCase(Locale.US);
        }
        player.tagData.setFavourites(data.getStringList(
                FAVOURITES_PATH + "." + player.getUniqueId(), new ArrayList<>()));
        updatePlaceholders(player);
    }

    // ------------------
    // Queries
    // ------------------

    /**
     * Returns the rendered display text of a player's equipped tag, or an empty string.
     *
     * @param   player
     *          Player to check
     * @return  Tag text, or empty when nothing is equipped or the tag no longer exists
     */
    @NotNull
    public String displayOf(@NotNull TabPlayer player) {
        TagDefinition tag = equippedTag(player);
        return tag == null ? "" : tag.getDisplay();
    }

    /**
     * Returns the player's equipped tag definition, or {@code null}.
     * <p>
     * Also returns {@code null} when the player no longer has permission for it, so a tag
     * that came with an expired rank stops showing without any cleanup step.
     *
     * @param   player
     *          Player to check
     * @return  Equipped tag, or {@code null}
     */
    @Nullable
    public TagDefinition equippedTag(@NotNull TabPlayer player) {
        String id = player.tagData.equipped;
        if (id == null) return null;
        TagDefinition tag = tags.get(id);
        if (tag == null) return null;
        return owns(player, tag) ? tag : null;
    }

    /**
     * Returns whether the player owns the given tag.
     *
     * @param   player
     *          Player to check
     * @param   tag
     *          Tag to check
     * @return  {@code true} if owned
     */
    public boolean owns(@NotNull TabPlayer player, @NotNull TagDefinition tag) {
        return player.hasPermission(tag.getPermission())
                || player.hasPermission(TabConstants.Permission.COMMAND_ALL);
    }

    /**
     * Returns the tags this player owns in the given view, optionally filtered by a search.
     * <p>
     * Favourites are listed first within the result, so starring a tag moves it to the front
     * of the menu rather than only changing its lore.
     *
     * @param   player
     *          Player to list for
     * @param   view
     *          Which list to build
     * @param   search
     *          Case-insensitive filter on id and name, or {@code null}
     * @return  Matching tags, favourites first, then in display order
     */
    @NotNull
    public List<TagDefinition> ownedTags(@NotNull TabPlayer player, @NotNull View view, @Nullable String search) {
        List<TagDefinition> favourites = new ArrayList<>();
        List<TagDefinition> others = new ArrayList<>();
        String needle = search == null ? null : search.toLowerCase(Locale.US);

        for (TagDefinition tag : tags.values()) {
            boolean favourite = player.tagData.isFavourite(tag.getId());
            switch (view) {
                case ALL:
                    if (tag.isLimited()) continue;
                    break;
                case LIMITED:
                    if (!tag.isLimited()) continue;
                    break;
                case FAVOURITES:
                    // Favourites span both lists, so limited tags are included here
                    if (!favourite) continue;
                    break;
            }
            if (!owns(player, tag)) continue;
            if (needle != null
                    && !tag.getId().toLowerCase(Locale.US).contains(needle)
                    && !stripColors(tag.getName()).toLowerCase(Locale.US).contains(needle)) continue;
            (favourite ? favourites : others).add(tag);
        }

        favourites.addAll(others);
        return favourites;
    }

    /**
     * Returns how many tags this player owns and has favourited.
     *
     * @param   player
     *          Player to count for
     * @return  Number of owned favourites
     */
    public int favouriteCount(@NotNull TabPlayer player) {
        int count = 0;
        for (TagDefinition tag : tags.values()) {
            if (owns(player, tag) && player.tagData.isFavourite(tag.getId())) count++;
        }
        return count;
    }

    /**
     * Returns how many tags this player owns in total, both ordinary and limited.
     *
     * @param   player
     *          Player to count for
     * @return  Number of owned tags
     */
    public int ownedCount(@NotNull TabPlayer player) {
        int count = 0;
        for (TagDefinition tag : tags.values()) {
            if (owns(player, tag)) count++;
        }
        return count;
    }

    /**
     * Returns how many limited tags this player owns.
     *
     * @param   player
     *          Player to count for
     * @return  Number of owned limited tags
     */
    public int ownedLimitedCount(@NotNull TabPlayer player) {
        int count = 0;
        for (TagDefinition tag : tags.values()) {
            if (tag.isLimited() && owns(player, tag)) count++;
        }
        return count;
    }

    // ------------------
    // Actions
    // ------------------

    /**
     * Equips a tag, or takes the current one off when {@code id} is {@code null}.
     * <p>
     * When {@code require-unequip-first} is on, equipping while another tag is already worn
     * is refused. Clicking the tag you are already wearing always takes it off, so there is
     * no state the player can get stuck in.
     *
     * @param   player
     *          Player to change
     * @param   id
     *          Tag id, or {@code null} to take the current tag off
     * @return  What happened
     */
    @NotNull
    public EquipResult equip(@NotNull TabPlayer player, @Nullable String id) {
        if (id == null) {
            player.tagData.equipped = null;
            save(player);
            updatePlaceholders(player);
            refreshDisplay(player);
            return EquipResult.UNEQUIPPED;
        }

        TagDefinition tag = tags.get(id.toLowerCase(Locale.US));
        if (tag == null) return EquipResult.UNKNOWN;
        if (!owns(player, tag)) return EquipResult.NOT_OWNED;

        String key = tag.getId().toLowerCase(Locale.US);

        // Clicking the equipped tag takes it off
        if (key.equals(player.tagData.equipped)) {
            player.tagData.equipped = null;
            save(player);
            updatePlaceholders(player);
            refreshDisplay(player);
            return EquipResult.UNEQUIPPED;
        }

        // equippedTag() rather than the raw field, so a tag whose permission was lost, or one
        // deleted from tags.yml, never blocks the player from equipping something else
        if (requireUnequipFirst && equippedTag(player) != null) {
            return EquipResult.ALREADY_EQUIPPED;
        }

        player.tagData.equipped = key;
        save(player);
        updatePlaceholders(player);
        refreshDisplay(player);
        return EquipResult.EQUIPPED;
    }

    /**
     * Applies an equip attempt and tells the player what happened.
     *
     * @param   player
     *          Player to change
     * @param   id
     *          Tag id, or {@code null} to take the current tag off
     * @return  What happened
     */
    @NotNull
    public EquipResult equipAndNotify(@NotNull TabPlayer player, @Nullable String id) {
        MessageFile messages = HavocTab.getInstance().getConfiguration().getMessages();
        EquipResult result = equip(player, id);
        String name = displayNameOf(id);
        switch (result) {
            case EQUIPPED:
                notify(player, messages.getTagEquipped(name));
                break;
            case UNEQUIPPED:
                notify(player, messages.getTagCleared());
                break;
            case UNKNOWN:
                notify(player, messages.getTagNotFound(name));
                break;
            case NOT_OWNED:
                notify(player, messages.getTagLocked(name));
                break;
            case ALREADY_EQUIPPED:
                TagDefinition current = equippedTag(player);
                notify(player, messages.getTagAlreadyEquipped(
                        current == null ? name : current.getName()));
                break;
        }
        return result;
    }

    /**
     * Returns a tag's menu name, falling back to the raw id when it is not a known tag.
     *
     * @param   id
     *          Tag id, or {@code null}
     * @return  Readable name
     */
    @NotNull
    private String displayNameOf(@Nullable String id) {
        if (id == null) return "";
        TagDefinition tag = tags.get(id.toLowerCase(Locale.US));
        return tag == null ? id : tag.getName();
    }

    /**
     * Toggles whether a tag is favourited.
     *
     * @param   player
     *          Player to change
     * @param   id
     *          Tag id
     * @return  {@code true} if it is now favourited
     */
    public boolean toggleFavourite(@NotNull TabPlayer player, @NotNull String id) {
        boolean now = player.tagData.toggleFavourite(id.toLowerCase(Locale.US));
        HavocTab.getInstance().getConfiguration().getPlayerData().set(
                FAVOURITES_PATH + "." + player.getUniqueId(),
                new ArrayList<>(player.tagData.getFavourites()));
        return now;
    }

    /**
     * Toggles a favourite and tells the player what happened.
     *
     * @param   player
     *          Player to change
     * @param   id
     *          Tag id
     * @return  {@code true} if it is now favourited
     */
    public boolean toggleFavouriteAndNotify(@NotNull TabPlayer player, @NotNull String id) {
        MessageFile messages = HavocTab.getInstance().getConfiguration().getMessages();
        boolean now = toggleFavourite(player, id);
        String name = displayNameOf(id);
        notify(player, now ? messages.getTagFavourited(name) : messages.getTagUnfavourited(name));
        return now;
    }

    private void save(@NotNull TabPlayer player) {
        HavocTab.getInstance().getConfiguration().getPlayerData()
                .set(EQUIPPED_PATH + "." + player.getUniqueId(), player.tagData.equipped);
    }

    /**
     * Opens the tags menu, falling back to a chat listing when no menu is available.
     *
     * @param   player
     *          Player to open for
     * @param   page
     *          Zero-based page
     * @param   view
     *          Which list of tags to show
     * @param   search
     *          Search filter, or {@code null}
     */
    public void openMenu(@NotNull TabPlayer player, int page, @NotNull View view, @Nullable String search) {
        TagMenu menu = MENU.get();
        if (menu != null && menu.open(player, page, view, search)) return;

        MessageFile messages = HavocTab.getInstance().getConfiguration().getMessages();
        List<TagDefinition> owned = ownedTags(player, view, search);
        if (owned.isEmpty()) {
            notify(player, messages.getTagsNone());
            return;
        }
        player.sendMessage(messages.getTagsHeader(owned.size()));
        for (TagDefinition tag : owned) {
            player.sendMessage(messages.getTagsEntry(tag.getName(), tag.getId(), "/" + menuCommand));
        }
    }

    /**
     * Handles {@code /tags}, which both opens the menu and changes the equipped tag.
     *
     * @param   player
     *          Player who ran the command
     * @param   args
     *          Command arguments
     */
    private void handleTagCommand(@NotNull TabPlayer player, @NotNull String[] args) {
        if (args.length == 0) {
            openMenu(player, 0, View.ALL, null);
            return;
        }
        String first = args[0].toLowerCase(Locale.US);
        switch (first) {
            case "clear":
            case "none":
            case "off":
            case "remove":
            case "unequip":
                equipAndNotify(player, null);
                return;
            case "limited":
                openMenu(player, 0, View.LIMITED, null);
                return;
            case "favourites":
            case "favorites":
            case "favs":
                openMenu(player, 0, View.FAVOURITES, null);
                return;
            default:
                equipAndNotify(player, first);
        }
    }

    private void updatePlaceholders(@NotNull TabPlayer player) {
        if (placeholders == null) return;
        for (PlayerPlaceholderImpl placeholder : placeholders) {
            placeholder.update(player);
        }
    }

    /**
     * Redraws everywhere the tag is shown automatically.
     * <p>
     * Placeholder updates alone are not enough here: with {@code display.tablist} the tag is
     * appended while the format is rendered, so no property actually changed and nothing
     * would otherwise trigger a resend. Only called when a tag changes, not on a timer.
     *
     * @param   player
     *          Player whose tag changed
     */
    private void refreshDisplay(@NotNull TabPlayer player) {
        if (shouldShowIn(Slot.TABLIST)) {
            PlayerList playerList = HavocTab.getInstance().getFeatureManager()
                    .getFeature(TabConstants.Feature.PLAYER_LIST);
            if (playerList != null) playerList.formatPlayerForEveryone(player, true);
        }
        if (shouldShowIn(Slot.NAMETAG)) {
            NameTag nameTags = HavocTab.getInstance().getNameTagManager();
            if (nameTags != null) nameTags.refreshPrefixSuffixForPlayer(player);
        }
    }

    /**
     * Removes colour codes so a search matches what the player actually reads.
     *
     * @param   text
     *          Text to strip
     * @return  Text without formatting codes
     */
    @NotNull
    public static String stripColors(@NotNull String text) {
        return text.replaceAll("(?i)[&§][0-9a-fk-or]", "")
                .replaceAll("(?i)[&§]#[0-9a-f]{6}", "")
                .replaceAll("<[^>]*>", "");
    }

    /**
     * Writes a setting into tags.yml if the file does not already have it.
     * <p>
     * Only touches the file when something was actually missing, so a server whose tags.yml
     * is already current pays nothing and its file is never rewritten.
     *
     * @param   path
     *          Config path to ensure exists
     * @param   value
     *          Value to write when it is absent
     */
    private void addMissing(@NotNull String path, @NotNull Object value) {
        if (file.getObject(path) != null) return;
        file.set(path, value);
    }

    @NotNull
    private static String stripSlash(@NotNull String command) {
        return command.startsWith("/") ? command.substring(1) : command;
    }

    @NotNull
    @Override
    public String getFeatureName() {
        return "Tags";
    }
}
