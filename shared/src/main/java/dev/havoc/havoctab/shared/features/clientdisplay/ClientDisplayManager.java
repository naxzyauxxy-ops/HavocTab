package dev.havoc.havoctab.shared.features.clientdisplay;

import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.features.PlaceholderManagerImpl;
import dev.havoc.havoctab.shared.features.ToggleManager;
import dev.havoc.havoctab.shared.features.belowname.BelowName;
import dev.havoc.havoctab.shared.features.nametags.NameTag;
import dev.havoc.havoctab.shared.features.playerlist.PlayerList;
import dev.havoc.havoctab.shared.features.types.JoinListener;
import dev.havoc.havoctab.shared.features.types.Loadable;
import dev.havoc.havoctab.shared.features.types.TabFeature;
import dev.havoc.havoctab.shared.placeholders.types.PlayerPlaceholderImpl;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * HavocTab feature adding <b>client-side</b> display toggles.
 * <p>
 * "Client-side" here means per-viewer: when a player turns ranks off, only that player
 * stops seeing rank prefixes and suffixes. Everyone else keeps seeing them, and the
 * player's own rank is unchanged for everybody else. Nothing is removed server-side,
 * no permissions are touched and no other player is affected.
 * <p>
 * Two independent toggles are provided:
 * <ul>
 *     <li><b>Ranks</b> - hides tablist and/or nametag prefixes and suffixes for the viewer.</li>
 *     <li><b>Belowname</b> - hides the belowname objective (text under player nametags,
 *     including animated ones such as {@code %animation:belowname_cycle%}) for the viewer.</li>
 * </ul>
 */
@Getter
public class ClientDisplayManager extends TabFeature implements JoinListener, Loadable {

    /** Feature configuration */
    @NotNull private final ClientDisplayConfiguration configuration;

    /** Remembers rank toggle choices across sessions, {@code null} if remembering is disabled */
    @Nullable private ToggleManager rankToggleManager;

    /** Remembers belowname toggle choices across sessions, {@code null} if remembering is disabled */
    @Nullable private ToggleManager belowNameToggleManager;

    /** Status placeholders registered by this feature, refreshed whenever a toggle changes */
    @Nullable private PlayerPlaceholderImpl[] statusPlaceholders;

    /**
     * Constructs new instance with given configuration and prepares toggle persistence.
     *
     * @param   configuration
     *          Feature configuration
     */
    public ClientDisplayManager(@NotNull ClientDisplayConfiguration configuration) {
        this.configuration = configuration;
        if (configuration.getRanks().isEnabled() && configuration.getRanks().isRememberToggleChoice()) {
            rankToggleManager = new ToggleManager(HavocTab.getInstance().getConfiguration().getPlayerData(), "havoctab-ranks-hidden");
        }
        if (configuration.getBelowName().isEnabled() && configuration.getBelowName().isRememberToggleChoice()) {
            belowNameToggleManager = new ToggleManager(HavocTab.getInstance().getConfiguration().getPlayerData(), "havoctab-belowname-hidden");
        }
    }

    @Override
    public void load() {
        PlaceholderManagerImpl placeholders = HavocTab.getInstance().getPlaceholderManager();

        // Status placeholders. Default output is "&aON" / "&cOFF", both configurable.
        // Refresh is -1 (event driven) - they are updated by hand when a toggle changes.
        statusPlaceholders = new PlayerPlaceholderImpl[]{
                placeholders.registerPlayerPlaceholder(TabConstants.Placeholder.RANKS_VISIBLE, -1,
                        p -> formatStatus(!((TabPlayer) p).clientDisplayData.ranksHidden)),
                placeholders.registerPlayerPlaceholder(TabConstants.Placeholder.RANKS_VISIBLE_SHORT, -1,
                        p -> formatStatus(!((TabPlayer) p).clientDisplayData.ranksHidden)),
                placeholders.registerPlayerPlaceholder(TabConstants.Placeholder.BELOWNAME_VISIBLE, -1,
                        p -> formatStatus(!((TabPlayer) p).clientDisplayData.belowNameHidden)),
                placeholders.registerPlayerPlaceholder(TabConstants.Placeholder.BELOWNAME_VISIBLE_SHORT, -1,
                        p -> formatStatus(!((TabPlayer) p).clientDisplayData.belowNameHidden))
        };

        // Toggle commands
        if (configuration.getRanks().isEnabled()) {
            HavocTab.getInstance().getPlatform().registerCustomCommand(
                    stripSlash(configuration.getRanks().getToggleCommand()),
                    (player, args) -> handleCommand(player, args, true));
        }
        if (configuration.getBelowName().isEnabled()) {
            HavocTab.getInstance().getPlatform().registerCustomCommand(
                    stripSlash(configuration.getBelowName().getToggleCommand()),
                    (player, args) -> handleCommand(player, args, false));
        }

        for (TabPlayer player : HavocTab.getInstance().getOnlinePlayers()) {
            loadPlayer(player);
        }
    }

    @Override
    public void onJoin(@NotNull TabPlayer connectedPlayer) {
        loadPlayer(connectedPlayer);
    }

    /**
     * Loads a player's saved preferences into memory. This only sets flags, it does not
     * send anything, because the features that read the flags run their own join logic
     * right after this feature does.
     *
     * @param   player
     *          Player to load
     */
    private void loadPlayer(@NotNull TabPlayer player) {
        ClientDisplayPlayerData data = player.clientDisplayData;
        data.ranksAffectTablist = configuration.getRanks().isHideTablist();
        data.ranksAffectNameTags = configuration.getRanks().isHideNameTags();

        if (configuration.getRanks().isEnabled()) {
            if (rankToggleManager != null) {
                rankToggleManager.convert(player);
                data.ranksHidden = configuration.getRanks().isHiddenByDefault() != rankToggleManager.contains(player);
            } else {
                data.ranksHidden = configuration.getRanks().isHiddenByDefault();
            }
        } else {
            data.ranksHidden = false;
        }

        if (configuration.getBelowName().isEnabled()) {
            if (belowNameToggleManager != null) {
                belowNameToggleManager.convert(player);
                data.belowNameHidden = configuration.getBelowName().isHiddenByDefault() != belowNameToggleManager.contains(player);
            } else {
                data.belowNameHidden = configuration.getBelowName().isHiddenByDefault();
            }
        } else {
            data.belowNameHidden = false;
        }

        player.expansionData.setRanksVisible(!data.ranksHidden);
        player.expansionData.setBelowNameVisible(!data.belowNameHidden);
        updatePlaceholders(player);
    }

    /**
     * Handles the {@code /ranks} and {@code /belowname} commands.
     *
     * @param   player
     *          Player who ran the command
     * @param   args
     *          Command arguments, supports {@code on}, {@code off} and {@code toggle}
     * @param   ranks
     *          {@code true} for the ranks toggle, {@code false} for the belowname toggle
     */
    private void handleCommand(@Nullable TabPlayer player, @NotNull String[] args, boolean ranks) {
        if (!isActive() || player == null) return;

        boolean requirePermission = ranks
                ? configuration.getRanks().isRequirePermission()
                : configuration.getBelowName().isRequirePermission();
        String permission = ranks ? TabConstants.Permission.COMMAND_RANKS_TOGGLE : TabConstants.Permission.COMMAND_BELOWNAME_TOGGLE;
        if (requirePermission && !player.hasPermission(permission) && !player.hasPermission(TabConstants.Permission.COMMAND_ALL)) {
            player.sendMessage(HavocTab.getInstance().getConfiguration().getMessages().getNoPermission());
            return;
        }

        boolean currentlyVisible = ranks ? !player.clientDisplayData.ranksHidden : !player.clientDisplayData.belowNameHidden;
        boolean newVisible;
        if (args.length > 0 && args[0].equalsIgnoreCase("on")) {
            newVisible = true;
        } else if (args.length > 0 && args[0].equalsIgnoreCase("off")) {
            newVisible = false;
        } else {
            newVisible = !currentlyVisible;
        }

        if (ranks) {
            setRanksVisible(player, newVisible, true);
        } else {
            setBelowNameVisible(player, newVisible, true);
        }
    }

    /**
     * Changes rank visibility for the given viewer and resends everything they need to see.
     *
     * @param   player
     *          Viewer to change the setting for
     * @param   visible
     *          {@code true} to show ranks, {@code false} to hide them
     * @param   sendMessage
     *          Whether the player should be told about the change
     */
    public void setRanksVisible(@NotNull TabPlayer player, boolean visible, boolean sendMessage) {
        if (player.clientDisplayData.ranksHidden == !visible) {
            // Already in requested state, only send the message
            if (sendMessage) sendToggleMessage(player, visible, true);
            return;
        }
        player.clientDisplayData.ranksHidden = !visible;

        if (rankToggleManager != null) {
            if (configuration.getRanks().isHiddenByDefault() == visible) {
                rankToggleManager.add(player);
            } else {
                rankToggleManager.remove(player);
            }
        }

        PlayerList playerList = HavocTab.getInstance().getFeatureManager().getFeature(TabConstants.Feature.PLAYER_LIST);
        if (playerList != null && configuration.getRanks().isHideTablist()) {
            playerList.refreshTablistForViewer(player);
        }
        NameTag nameTags = HavocTab.getInstance().getFeatureManager().getFeature(TabConstants.Feature.NAME_TAGS);
        if (nameTags != null && configuration.getRanks().isHideNameTags()) {
            nameTags.refreshPrefixSuffixForViewer(player);
        }

        player.expansionData.setRanksVisible(visible);
        updatePlaceholders(player);
        if (sendMessage) sendToggleMessage(player, visible, true);
    }

    /**
     * Changes belowname visibility for the given viewer and registers or unregisters
     * the objective on their client accordingly.
     *
     * @param   player
     *          Viewer to change the setting for
     * @param   visible
     *          {@code true} to show belowname, {@code false} to hide it
     * @param   sendMessage
     *          Whether the player should be told about the change
     */
    public void setBelowNameVisible(@NotNull TabPlayer player, boolean visible, boolean sendMessage) {
        if (player.clientDisplayData.belowNameHidden == !visible) {
            if (sendMessage) sendToggleMessage(player, visible, false);
            return;
        }
        player.clientDisplayData.belowNameHidden = !visible;

        if (belowNameToggleManager != null) {
            if (configuration.getBelowName().isHiddenByDefault() == visible) {
                belowNameToggleManager.add(player);
            } else {
                belowNameToggleManager.remove(player);
            }
        }

        BelowName belowName = HavocTab.getInstance().getFeatureManager().getFeature(TabConstants.Feature.BELOW_NAME);
        if (belowName != null) {
            belowName.setVisibleForViewer(player, visible);
        }

        player.expansionData.setBelowNameVisible(visible);
        updatePlaceholders(player);
        if (sendMessage) sendToggleMessage(player, visible, false);
    }

    /**
     * Refreshes the status placeholders of the given player so anything using them
     * (header/footer, scoreboard, tablist format, ...) updates immediately.
     *
     * @param   player
     *          Player to update placeholders for
     */
    private void updatePlaceholders(@NotNull TabPlayer player) {
        if (statusPlaceholders == null) return; // Not loaded yet
        for (PlayerPlaceholderImpl placeholder : statusPlaceholders) {
            placeholder.update(player);
        }
    }

    private void sendToggleMessage(@NotNull TabPlayer player, boolean visible, boolean ranks) {
        String message;
        if (ranks) {
            message = visible
                    ? HavocTab.getInstance().getConfiguration().getMessages().getRanksToggleOn()
                    : HavocTab.getInstance().getConfiguration().getMessages().getRanksToggleOff();
        } else {
            message = visible
                    ? HavocTab.getInstance().getConfiguration().getMessages().getBelowNameToggleOn()
                    : HavocTab.getInstance().getConfiguration().getMessages().getBelowNameToggleOff();
        }
        player.sendMessage(message);
    }

    /**
     * Returns the configured ON / OFF text for the given state.
     *
     * @param   visible
     *          Whether the feature is currently visible
     * @return  Configured text for the state
     */
    @NotNull
    private String formatStatus(boolean visible) {
        return visible ? configuration.getValueOn() : configuration.getValueOff();
    }

    @NotNull
    private static String stripSlash(@NotNull String command) {
        return command.startsWith("/") ? command.substring(1) : command;
    }

    @NotNull
    @Override
    public String getFeatureName() {
        return "ClientDisplaySettings";
    }
}
