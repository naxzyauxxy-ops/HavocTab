package dev.havoc.havoctab.shared.features.chat;

import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.Property;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.config.file.ConfigurationFile;
import dev.havoc.havoctab.shared.features.PlaceholderManagerImpl;
import dev.havoc.havoctab.shared.features.types.JoinListener;
import dev.havoc.havoctab.shared.features.types.Loadable;
import dev.havoc.havoctab.shared.features.types.RefreshableFeature;
import dev.havoc.havoctab.shared.placeholders.types.PlayerPlaceholderImpl;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import dev.havoc.havoctab.shared.util.PerformanceUtil;
import dev.havoc.havoctab.shared.util.cache.StringToComponentCache;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * HavocTab's chat feature.
 * <p>
 * Owns three things, all of them per-viewer:
 * <ul>
 *     <li><b>Formatting</b> - the chat line is rendered once per recipient, so placeholders
 *     in the format can differ per viewer and a viewer can opt out of the format entirely.</li>
 *     <li><b>Public chat toggle</b> - a viewer can stop receiving public chat without
 *     affecting anyone else.</li>
 *     <li><b>Blocking</b> - {@code /block} and {@code /ignore} are the same command; a blocked
 *     player's messages (and optionally private messages) never reach the blocker.</li>
 * </ul>
 * The platform-specific listener asks this class what to send and to whom; all the
 * decisions live here so every platform behaves identically.
 */
@Getter
public class ChatManager extends RefreshableFeature implements JoinListener, Loadable {

    /** Path in playerdata.yml under which block lists are stored */
    private static final String BLOCKED_PATH = "havoctab-blocked";

    /** Path in playerdata.yml under which public chat toggles are stored */
    private static final String PUBLIC_CHAT_PATH = "havoctab-publicchat-hidden";

    /** Path in playerdata.yml under which chat format toggles are stored */
    private static final String CHAT_FORMAT_PATH = "havoctab-chatformat-disabled";

    /** Feature configuration */
    @NotNull private final ChatConfiguration configuration;

    /** Status placeholders registered by this feature */
    @Nullable private PlayerPlaceholderImpl[] statusPlaceholders;

    /** Cache turning rendered chat text into components, shared with the platform listener */
    @NotNull private final StringToComponentCache componentCache = new StringToComponentCache("Chat", 1000);

    /**
     * Constructs new instance with the given configuration.
     *
     * @param   configuration
     *          Feature configuration
     */
    public ChatManager(@NotNull ChatConfiguration configuration) {
        this.configuration = configuration;
    }

    // ------------------
    // Loading
    // ------------------

    @Override
    public void load() {
        PlaceholderManagerImpl placeholders = HavocTab.getInstance().getPlaceholderManager();
        statusPlaceholders = new PlayerPlaceholderImpl[]{
                placeholders.registerPlayerPlaceholder(TabConstants.Placeholder.PUBLIC_CHAT_VISIBLE, -1,
                        p -> formatStatus(!((TabPlayer) p).chatData.publicChatHidden)),
                placeholders.registerPlayerPlaceholder(TabConstants.Placeholder.PUBLIC_CHAT_VISIBLE_SHORT, -1,
                        p -> formatStatus(!((TabPlayer) p).chatData.publicChatHidden)),
                placeholders.registerPlayerPlaceholder(TabConstants.Placeholder.CHAT_FORMAT_VISIBLE, -1,
                        p -> formatStatus(!((TabPlayer) p).chatData.formatDisabled)),
                placeholders.registerPlayerPlaceholder(TabConstants.Placeholder.CHAT_FORMAT_VISIBLE_SHORT, -1,
                        p -> formatStatus(!((TabPlayer) p).chatData.formatDisabled)),
                placeholders.registerPlayerPlaceholder(TabConstants.Placeholder.BLOCKED_COUNT, -1,
                        p -> PerformanceUtil.toString(((TabPlayer) p).chatData.getBlockedCount())),
                placeholders.registerPlayerPlaceholder(TabConstants.Placeholder.BLOCKED_COUNT_SHORT, -1,
                        p -> PerformanceUtil.toString(((TabPlayer) p).chatData.getBlockedCount()))
        };

        registerCommands();

        for (TabPlayer player : HavocTab.getInstance().getOnlinePlayers()) {
            loadPlayer(player);
        }
    }

    private void registerCommands() {
        if (configuration.getPublicChat().isEnabled()) {
            HavocTab.getInstance().getPlatform().registerCustomCommand(
                    stripSlash(configuration.getPublicChat().getToggleCommand()),
                    (player, args) -> {
                        if (player == null) return;
                        boolean visible = resolveTarget(args, !player.chatData.publicChatHidden);
                        setPublicChatVisible(player, visible, true);
                    });
        }
        if (configuration.getChatFormat().isEnabled()) {
            HavocTab.getInstance().getPlatform().registerCustomCommand(
                    stripSlash(configuration.getChatFormat().getToggleCommand()),
                    (player, args) -> {
                        if (player == null) return;
                        boolean visible = resolveTarget(args, !player.chatData.formatDisabled);
                        setChatFormatEnabled(player, visible, true);
                    });
        }
        if (!configuration.getBlock().isEnabled()) return;

        for (String command : configuration.getBlock().getBlockCommands()) {
            HavocTab.getInstance().getPlatform().registerCustomCommand(stripSlash(command),
                    (player, args) -> handleBlockCommand(player, args));
        }
        for (String command : configuration.getBlock().getUnblockCommands()) {
            HavocTab.getInstance().getPlatform().registerCustomCommand(stripSlash(command),
                    (player, args) -> handleUnblockCommand(player, args));
        }
        for (String command : configuration.getBlock().getListCommands()) {
            HavocTab.getInstance().getPlatform().registerCustomCommand(stripSlash(command),
                    (player, args) -> {
                        if (player != null) openBlockList(player);
                    });
        }
    }

    /**
     * Reads {@code on} / {@code off} out of command arguments, falling back to toggling.
     *
     * @param   args
     *          Command arguments
     * @param   current
     *          Current state, returned inverted when no explicit argument was given
     * @return  Requested new state
     */
    private boolean resolveTarget(@NotNull String[] args, boolean current) {
        if (args.length > 0 && args[0].equalsIgnoreCase("on")) return true;
        if (args.length > 0 && args[0].equalsIgnoreCase("off")) return false;
        return !current;
    }

    @Override
    public void onJoin(@NotNull TabPlayer connectedPlayer) {
        loadPlayer(connectedPlayer);
    }

    /**
     * Loads a player's chat properties and saved preferences.
     *
     * @param   player
     *          Player to load
     */
    private void loadPlayer(@NotNull TabPlayer player) {
        loadProperties(player);

        ConfigurationFile data = HavocTab.getInstance().getConfiguration().getPlayerData();
        String id = player.getUniqueId().toString();

        if (configuration.getPublicChat().isRememberToggleChoice()) {
            boolean stored = data.getStringList(PUBLIC_CHAT_PATH, new ArrayList<>()).contains(id);
            player.chatData.publicChatHidden = configuration.getPublicChat().isHiddenByDefault() != stored;
        } else {
            player.chatData.publicChatHidden = configuration.getPublicChat().isHiddenByDefault();
        }

        if (configuration.getChatFormat().isRememberToggleChoice()) {
            boolean stored = data.getStringList(CHAT_FORMAT_PATH, new ArrayList<>()).contains(id);
            player.chatData.formatDisabled = configuration.getChatFormat().isHiddenByDefault() != stored;
        } else {
            player.chatData.formatDisabled = configuration.getChatFormat().isHiddenByDefault();
        }

        player.chatData.setBlocked(readBlockList(player));

        player.expansionData.setPublicChatVisible(!player.chatData.publicChatHidden);
        player.expansionData.setChatFormatVisible(!player.chatData.formatDisabled);
        updatePlaceholders(player);
    }

    /**
     * Loads chatprefix and chatsuffix from groups.yml / users.yml.
     *
     * @param   player
     *          Player to load properties for
     */
    public void loadProperties(@NotNull TabPlayer player) {
        player.chatData.prefix = player.loadPropertyFromConfig(this, "chatprefix", "");
        player.chatData.suffix = player.loadPropertyFromConfig(this, "chatsuffix", "");

        // The format, hover and click command are Properties rather than raw strings on
        // purpose. Creating a Property registers every placeholder it contains as "used",
        // which is what puts them into the refresh cycle. Parsing the raw config string by
        // hand skips that registration, and unregistered placeholders never get a value -
        // they would render as literal %placeholder% text forever.
        player.chatData.format = new Property(this, player, configuration.getFormat());
        player.chatData.plainFormat = new Property(this, player, configuration.getPlainFormat());
        player.chatData.hover = new Property(this, player, String.join("\n", configuration.getHoverLore()));
        player.chatData.clickCommand = new Property(this, player, configuration.getClickCommand());
    }

    @NotNull
    @Override
    public String getRefreshDisplayName() {
        return "Updating chat prefix/suffix";
    }

    @Override
    public void refresh(@NotNull TabPlayer refreshed, boolean force) {
        if (refreshed.chatData.prefix == null) return; // Not loaded yet
        if (force) {
            refreshed.updatePropertyFromConfig(refreshed.chatData.prefix, "");
            refreshed.updatePropertyFromConfig(refreshed.chatData.suffix, "");
        } else {
            refreshed.chatData.prefix.update();
            refreshed.chatData.suffix.update();
        }
        refreshed.chatData.format.update();
        refreshed.chatData.plainFormat.update();
        refreshed.chatData.hover.update();
        refreshed.chatData.clickCommand.update();
    }

    // ------------------
    // Block list persistence
    // ------------------

    @NotNull
    private Map<UUID, String> readBlockList(@NotNull TabPlayer player) {
        Map<UUID, String> result = new LinkedHashMap<>();
        List<String> stored = HavocTab.getInstance().getConfiguration().getPlayerData()
                .getStringList(BLOCKED_PATH + "." + player.getUniqueId(), new ArrayList<>());
        for (String entry : stored) {
            // Stored as "<uuid> <last known name>"
            int space = entry.indexOf(' ');
            String rawId = space == -1 ? entry : entry.substring(0, space);
            String name = space == -1 ? "?" : entry.substring(space + 1);
            try {
                result.put(UUID.fromString(rawId), name);
            } catch (IllegalArgumentException ignored) {
                // Corrupted entry, drop it silently rather than failing the player's join
            }
        }
        return result;
    }

    private void saveBlockList(@NotNull TabPlayer player) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : player.chatData.getBlocked().entrySet()) {
            out.add(entry.getKey() + " " + entry.getValue());
        }
        HavocTab.getInstance().getConfiguration().getPlayerData()
                .set(BLOCKED_PATH + "." + player.getUniqueId(), out);
    }

    private void saveToggle(@NotNull String path, @NotNull TabPlayer player, boolean stored) {
        ConfigurationFile data = HavocTab.getInstance().getConfiguration().getPlayerData();
        List<String> list = new ArrayList<>(data.getStringList(path, new ArrayList<>()));
        String id = player.getUniqueId().toString();
        boolean changed = stored ? (!list.contains(id) && list.add(id)) : list.remove(id);
        if (changed) data.set(path, list);
    }

    // ------------------
    // Commands
    // ------------------

    private void handleBlockCommand(@Nullable TabPlayer player, @NotNull String[] args) {
        if (player == null) return;
        if (args.length == 0) {
            openBlockList(player);
            return;
        }
        TabPlayer target = findPlayer(args[0]);
        if (target == null) {
            player.sendMessage(HavocTab.getInstance().getConfiguration().getMessages().getPlayerNotFound(args[0]));
            return;
        }
        if (target == player) {
            player.sendMessage(HavocTab.getInstance().getConfiguration().getMessages().getChatBlockSelf());
            return;
        }
        if (player.chatData.hasBlocked(target.getUniqueId())) {
            player.sendMessage(HavocTab.getInstance().getConfiguration().getMessages().getChatAlreadyBlocked(target.getName()));
            return;
        }
        int max = configuration.getBlock().getMaxBlocked();
        if (max > 0 && player.chatData.getBlockedCount() >= max) {
            player.sendMessage(HavocTab.getInstance().getConfiguration().getMessages().getChatBlockLimit(max));
            return;
        }
        block(player, target.getUniqueId(), target.getName());
        player.sendMessage(HavocTab.getInstance().getConfiguration().getMessages().getChatBlocked(target.getName()));
    }

    private void handleUnblockCommand(@Nullable TabPlayer player, @NotNull String[] args) {
        if (player == null) return;
        if (args.length == 0) {
            openBlockList(player);
            return;
        }
        // Match by stored name first so offline players can still be unblocked
        UUID found = null;
        String name = args[0];
        for (Map.Entry<UUID, String> entry : player.chatData.getBlocked().entrySet()) {
            if (entry.getValue().equalsIgnoreCase(name)) {
                found = entry.getKey();
                break;
            }
        }
        if (found == null) {
            TabPlayer target = findPlayer(name);
            if (target != null && player.chatData.hasBlocked(target.getUniqueId())) found = target.getUniqueId();
        }
        if (found == null) {
            player.sendMessage(HavocTab.getInstance().getConfiguration().getMessages().getChatNotBlocked(name));
            return;
        }
        unblock(player, found);
        player.sendMessage(HavocTab.getInstance().getConfiguration().getMessages().getChatUnblocked(name));
    }

    @Nullable
    private TabPlayer findPlayer(@NotNull String name) {
        for (TabPlayer player : HavocTab.getInstance().getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name)) return player;
        }
        return null;
    }

    /**
     * Opens the block list menu. The platform hook is asked first so Paper servers get the
     * native dialog; anything else falls back to a clickable chat list.
     *
     * @param   player
     *          Player to show the block list to
     */
    public void openBlockList(@NotNull TabPlayer player) {
        BlockListView view = BLOCK_LIST_VIEW.get();
        if (view != null && view.open(player)) return;
        sendChatBlockList(player);
    }

    private void sendChatBlockList(@NotNull TabPlayer player) {
        Map<UUID, String> blocked = player.chatData.getBlocked();
        if (blocked.isEmpty()) {
            player.sendMessage(HavocTab.getInstance().getConfiguration().getMessages().getChatBlockListEmpty());
            return;
        }
        player.sendMessage(HavocTab.getInstance().getConfiguration().getMessages().getChatBlockListHeader(blocked.size()));
        String unblockCommand = configuration.getBlock().getUnblockCommands().isEmpty()
                ? "/unblock" : configuration.getBlock().getUnblockCommands().get(0);
        for (String name : blocked.values()) {
            player.sendMessage(HavocTab.getInstance().getConfiguration().getMessages()
                    .getChatBlockListEntry(name, unblockCommand));
        }
    }

    // ------------------
    // State changes
    // ------------------

    /**
     * Blocks a player for the given viewer and persists the change.
     *
     * @param   player
     *          Viewer doing the blocking
     * @param   target
     *          UUID of the player being blocked
     * @param   name
     *          Name to remember the blocked player by
     */
    public void block(@NotNull TabPlayer player, @NotNull UUID target, @NotNull String name) {
        if (player.chatData.addBlocked(target, name)) {
            saveBlockList(player);
            updatePlaceholders(player);
        }
    }

    /**
     * Unblocks a player for the given viewer and persists the change.
     *
     * @param   player
     *          Viewer doing the unblocking
     * @param   target
     *          UUID of the player being unblocked
     * @return  {@code true} if the player was blocked and is now not
     */
    public boolean unblock(@NotNull TabPlayer player, @NotNull UUID target) {
        if (player.chatData.removeBlocked(target)) {
            saveBlockList(player);
            updatePlaceholders(player);
            return true;
        }
        return false;
    }

    /**
     * Turns public chat on or off for a single viewer.
     *
     * @param   player
     *          Viewer to change the setting for
     * @param   visible
     *          {@code true} to receive public chat
     * @param   sendMessage
     *          Whether to tell the player about the change
     */
    public void setPublicChatVisible(@NotNull TabPlayer player, boolean visible, boolean sendMessage) {
        player.chatData.publicChatHidden = !visible;
        if (configuration.getPublicChat().isRememberToggleChoice()) {
            saveToggle(PUBLIC_CHAT_PATH, player, configuration.getPublicChat().isHiddenByDefault() == visible);
        }
        player.expansionData.setPublicChatVisible(visible);
        updatePlaceholders(player);
        if (sendMessage) {
            player.sendMessage(visible
                    ? HavocTab.getInstance().getConfiguration().getMessages().getPublicChatOn()
                    : HavocTab.getInstance().getConfiguration().getMessages().getPublicChatOff());
        }
    }

    /**
     * Turns the chat format on or off for a single viewer. When off, that viewer sees
     * the plain format instead - everyone else still sees the styled one.
     *
     * @param   player
     *          Viewer to change the setting for
     * @param   enabled
     *          {@code true} to use the configured format
     * @param   sendMessage
     *          Whether to tell the player about the change
     */
    public void setChatFormatEnabled(@NotNull TabPlayer player, boolean enabled, boolean sendMessage) {
        player.chatData.formatDisabled = !enabled;
        if (configuration.getChatFormat().isRememberToggleChoice()) {
            saveToggle(CHAT_FORMAT_PATH, player, configuration.getChatFormat().isHiddenByDefault() == enabled);
        }
        player.expansionData.setChatFormatVisible(enabled);
        updatePlaceholders(player);
        if (sendMessage) {
            player.sendMessage(enabled
                    ? HavocTab.getInstance().getConfiguration().getMessages().getChatFormatOn()
                    : HavocTab.getInstance().getConfiguration().getMessages().getChatFormatOff());
        }
    }

    // ------------------
    // Rendering, called by the platform listener
    // ------------------

    /**
     * Returns whether the viewer should receive a public chat message from the sender.
     *
     * @param   viewer
     *          Player who would receive the message
     * @param   sender
     *          Player who sent the message
     * @return  {@code true} if the message should be delivered
     */
    public boolean shouldReceive(@NotNull TabPlayer viewer, @NotNull TabPlayer sender) {
        if (viewer == sender) return true; // Never hide your own messages from yourself
        if (viewer.chatData.publicChatHidden) return false;
        return !viewer.chatData.hasBlocked(sender.getUniqueId());
    }

    /**
     * Renders the chat line template for one viewer, with every placeholder resolved but
     * {@code %message%} left in place.
     * <p>
     * The message is deliberately NOT substituted here. The platform splits the result on
     * {@code %message%} and appends the player's text as its own component, so a player
     * cannot inject placeholders or colour codes into the format by typing them.
     *
     * @param   sender
     *          Player who sent the message
     * @param   viewer
     *          Player who will see this line
     * @return  Parsed template still containing {@code %message%}
     */
    @NotNull
    public String renderTemplate(@NotNull TabPlayer sender, @NotNull TabPlayer viewer) {
        Property template = viewer.chatData.formatDisabled ? sender.chatData.plainFormat : sender.chatData.format;
        return applyPrefixSuffix(template.updateAndGet(), template.getFormat(viewer), sender, viewer);
    }

    /**
     * Renders the hover card lines for one viewer.
     *
     * @param   sender
     *          Player the hover card describes
     * @param   viewer
     *          Player who will see the hover card
     * @return  Rendered lines, empty if hover is disabled
     */
    @NotNull
    public List<String> renderHover(@NotNull TabPlayer sender, @NotNull TabPlayer viewer) {
        if (!configuration.isHoverEnabled() || viewer.chatData.formatDisabled) return new ArrayList<>();
        Property hover = sender.chatData.hover;
        hover.update();
        String resolved = applyPrefixSuffix(hover.get(), hover.getFormat(viewer), sender, viewer);
        if (resolved.isEmpty()) return new ArrayList<>();
        List<String> out = new ArrayList<>();
        Collections.addAll(out, resolved.split("\n", -1));
        return out;
    }

    /**
     * Returns the click command for a message, with placeholders resolved.
     *
     * @param   sender
     *          Player who sent the message
     * @param   viewer
     *          Player who would click
     * @return  Command to run, or an empty string when clicking is disabled
     */
    @NotNull
    public String renderClickCommand(@NotNull TabPlayer sender, @NotNull TabPlayer viewer) {
        if (configuration.getClickCommand().isEmpty() || viewer.chatData.formatDisabled) return "";
        Property click = sender.chatData.clickCommand;
        click.update();
        return applyPrefixSuffix(click.get(), click.getFormat(viewer), sender, viewer);
    }

    /**
     * Substitutes %chatprefix% and %chatsuffix% into an already resolved Property value.
     * <p>
     * These two are handled here rather than by the Property itself because they are not
     * registered placeholders - they are HavocTab properties loaded from groups.yml, and
     * each is resolved for the viewer so relational placeholders inside a group's prefix
     * still work.
     *
     * @param   updated
     *          Result of updating the property, unused except to force the update
     * @param   text
     *          Property value already resolved for the viewer
     * @param   sender
     *          Player who sent the message
     * @param   viewer
     *          Player the result is shown to
     * @return  Text with the chat prefix and suffix substituted in
     */
    @NotNull
    private String applyPrefixSuffix(@NotNull String updated, @NotNull String text,
                                     @NotNull TabPlayer sender, @NotNull TabPlayer viewer) {
        String out = text;
        Property prefix = sender.chatData.prefix;
        Property suffix = sender.chatData.suffix;
        if (prefix != null && out.contains("%chatprefix%")) {
            prefix.update();
            out = out.replace("%chatprefix%", prefix.getFormat(viewer));
        }
        if (suffix != null && out.contains("%chatsuffix%")) {
            suffix.update();
            out = out.replace("%chatsuffix%", suffix.getFormat(viewer));
        }
        return out;
    }

    // ------------------
    // Private message blocking
    // ------------------

    /**
     * Returns whether the given command should be cancelled because its target has
     * blocked the sender. Only handles commands whose first argument is the target.
     *
     * @param   sender
     *          Player running the command
     * @param   command
     *          Command name without the leading slash
     * @param   args
     *          Command arguments
     * @return  {@code true} if the command should be cancelled
     */
    public boolean isPrivateMessageBlocked(@NotNull TabPlayer sender, @NotNull String command, @NotNull String[] args) {
        if (!configuration.getBlock().isEnabled()) return false;
        if (!configuration.getBlock().isBlockPrivateMessages()) return false;
        if (args.length == 0) return false;
        boolean matches = false;
        for (String candidate : configuration.getBlock().getPrivateMessageCommands()) {
            if (candidate.equalsIgnoreCase(command)) {
                matches = true;
                break;
            }
        }
        if (!matches) return false;
        TabPlayer target = findPlayer(args[0]);
        if (target == null) return false;
        return target.chatData.hasBlocked(sender.getUniqueId());
    }

    // ------------------
    // Helpers
    // ------------------

    /**
     * Platform hook able to open a native block list menu, {@code null} if unsupported.
     * Static so it survives {@code /havoctab reload}, which recreates every feature but
     * leaves the platform's registered listeners in place.
     */
    private static final AtomicReference<BlockListView> BLOCK_LIST_VIEW = new AtomicReference<>();

    /**
     * Registers the platform's native block list menu. Called by the platform on startup
     * when it has one; without it the block list is printed into chat instead.
     *
     * @param   view
     *          Menu implementation to use
     */
    public static void setBlockListView(@Nullable BlockListView view) {
        BLOCK_LIST_VIEW.set(view);
    }

    /**
     * A platform-specific block list menu.
     */
    public interface BlockListView {

        /**
         * Opens the menu for the given player.
         *
         * @param   player
         *          Player to open the menu for
         * @return  {@code true} if the menu was opened, {@code false} to fall back to chat
         */
        boolean open(@NotNull TabPlayer player);
    }

    private void updatePlaceholders(@NotNull TabPlayer player) {
        if (statusPlaceholders == null) return;
        for (PlayerPlaceholderImpl placeholder : statusPlaceholders) {
            placeholder.update(player);
        }
    }

    @NotNull
    private String formatStatus(boolean on) {
        return on
                ? HavocTab.getInstance().getConfiguration().getConfig().getClientDisplay().getValueOn()
                : HavocTab.getInstance().getConfiguration().getConfig().getClientDisplay().getValueOff();
    }

    @NotNull
    private static String stripSlash(@NotNull String command) {
        return command.startsWith("/") ? command.substring(1) : command;
    }

    @NotNull
    @Override
    public String getFeatureName() {
        return "Chat";
    }
}
