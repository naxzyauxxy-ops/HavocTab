package dev.havoc.havoctab.shared.features.playerlist;

import lombok.Getter;
import lombok.NonNull;
import dev.havoc.havoctab.api.tablist.TabListFormatManager;
import dev.havoc.havoctab.shared.Property;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.TabConstants.CpuUsageCategory;
import dev.havoc.havoctab.shared.chat.component.TabComponent;
import dev.havoc.havoctab.shared.cpu.TimedCaughtTask;
import dev.havoc.havoctab.shared.data.Server;
import dev.havoc.havoctab.shared.data.World;
import dev.havoc.havoctab.shared.features.layout.impl.common.PlayerSlot;
import dev.havoc.havoctab.shared.features.proxy.ProxyPlayer;
import dev.havoc.havoctab.shared.features.proxy.ProxySupport;
import dev.havoc.havoctab.shared.features.types.*;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import dev.havoc.havoctab.shared.util.DumpUtils;
import dev.havoc.havoctab.shared.util.cache.StringToComponentCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Feature handler for TabList display names
 */
@Getter
public class PlayerList extends RefreshableFeature implements TabListFormatManager, JoinListener, Loadable,
        UnLoadable, WorldSwitchListener, ServerSwitchListener, VanishListener, ProxyFeature, GroupListener, Dumpable {

    @NotNull private final StringToComponentCache cache = new StringToComponentCache("Tablist name formatting", 1000);
    @NotNull private final TablistFormattingConfiguration configuration;
    @Nullable private final ProxySupport proxy = HavocTab.getInstance().getFeatureManager().getFeature(TabConstants.Feature.PROXY_SUPPORT);
    @NotNull private final DisableChecker disableChecker;

    /**
     * Constructs new instance, registers disable checker into feature manager and starts anti-override.
     *
     * @param   configuration
     *          Feature configuration
     */
    public PlayerList(@NotNull TablistFormattingConfiguration configuration) {
        this.configuration = configuration;
        disableChecker = new DisableChecker(this, HavocTab.getInstance().getPlaceholderManager().getConditionManager().getByNameOrExpression(configuration.getDisableCondition()), this::onDisableConditionChange, p -> p.tablistData.disabled);
        HavocTab.getInstance().getFeatureManager().registerFeature(TabConstants.Feature.PLAYER_LIST + "-Condition", disableChecker);
        if (proxy != null) {
            proxy.registerMessage(PlayerListProxyPlayerData.class, in -> new PlayerListProxyPlayerData(in, this));
        }
    }

    private void updateDisplayName(@NotNull TabPlayer viewer, @NotNull TabPlayer target, @Nullable TabComponent displayName) {
        if (viewer.layoutData.currentLayout != null) {
            PlayerSlot slot = viewer.layoutData.currentLayout.view.getSlot(target);
            if (slot != null) {
                viewer.getTabList().updateDisplayName(slot.getUniqueId(), displayName);
                return;
            }
        }
        viewer.getTabList().updateDisplayName(target, displayName);
    }

    /**
     * Loads properties from config.
     *
     * @param   player
     *          Player to load properties for
     */
    public void loadProperties(@NotNull TabPlayer player) {
        player.tablistData.prefix = player.loadPropertyFromConfig(this, "tabprefix", "");
        player.tablistData.name = player.loadPropertyFromConfig(this, "customtabname", player.getName());
        player.tablistData.suffix = player.loadPropertyFromConfig(this, "tabsuffix", "");
    }

    /**
     * Loads all properties from config and returns {@code true} if at least
     * one of them either wasn't loaded or changed value, {@code false} otherwise.
     *
     * @param   p
     *          Player to update properties of
     * @return  {@code true} if at least one property changed, {@code false} if not
     */
    public boolean updateProperties(@NotNull TabPlayer p) {
        boolean changed = p.updatePropertyFromConfig(p.tablistData.prefix, "");
        if (p.updatePropertyFromConfig(p.tablistData.name, p.getName())) changed = true;
        if (p.updatePropertyFromConfig(p.tablistData.suffix, "")) changed = true;
        return changed;
    }

    /**
     * Updates TabList format of requested player to everyone.
     *
     * @param   player
     *          Player to update
     * @param   format
     *          Whether player's actual format should be used or {@code null} for reset
     */
    public void formatPlayerForEveryone(@NotNull TabPlayer player, boolean format) {
        if (player.tablistData.disabled.get()) return;
        for (TabPlayer viewer : HavocTab.getInstance().getOnlinePlayers()) {
            // TODO This probably needs some layout check to make sure it does not use layout entry names for player names
            updateDisplayName(viewer, player, format ? getTabFormat(player, viewer) : null);
        }
        sendProxyMessage(player);
    }

    /**
     * Returns TabList format of player for viewer
     *
     * @param   p
     *          Player to get format of
     * @param   viewer
     *          Viewer seeing the format
     * @return  Format of specified player for viewer
     */
    @Nullable
    public TabComponent getTabFormat(@NotNull TabPlayer p, @NotNull TabPlayer viewer) {
        Property prefix = p.tablistData.prefix;
        Property name = p.tablistData.name;
        Property suffix = p.tablistData.suffix;
        if (prefix == null || name == null || suffix == null) {
            return null;
        }
        // HavocTab: viewer turned ranks off for themselves, show the plain name only.
        // This is purely per-viewer, other players still see the full format.
        if (viewer.clientDisplayData.hidesTablistRanks()) {
            return cache.get(name.getFormat(viewer));
        }
        return cache.get(prefix.getFormat(viewer) + name.getFormat(viewer) + suffix.getFormat(viewer));
    }

    /**
     * Resends TabList display names of all players to a single viewer. Used by HavocTab's
     * client-side rank toggle, where only one viewer's view needs to change.
     *
     * @param   viewer
     *          Player to resend all TabList formats to
     */
    public void refreshTablistForViewer(@NotNull TabPlayer viewer) {
        for (TabPlayer target : HavocTab.getInstance().getOnlinePlayers()) {
            if (target.tablistData.disabled.get()) continue;
            if (!viewer.server.canSee(target.server)) continue;
            updateDisplayName(viewer, target, getTabFormat(target, viewer));
        }
    }

    @Override
    public void load() {
        for (TabPlayer all : HavocTab.getInstance().getOnlinePlayers()) {
            loadProperties(all);
            if (disableChecker.isDisableConditionMet(all)) {
                all.tablistData.disabled.set(true);
            }
        }
        for (TabPlayer target : HavocTab.getInstance().getOnlinePlayers()) {
            formatPlayerForEveryone(target, true);
        }
    }

    @Override
    public void unload() {
        for (TabPlayer target : HavocTab.getInstance().getOnlinePlayers()) {
            formatPlayerForEveryone(target, false);
        }
    }

    @Override
    public void onServerChange(@NotNull TabPlayer p, @NotNull Server from, @NotNull Server to) {
        updateProperties(p);
        formatPlayerForEveryone(p, true); // Always update because this feature only affects the same server (group)
        if (HavocTab.getInstance().getFeatureManager().isFeatureEnabled(TabConstants.Feature.PIPELINE_INJECTION)) return;
        HavocTab.getInstance().getCpu().getProcessingThread().executeLater(new TimedCaughtTask(HavocTab.getInstance().getCpu(), () -> {
            for (TabPlayer all : HavocTab.getInstance().getOnlinePlayers()) {
                if (!all.tablistData.disabled.get() && p.server.canSee(all.server))
                    updateDisplayName(p, all, getTabFormat(all, p));
                if (all != p && !p.tablistData.disabled.get() && all.server.canSee(p.server))
                    updateDisplayName(all, p, getTabFormat(p, all));
            }
            if (proxy != null) {
                for (ProxyPlayer proxied : proxy.getProxyPlayers().values()) {
                    if (proxied.getTabFormat() == null) continue;
                    p.getTabList().updateDisplayName(proxied.getTablistId(), proxied.getTabFormat().getFormatComponent());
                }
            }
        }, getFeatureName(), CpuUsageCategory.SERVER_SWITCH), 300);
    }

    @Override
    public void onWorldChange(@NotNull TabPlayer changed, @NotNull World from, @NotNull World to) {
        if (updateProperties(changed)) formatPlayerForEveryone(changed, true);
    }

    /**
     * Processes disable condition change.
     *
     * @param   p
     *          Player who the condition has changed for
     * @param   disabledNow
     *          Whether the feature is disabled now or not
     */
    public void onDisableConditionChange(TabPlayer p, boolean disabledNow) {
        if (disabledNow) {
            for (TabPlayer viewer : HavocTab.getInstance().getOnlinePlayers()) {
                updateDisplayName(viewer, p, null);
            }
            sendProxyMessage(p);
        } else {
            formatPlayerForEveryone(p, true);
        }
        if (proxy != null) {
            sendProxyMessage(p);
        }
    }

    @NotNull
    @Override
    public String getRefreshDisplayName() {
        return "Updating TabList format";
    }

    @Override
    public void refresh(@NotNull TabPlayer refreshed, boolean force) {
        if (refreshed.tablistData.prefix == null) return; // Placeholder in condition on join
        boolean refresh;
        if (force) {
            updateProperties(refreshed);
            refresh = true;
        } else {
            boolean prefix = refreshed.tablistData.prefix.update();
            boolean name = refreshed.tablistData.name.update();
            boolean suffix = refreshed.tablistData.suffix.update();
            refresh = prefix || name || suffix;
        }
        if (refresh) {
            formatPlayerForEveryone(refreshed, true);
        }
    }

    @Override
    public void onGroupChange(@NotNull TabPlayer player) {
        if (updateProperties(player)) {
            formatPlayerForEveryone(player, true);
        }
    }

    @Override
    public void onJoin(@NotNull TabPlayer connectedPlayer) {
        loadProperties(connectedPlayer);
        if (disableChecker.isDisableConditionMet(connectedPlayer)) {
            connectedPlayer.tablistData.disabled.set(true);
        } else {
            formatPlayerForEveryone(connectedPlayer, true);
        }
        Runnable r = () -> {
            for (TabPlayer all : HavocTab.getInstance().getOnlinePlayers()) {
                if (all == connectedPlayer) continue; // Already updated above
                if (all.tablistData.disabled.get()) continue;
                updateDisplayName(connectedPlayer, all, getTabFormat(all, connectedPlayer));
            }
            if (proxy != null) {
                for (ProxyPlayer proxied : proxy.getProxyPlayers().values()) {
                    if (proxied.getTabFormat() == null) continue;
                    connectedPlayer.getTabList().updateDisplayName(proxied.getTablistId(), proxied.getTabFormat().getFormatComponent());
                }
            }
        };
        //add packet might be sent after tab's refresh packet, resending again when anti-override is disabled
        if (!HavocTab.getInstance().getFeatureManager().isFeatureEnabled(TabConstants.Feature.PIPELINE_INJECTION)) {
            HavocTab.getInstance().getCpu().getProcessingThread().executeLater(new TimedCaughtTask(HavocTab.getInstance().getCpu(),
                    r, getFeatureName(), CpuUsageCategory.PLAYER_JOIN), 300);
        } else {
            r.run();
        }
    }

    @Override
    public void onVanishStatusChange(@NotNull TabPlayer player) {
        if (player.isVanished()) return;
        formatPlayerForEveryone(player, true);
    }

    @Override
    @NotNull
    public Object dump(@NotNull TabPlayer player) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("configuration", configuration.getSection().getMap());
        map.put("disabled with condition", player.tablistData.disabled.get());
        for (Property property : Arrays.asList(player.tablistData.prefix, player.tablistData.name, player.tablistData.suffix)) {
            Map<String, Object> propertyMap = new LinkedHashMap<>();
            propertyMap.put("configured-raw-value", property.getOriginalRawValue());
            propertyMap.put("api-forced-raw-value", property.getTemporaryValue());
            propertyMap.put("current-source", property.getSource());
            propertyMap.put("replaced-value", property.get());
            map.put(property.getName(), propertyMap);
        }
        List<String> header = Arrays.asList("Player", "tabprefix", "(custom)tabname", "tabsuffix", "Disabled with condition");
        List<List<String>> players = Arrays.stream(HavocTab.getInstance().getOnlinePlayers()).map(p -> Arrays.asList(
                p.getName(),
                "\"" + p.tablistData.prefix.get() + "\"",
                "\"" + p.tablistData.name.get() + "\"",
                "\"" + p.tablistData.suffix.get() + "\"",
                String.valueOf(p.tablistData.disabled.get())
        )).collect(Collectors.toList());
        if (proxy != null) {
            for (ProxyPlayer proxied : proxy.getProxyPlayers().values()) {
                players.add(Arrays.asList(
                        "[Proxy] " + proxied.getName(),
                        proxied.getTabFormat() == null ? "NULL" : "\"" + proxied.getTabFormat().getPrefix() + "\"",
                        proxied.getTabFormat() == null ? "NULL" : "\"" + proxied.getTabFormat().getName() + "\"",
                        proxied.getTabFormat() == null ? "NULL" : "\"" + proxied.getTabFormat().getSuffix() + "\"",
                        proxied.getTabFormat() == null ? "NULL" : String.valueOf(proxied.getTabFormat().isDisabled())
                ));
            }
        }
        map.put("current values for all players (without applying relational placeholders)", DumpUtils.tableToLines(header, players));
        return map;
    }

    // ------------------
    // API Implementation
    // ------------------
    
    @Override
    public void setPrefix(@NonNull dev.havoc.havoctab.api.TabPlayer player, @Nullable String prefix) {
        ensureActive();
        ((TabPlayer)player).ensureLoaded();
        ((TabPlayer)player).tablistData.prefix.setTemporaryValue(prefix);
        formatPlayerForEveryone(((TabPlayer)player), true);
    }

    @Override
    public void setName(@NonNull dev.havoc.havoctab.api.TabPlayer player, @Nullable String customName) {
        ensureActive();
        ((TabPlayer)player).ensureLoaded();
        ((TabPlayer)player).tablistData.name.setTemporaryValue(customName);
        formatPlayerForEveryone(((TabPlayer)player), true);
    }

    @Override
    public void setSuffix(@NonNull dev.havoc.havoctab.api.TabPlayer player, @Nullable String suffix) {
        ensureActive();
        ((TabPlayer)player).ensureLoaded();
        ((TabPlayer)player).tablistData.suffix.setTemporaryValue(suffix);
        formatPlayerForEveryone(((TabPlayer)player), true);
    }

    @Override
    public String getCustomPrefix(@NonNull dev.havoc.havoctab.api.TabPlayer player) {
        ensureActive();
        ((TabPlayer)player).ensureLoaded();
        return ((TabPlayer)player).tablistData.prefix.getTemporaryValue();
    }

    @Override
    public String getCustomName(@NonNull dev.havoc.havoctab.api.TabPlayer player) {
        ensureActive();
        ((TabPlayer)player).ensureLoaded();
        return ((TabPlayer)player).tablistData.name.getTemporaryValue();
    }

    @Override
    public String getCustomSuffix(@NonNull dev.havoc.havoctab.api.TabPlayer player) {
        ensureActive();
        ((TabPlayer)player).ensureLoaded();
        return ((TabPlayer)player).tablistData.suffix.getTemporaryValue();
    }

    @Override
    @NotNull
    public String getOriginalPrefix(@NonNull dev.havoc.havoctab.api.TabPlayer player) {
        return getOriginalRawPrefix(player);
    }

    @Override
    @NotNull
    public String getOriginalName(@NonNull dev.havoc.havoctab.api.TabPlayer player) {
        return getOriginalRawName(player);
    }

    @Override
    @NotNull
    public String getOriginalSuffix(@NonNull dev.havoc.havoctab.api.TabPlayer player) {
        return getOriginalRawSuffix(player);
    }

    @Override
    @NotNull
    public String getOriginalRawPrefix(@NonNull dev.havoc.havoctab.api.TabPlayer player) {
        ensureActive();
        ((TabPlayer)player).ensureLoaded();
        return ((TabPlayer)player).tablistData.prefix.getOriginalRawValue();
    }

    @Override
    @NotNull
    public String getOriginalRawName(@NonNull dev.havoc.havoctab.api.TabPlayer player) {
        ensureActive();
        ((TabPlayer)player).ensureLoaded();
        return ((TabPlayer)player).tablistData.name.getOriginalRawValue();
    }

    @Override
    @NotNull
    public String getOriginalRawSuffix(@NonNull dev.havoc.havoctab.api.TabPlayer player) {
        ensureActive();
        ((TabPlayer)player).ensureLoaded();
        return ((TabPlayer)player).tablistData.suffix.getOriginalRawValue();
    }

    @Override
    @NotNull
    public String getOriginalReplacedPrefix(@NonNull dev.havoc.havoctab.api.TabPlayer player) {
        ensureActive();
        ((TabPlayer)player).ensureLoaded();
        return ((TabPlayer)player).tablistData.prefix.getOriginalReplacedValue();
    }

    @Override
    @NotNull
    public String getOriginalReplacedName(@NonNull dev.havoc.havoctab.api.TabPlayer player) {
        ensureActive();
        ((TabPlayer)player).ensureLoaded();
        return ((TabPlayer)player).tablistData.name.getOriginalReplacedValue();
    }

    @Override
    @NotNull
    public String getOriginalReplacedSuffix(@NonNull dev.havoc.havoctab.api.TabPlayer player) {
        ensureActive();
        ((TabPlayer)player).ensureLoaded();
        return ((TabPlayer)player).tablistData.suffix.getOriginalReplacedValue();
    }

    // ------------------
    // ProxySupport
    // ------------------

    private void sendProxyMessage(@NotNull TabPlayer player) {
        if (proxy != null) {
            proxy.sendMessage(new PlayerListProxyPlayerData(
                    this,
                    proxy.getIdCounter().incrementAndGet(),
                    player.getUniqueId(),
                    player.getName(),
                    player.tablistData.prefix.get(),
                    player.tablistData.name.get(),
                    player.tablistData.suffix.get(),
                    TabComponent.empty(), // This instance is for writing, parsed is not needed on this side
                    player.tablistData.disabled.get()
            ));
        }
    }

    @Override
    public void onProxyLoadRequest() {
        for (TabPlayer all : HavocTab.getInstance().getOnlinePlayers()) {
            sendProxyMessage(all);
        }
    }

    @Override
    public void onVanishStatusChange(@NotNull ProxyPlayer player) {
        formatPlayerForEveryone(player);
    }

    @Override
    public void onJoin(@NotNull ProxyPlayer player) {
        formatPlayerForEveryone(player);
    }

    /**
     * Updates TabList format of requested player to everyone.
     *
     * @param   player
     *          Player to update
     */
    public void formatPlayerForEveryone(@NotNull ProxyPlayer player) {
        if (player.isVanished()) return;
        if (player.getTabFormat() == null) return; // Player not loaded yet
        for (TabPlayer viewer : HavocTab.getInstance().getOnlinePlayers()) {
            viewer.getTabList().updateDisplayName(player.getTablistId(), player.getTabFormat().getFormatComponent());
        }
    }

    @NotNull
    @Override
    public String getFeatureName() {
        return "Tablist name formatting";
    }
}