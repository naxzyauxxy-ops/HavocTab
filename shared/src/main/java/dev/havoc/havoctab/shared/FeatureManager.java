package dev.havoc.havoctab.shared;

import com.saicone.delivery4j.broker.RabbitMQBroker;
import com.saicone.delivery4j.broker.RedisBroker;
import dev.havoc.havoctab.api.placeholder.PlayerPlaceholder;
import dev.havoc.havoctab.shared.TabConstants.CpuUsageCategory;
import dev.havoc.havoctab.shared.config.files.Config;
import dev.havoc.havoctab.shared.config.mysql.MySQLUserConfiguration;
import dev.havoc.havoctab.shared.cpu.TimedCaughtTask;
import dev.havoc.havoctab.shared.data.Server;
import dev.havoc.havoctab.shared.data.World;
import dev.havoc.havoctab.shared.features.NickCompatibility;
import dev.havoc.havoctab.shared.features.SpectatorFix;
import dev.havoc.havoctab.shared.features.belowname.BelowName;
import dev.havoc.havoctab.shared.features.bossbar.BossBarManagerImpl;
import dev.havoc.havoctab.shared.features.chat.ChatManager;
import dev.havoc.havoctab.shared.features.clientdisplay.ClientDisplayManager;
import dev.havoc.havoctab.shared.features.globalplayerlist.GlobalPlayerList;
import dev.havoc.havoctab.shared.features.header.HeaderFooter;
import dev.havoc.havoctab.shared.features.injection.PipelineInjector;
import dev.havoc.havoctab.shared.features.layout.LayoutManagerImpl;
import dev.havoc.havoctab.shared.features.nametags.NameTag;
import dev.havoc.havoctab.shared.features.pingspoof.PingSpoof;
import dev.havoc.havoctab.shared.features.playerlist.PlayerList;
import dev.havoc.havoctab.shared.features.playerlistobjective.YellowNumber;
import dev.havoc.havoctab.shared.features.proxy.ProxyMessengerSupport;
import dev.havoc.havoctab.shared.features.proxy.ProxyPlayer;
import dev.havoc.havoctab.shared.features.proxy.ProxySupport;
import dev.havoc.havoctab.shared.features.proxy.ProxySupportConfiguration;
import dev.havoc.havoctab.shared.features.scoreboard.ScoreboardManagerImpl;
import dev.havoc.havoctab.shared.features.sorting.Sorting;
import dev.havoc.havoctab.shared.features.types.*;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import dev.havoc.havoctab.shared.platform.decorators.TrackedTabList;
import dev.havoc.havoctab.shared.proxy.ProxyPlatform;
import dev.havoc.havoctab.shared.proxy.ProxyTabPlayer;
import dev.havoc.havoctab.shared.proxy.message.outgoing.Unload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Feature registration which offers calls to all features
 * and measures how long it took them to process
 */
public class FeatureManager {

    /** Map of all registered feature where key is feature's identifier */
    private final Map<String, TabFeature> features = new LinkedHashMap<>();

    /** All registered features in an array to avoid memory allocations on iteration */
    @NotNull
    private TabFeature[] values = new TabFeature[0];

    /**
     * Calls load() on all features.
     * This function is called on plugin startup.
     */
    public void load() {
        for (TabFeature f : values) {
            if (!(f instanceof Loadable)) continue;
            long time = System.currentTimeMillis();
            ((Loadable) f).load();
            HavocTab.getInstance().debug("Loaded feature " + f.getClass().getSimpleName() + " in " + (System.currentTimeMillis()-time) + "ms");
        }
        if (HavocTab.getInstance().getConfiguration().getUsers() instanceof MySQLUserConfiguration) {
            MySQLUserConfiguration users = (MySQLUserConfiguration) HavocTab.getInstance().getConfiguration().getUsers();
            for (TabPlayer p : HavocTab.getInstance().getOnlinePlayers()) users.load(p);
        }
    }

    /**
     * Calls unload() on all features.
     * This function is called on plugin disable.
     */
    public void unload() {
        // Shut down and then unload sync to prevent load running before old unload ends on reloads
        for (TabFeature f : values) {
            if (f instanceof CustomThreaded) {
                long time = System.currentTimeMillis();
                ((CustomThreaded) f).getCustomThread().shutdown();
                HavocTab.getInstance().debug("Shut down custom thread of " + f.getClass().getSimpleName() + " in " + (System.currentTimeMillis()-time) + "ms");
            }
            if (f instanceof UnLoadable) {
                long time = System.currentTimeMillis();
                ((UnLoadable) f).unload();
                HavocTab.getInstance().debug("Unloaded feature " + f.getClass().getSimpleName() + " in " + (System.currentTimeMillis()-time) + "ms");
            }
        }
        for (TabFeature f : values) {
            f.deactivate();
        }
        long time = System.currentTimeMillis();
        for (TabPlayer player : HavocTab.getInstance().getOnlinePlayers()) {
            player.getScoreboard().clear();
            player.getBossBar().clear();
        }
        HavocTab.getInstance().debug("Unregistered all scoreboard teams, objectives and boss bars for all players in " + (System.currentTimeMillis()-time) + "ms");
        HavocTab.getInstance().getPlaceholderManager().getTabExpansion().unregisterExpansion();
        if (HavocTab.getInstance().getPlatform() instanceof ProxyPlatform) {
            for (TabPlayer player : HavocTab.getInstance().getOnlinePlayers()) {
                ((ProxyTabPlayer)player).sendPluginMessage(new Unload());
            }
        }
        HavocTab.getInstance().getPlatform().unregisterAllCustomCommands();
    }

    /**
     * Calls onGroupChange to all features implementing {@link GroupListener}.
     *
     * @param   player
     *          player with new group
     */
    public void onGroupChange(@NotNull TabPlayer player) {
        for (TabFeature f : values) {
            if (!(f instanceof GroupListener)) continue;
            TimedCaughtTask task = new TimedCaughtTask(HavocTab.getInstance().getCpu(),
                    () -> ((GroupListener) f).onGroupChange(player), f.getFeatureName(), CpuUsageCategory.GROUP_CHANGE);
            if (f instanceof CustomThreaded) {
                ((CustomThreaded) f).getCustomThread().execute(task);
            } else {
                task.run();
            }
        }
    }

    /**
     * Forwards gamemode change to all enabled features.
     *
     * @param   player
     *          Player whose gamemode has changed.
     */
    public void onGameModeChange(@NotNull TabPlayer player) {
        for (TabFeature f : values) {
            if (!(f instanceof GameModeListener)) continue;
            TimedCaughtTask task = new TimedCaughtTask(HavocTab.getInstance().getCpu(),
                    () -> ((GameModeListener) f).onGameModeChange(player), f.getFeatureName(), CpuUsageCategory.GAMEMODE_CHANGE);
            if (f instanceof CustomThreaded) {
                ((CustomThreaded) f).getCustomThread().execute(task);
            } else {
                task.run();
            }
        }
    }

    /**
     * Forwards player quit to all features
     *
     * @param   disconnectedPlayer
     *          Player who left
     */
    public void onQuit(@Nullable TabPlayer disconnectedPlayer) {
        if (disconnectedPlayer == null) return;
        disconnectedPlayer.markOffline();
        long millis = System.currentTimeMillis();
        for (TabFeature f : values) {
            if (!(f instanceof QuitListener)) continue;
            TimedCaughtTask task = new TimedCaughtTask(HavocTab.getInstance().getCpu(),
                    () -> ((QuitListener) f).onQuit(disconnectedPlayer), f.getFeatureName(), CpuUsageCategory.PLAYER_QUIT);
            if (f instanceof CustomThreaded) {
                ((CustomThreaded) f).getCustomThread().execute(task);
            } else {
                task.run();
            }
        }
        HavocTab.getInstance().removePlayer(disconnectedPlayer);
        removeforcedDisplayName(disconnectedPlayer.getTablistId());
        HavocTab.getInstance().debug("Player quit of " + disconnectedPlayer.getName() + " processed in " + (System.currentTimeMillis()-millis) + "ms");

        ProxySupport proxy = getFeature(TabConstants.Feature.PROXY_SUPPORT);
        if (proxy != null) {
            ProxyPlayer proxyPlayer = proxy.getProxyPlayers().get(disconnectedPlayer.getUniqueId());
            if (proxyPlayer != null && proxyPlayer.getConnectionState() == ProxyPlayer.ConnectionState.QUEUED) {
                onJoin(proxyPlayer);
            }
        }
    }

    private void removeforcedDisplayName(@NotNull UUID id) {
        ProxySupport proxy = getFeature(TabConstants.Feature.PROXY_SUPPORT);
        if (proxy != null) {
            ProxyPlayer proxyPlayer = proxy.getProxyPlayers().get(id);
            if (proxyPlayer != null) return; // Proxy player is already connected, do not remove anymore
        }

        if (HavocTab.getInstance().getPlayer(id) != null) return; // Real player connected in the meantime, do not remove anymore

        // Player is actually not online anymore, remove to avoid memory leak
        for (TabPlayer all : HavocTab.getInstance().getOnlinePlayers()) {
            ((TrackedTabList<?>)all.getTabList()).getForcedDisplayNames().remove(id);
            ((TrackedTabList<?>)all.getTabList()).getBlockedSpectators().remove(id);
        }
    }

    /**
     * Handles player join and forwards it to all features.
     *
     * @param   connectedPlayer
     *          Player who joined
     */
    public void onJoin(@NotNull TabPlayer connectedPlayer) {
        long millis = System.currentTimeMillis();
        HavocTab.getInstance().addPlayer(connectedPlayer);
        for (TabFeature f : values) {
            if (!(f instanceof JoinListener)) continue;
            TimedCaughtTask task = new TimedCaughtTask(HavocTab.getInstance().getCpu(), () -> ((JoinListener) f).onJoin(connectedPlayer), f.getFeatureName(), CpuUsageCategory.PLAYER_JOIN);
            if (f instanceof CustomThreaded) {
                ((CustomThreaded) f).getCustomThread().execute(task);
            } else {
                task.run();
            }
        }
        connectedPlayer.markAsLoaded(true);
        HavocTab.getInstance().debug("Player join of " + connectedPlayer.getName() + " processed in " + (System.currentTimeMillis()-millis) + "ms");
        if (HavocTab.getInstance().getConfiguration().getUsers() instanceof MySQLUserConfiguration) {
            MySQLUserConfiguration users = (MySQLUserConfiguration) HavocTab.getInstance().getConfiguration().getUsers();
            users.load(connectedPlayer);
        }
    }

    /**
     * Processed world change and forwards it to all features.
     *
     * @param   playerUUID
     *          UUID of player who switched worlds
     * @param   to
     *          New world
     */
    public void onWorldChange(@NotNull UUID playerUUID, @NotNull World to) {
        TabPlayer changed = HavocTab.getInstance().getPlayer(playerUUID);
        if (changed == null) return;
        World from = changed.world;
        changed.world = to;
        for (TabFeature f : values) {
            if (!(f instanceof WorldSwitchListener)) continue;
            TimedCaughtTask task = new TimedCaughtTask(HavocTab.getInstance().getCpu(),
                    () -> ((WorldSwitchListener) f).onWorldChange(changed, from, to), f.getFeatureName(), CpuUsageCategory.WORLD_SWITCH);
            if (f instanceof CustomThreaded) {
                ((CustomThreaded) f).getCustomThread().execute(task);
            } else {
                task.run();
            }
        }
        ((PlayerPlaceholder)HavocTab.getInstance().getPlaceholderManager().getPlaceholder(TabConstants.Placeholder.WORLD)).updateValue(changed, to.getName());
    }

    /**
     * Processed server switch and forwards it to all features.
     *
     * @param   playerUUID
     *          UUID of player who switched server
     * @param   to
     *          New server name
     */
    public void onServerChange(@NotNull UUID playerUUID, @NotNull Server to) {
        TabPlayer changed = HavocTab.getInstance().getPlayer(playerUUID);
        if (changed == null) return;
        Server from = changed.server;
        changed.server = to;
        ((ProxyTabPlayer)changed).sendJoinPluginMessage();
        ((TrackedTabList<?>)changed.getTabList()).resendHeaderFooter();
        for (TabFeature f : values) {
            if (!(f instanceof ServerSwitchListener)) continue;
            TimedCaughtTask task = new TimedCaughtTask(HavocTab.getInstance().getCpu(),
                    () -> ((ServerSwitchListener) f).onServerChange(changed, from, to), f.getFeatureName(), CpuUsageCategory.SERVER_SWITCH);
            if (f instanceof CustomThreaded) {
                ((CustomThreaded) f).getCustomThread().execute(task);
            } else {
                task.run();
            }
        }
        ((PlayerPlaceholder)HavocTab.getInstance().getPlaceholderManager().getPlaceholder(TabConstants.Placeholder.SERVER)).updateValue(changed, to.getName());
    }

    /**
     * Calls onDisplayObjective(...) on all features
     *
     * @param   packetReceiver
     *          player who received the packet
     * @param   slot
     *          Objective slot
     * @param   objective
     *          Objective name
     */
    public void onDisplayObjective(@NotNull TabPlayer packetReceiver, int slot, @Nullable String objective) {
        for (TabFeature f : values) {
            if (!(f instanceof DisplayObjectiveListener)) continue;
            TimedCaughtTask task = new TimedCaughtTask(HavocTab.getInstance().getCpu(),
                    () -> ((DisplayObjectiveListener) f).onDisplayObjective(packetReceiver, slot, objective), f.getFeatureName(), CpuUsageCategory.SCOREBOARD_PACKET_CHECK);
            if (f instanceof CustomThreaded) {
                ((CustomThreaded) f).getCustomThread().execute(task);
            } else {
                task.run();
            }
        }
    }

    /**
     * Calls onObjective(TabPlayer, PacketPlayOutScoreboardObjective) on all features
     *
     * @param   packetReceiver
     *          player who received the packet
     * @param   action
     *          Packet action
     * @param   objective
     *          Objective name
     */
    public void onObjective(@NotNull TabPlayer packetReceiver, int action, @NotNull String objective) {
        for (TabFeature f : values) {
            if (!(f instanceof ObjectiveListener)) continue;
            TimedCaughtTask task = new TimedCaughtTask(HavocTab.getInstance().getCpu(),
                    () -> ((ObjectiveListener) f).onObjective(packetReceiver, action, objective), f.getFeatureName(), CpuUsageCategory.SCOREBOARD_PACKET_CHECK);
            if (f instanceof CustomThreaded) {
                ((CustomThreaded) f).getCustomThread().execute(task);
            } else {
                task.run();
            }
        }
    }

    /**
     * Forwards vanish status change to all features.
     *
     * @param   player
     *          Player whose vanish status changed
     */
    public void onVanishStatusChange(@NotNull TabPlayer player) {
        for (TabFeature f : values) {
            if (!(f instanceof VanishListener)) continue;
            TimedCaughtTask task = new TimedCaughtTask(HavocTab.getInstance().getCpu(),
                    () -> ((VanishListener) f).onVanishStatusChange(player), f.getFeatureName(), CpuUsageCategory.VANISH_CHANGE);
            if (f instanceof CustomThreaded) {
                ((CustomThreaded) f).getCustomThread().execute(task);
            } else {
                task.run();
            }
        }
    }

    /**
     * Forwards disguise status changes to all enabled features.
     *
     * @param player player whose disguise status changed
     */
    public void onDisguiseStatusChange(@NotNull TabPlayer player) {
        for (TabFeature f : values) {
            if (!(f instanceof DisguiseListener)) continue;
            TimedCaughtTask task = new TimedCaughtTask(HavocTab.getInstance().getCpu(),
                    () -> ((DisguiseListener) f).onDisguiseStatusChange(player),
                    f.getFeatureName(), CpuUsageCategory.DISGUISE_CHANGE);
            if (f instanceof CustomThreaded) {
                ((CustomThreaded) f).getCustomThread().execute(task);
            } else {
                task.run();
            }
        }
    }

    /**
     * Forwards entry add to all features.
     *
     * @param   packetReceiver
     *          Player who received the packet
     * @param   id
     *          UUID of the entry
     * @param   name
     *          Player name of the entry
     */
    public void onEntryAdd(TabPlayer packetReceiver, UUID id, String name) {
        for (TabFeature f : values) {
            if (!(f instanceof EntryAddListener)) continue;
            long time = System.nanoTime();
            ((EntryAddListener)f).onEntryAdd(packetReceiver, id, name);
            HavocTab.getInstance().getCPUManager().addTime(f.getFeatureName(), CpuUsageCategory.NICK_PLUGIN_COMPATIBILITY, System.nanoTime() - time);
        }
    }

    /**
     * Forwards tablist clear to all enabled features.
     *
     * @param   packetReceiver
     *          Player whose tablist got cleared
     */
    public void onTabListClear(TabPlayer packetReceiver) {
        for (TabFeature f : values) {
            if (!(f instanceof TabListClearListener)) continue;
            TimedCaughtTask task = new TimedCaughtTask(HavocTab.getInstance().getCpu(),
                    () -> ((TabListClearListener) f).onTabListClear(packetReceiver), f.getFeatureName(), CpuUsageCategory.TABLIST_CLEAR);
            if (f instanceof CustomThreaded) {
                ((CustomThreaded) f).getCustomThread().execute(task);
            } else {
                task.run();
            }
        }
    }

    /**
     * Called when another proxy is reloaded to request all data again.
     */
    public void onProxyLoadRequest() {
        for (TabFeature f : values) {
            if (!(f instanceof ProxyFeature)) continue;
            TimedCaughtTask task = new TimedCaughtTask(HavocTab.getInstance().getCpu(),
                    () -> ((ProxyFeature) f).onProxyLoadRequest(), f.getFeatureName(), CpuUsageCategory.PROXY_RELOAD);
            if (f instanceof CustomThreaded) {
                ((CustomThreaded) f).getCustomThread().execute(task);
            } else {
                task.run();
            }
        }
    }

    /**
     * Handles proxy player join and forwards it to all features.
     *
     * @param   connectedPlayer
     *          Player who joined
     */
    public void onJoin(@NotNull ProxyPlayer connectedPlayer) {
        // Delay to let server remove the player from tablist, and then we can potentially add the player back
        // No delay could cause the server to send tablist remove packet of real player AFTER we sent add, removing the player
        HavocTab.getInstance().getCpu().getProcessingThread().executeLater(new TimedCaughtTask(HavocTab.getInstance().getCpu(), () -> {
            if (connectedPlayer.getConnectionState() == ProxyPlayer.ConnectionState.DISCONNECTED) return; // Player immediately disconnected in the meantime
            connectedPlayer.setConnectionState(ProxyPlayer.ConnectionState.CONNECTED);
            for (TabFeature f : values) {
                if (!(f instanceof ProxyFeature)) continue;
                TimedCaughtTask task = new TimedCaughtTask(HavocTab.getInstance().getCpu(),
                        () -> ((ProxyFeature) f).onJoin(connectedPlayer), f.getFeatureName(), CpuUsageCategory.PLAYER_JOIN);
                if (f instanceof CustomThreaded) {
                    ((CustomThreaded) f).getCustomThread().execute(task);
                } else {
                    task.run();
                }
            }
            if (connectedPlayer.isVanished()) {
                onVanishStatusChange(connectedPlayer);
            }
        }, getFeature(TabConstants.Feature.PROXY_SUPPORT).getFeatureName(), CpuUsageCategory.PLAYER_JOIN), 200);
    }

    /**
     * Handles proxy player server switch and forwards it to all features.
     *
     * @param   player
     *          Player who joined
     */
    public void onServerSwitch(@NotNull ProxyPlayer player) {
        for (TabFeature f : values) {
            if (!(f instanceof ProxyFeature)) continue;
            TimedCaughtTask task = new TimedCaughtTask(HavocTab.getInstance().getCpu(),
                    () -> ((ProxyFeature) f).onServerSwitch(player), f.getFeatureName(), CpuUsageCategory.SERVER_SWITCH);
            if (f instanceof CustomThreaded) {
                ((CustomThreaded) f).getCustomThread().execute(task);
            } else {
                task.run();
            }
        }
    }

    /**
     * Handles proxy player quit and forwards it to all features.
     *
     * @param   disconnectedPlayer
     *          Player who left
     */
    public void onQuit(@NotNull ProxyPlayer disconnectedPlayer) {
        disconnectedPlayer.setConnectionState(ProxyPlayer.ConnectionState.DISCONNECTED);
        for (TabFeature f : values) {
            if (!(f instanceof ProxyFeature)) continue;
            TimedCaughtTask task = new TimedCaughtTask(HavocTab.getInstance().getCpu(),
                    () -> ((ProxyFeature) f).onQuit(disconnectedPlayer), f.getFeatureName(), CpuUsageCategory.PLAYER_QUIT);
            if (f instanceof CustomThreaded) {
                ((CustomThreaded) f).getCustomThread().execute(task);
            } else {
                task.run();
            }
        }
        removeforcedDisplayName(disconnectedPlayer.getTablistId());
    }

    /**
     * Forwards vanish status change to all features.
     *
     * @param   player
     *          Player whose vanish status changed
     */
    public void onVanishStatusChange(@NotNull ProxyPlayer player) {
        for (TabFeature f : values) {
            if (!(f instanceof ProxyFeature)) continue;
            TimedCaughtTask task = new TimedCaughtTask(HavocTab.getInstance().getCpu(),
                    () -> ((ProxyFeature) f).onVanishStatusChange(player), f.getFeatureName(), CpuUsageCategory.VANISH_CHANGE);
            if (f instanceof CustomThreaded) {
                ((CustomThreaded) f).getCustomThread().execute(task);
            } else {
                task.run();
            }
        }
    }

    /**
     * Registers feature with given parameters.
     *
     * @param   featureName
     *          Name of feature to register as
     * @param   featureHandler
     *          Feature handler
     */
    public synchronized void registerFeature(@NotNull String featureName, @NotNull TabFeature featureHandler) {
        features.put(featureName, featureHandler);
        values = features.values().toArray(new TabFeature[0]);
        if (featureHandler instanceof VanishListener) {
            HavocTab.getInstance().getPlaceholderManager().addUsedPlaceholder(TabConstants.Placeholder.VANISHED);
        }
        if (featureHandler instanceof GameModeListener) {
            HavocTab.getInstance().getPlaceholderManager().addUsedPlaceholder(TabConstants.Placeholder.GAMEMODE);
        }
    }

    /**
     * Unregisters feature with given name.
     *
     * @param   featureName
     *          Name of the feature it was previously registered with.
     */
    public void unregisterFeature(@NotNull String featureName) {
        features.remove(featureName);
        values = features.values().toArray(new TabFeature[0]);
    }

    /**
     * Returns {@code true} if feature is enabled, {@code false} if not.
     *
     * @param   name
     *          Name of the feature
     * @return  {@code true} if enabled, {@code false} if not
     */
    public boolean isFeatureEnabled(@NotNull String name) {
        return features.containsKey(name);
    }

    /**
     * Returns feature by given name.
     *
     * @param   name
     *          Name of the feature
     * @return  Feature handler
     * @param   <T>
     *          class extending TabFeature
     */
    @SuppressWarnings("unchecked")
    public <T extends TabFeature> T getFeature(@NotNull String name) {
        return (T) features.get(name);
    }

    /**
     * Loads features from config
     */
    public void loadFeaturesFromConfig() {
        Config config = HavocTab.getInstance().getConfiguration().getConfig();

        // HavocTab: registered first so client-side display preferences are loaded
        // before any feature that reads them processes the player.
        registerFeature(TabConstants.Feature.CLIENT_DISPLAY, new ClientDisplayManager(config.getClientDisplay()));
        if (config.getChat() != null) {
            registerFeature(TabConstants.Feature.CHAT, new ChatManager(config.getChat()));
        }

        // Load the feature first, because it will be processed in main thread (to make it run before feature threads)
        if (config.getProxySupport() != null) {
            ProxySupportConfiguration configuration = config.getProxySupport();
            ProxySupport proxy = null;
            if (configuration.getType().equalsIgnoreCase("PLUGIN")) {
                proxy = HavocTab.getInstance().getPlatform().getProxySupport(configuration.getPluginName(), configuration.getChannelName());
            } else if (configuration.getType().equalsIgnoreCase("REDIS")) {
                proxy = new ProxyMessengerSupport("Redis", configuration.getChannelName(), () -> RedisBroker.of(configuration.getRedisUrl()));
            } else if (configuration.getType().equalsIgnoreCase("RABBITMQ")) {
                proxy = new ProxyMessengerSupport("RabbitMQ", configuration.getChannelName(),
                        () -> RabbitMQBroker.of(configuration.getRabbitmqUrl(), configuration.getRabbitmqExchange()));
            }
            if (proxy != null) HavocTab.getInstance().getFeatureManager().registerFeature(TabConstants.Feature.PROXY_SUPPORT, proxy);
        }

        if (config.isPipelineInjection()) {
            PipelineInjector inj = HavocTab.getInstance().getPlatform().createPipelineInjector();
            if (inj != null) registerFeature(TabConstants.Feature.PIPELINE_INJECTION, inj);
        }

        if (config.getPerWorldPlayerList() != null) {
            TabFeature pwp = HavocTab.getInstance().getPlatform().getPerWorldPlayerList(config.getPerWorldPlayerList());
            if (pwp != null) registerFeature(TabConstants.Feature.PER_WORLD_PLAYER_LIST, pwp);
        }
        if (config.getBossbar() != null) {
            registerFeature(TabConstants.Feature.BOSS_BAR, new BossBarManagerImpl(config.getBossbar()));
        }
        if (config.getPingSpoof() != null) {
            registerFeature(TabConstants.Feature.PING_SPOOF, new PingSpoof(config.getPingSpoof()));
        }
        if (config.getHeaderFooter() != null) {
            registerFeature(TabConstants.Feature.HEADER_FOOTER, new HeaderFooter(config.getHeaderFooter()));
        }
        if (config.isPreventSpectatorEffect()) {
            registerFeature(TabConstants.Feature.SPECTATOR_FIX, new SpectatorFix());
        }
        if (config.getScoreboard() != null) {
            registerFeature(TabConstants.Feature.SCOREBOARD, new ScoreboardManagerImpl(config.getScoreboard()));
        }
        if (config.getPlayerlistObjective() != null) {
            registerFeature(TabConstants.Feature.YELLOW_NUMBER, new YellowNumber(config.getPlayerlistObjective()));
        }
        if (config.getBelowname() != null) {
            registerFeature(TabConstants.Feature.BELOW_NAME, new BelowName(config.getBelowname()));
        }
        if (config.getSorting() != null) {
            registerFeature(TabConstants.Feature.SORTING, new Sorting(config.getSorting()));
        }
        if (config.getTablistFormatting() != null) {
            registerFeature(TabConstants.Feature.PLAYER_LIST, new PlayerList(config.getTablistFormatting()));
        }

        // Must be loaded after: Sorting
        if (config.getTeams() != null) {
            registerFeature(TabConstants.Feature.NAME_TAGS, new NameTag(config.getTeams()));
        }

        // Must be loaded after: Sorting, PlayerList
        if (config.getLayout() != null) {
            registerFeature(TabConstants.Feature.LAYOUT, new LayoutManagerImpl(config.getLayout()));
        }

        // Must be loaded after: PlayerList
        HavocTab.getInstance().getDataManager().applyConfiguration(config.getGlobalPlayerList());
        if (config.getGlobalPlayerList() != null) {
            registerFeature(TabConstants.Feature.GLOBAL_PLAYER_LIST, new GlobalPlayerList(config.getGlobalPlayerList()));
        }

        registerFeature(TabConstants.Feature.NICK_COMPATIBILITY, new NickCompatibility());
    }
}
