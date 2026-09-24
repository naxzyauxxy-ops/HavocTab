package dev.havoc.havoctab.platforms.velocity;

import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.scoreboard.ObjectiveEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.scoreboard.ScoreboardManager;
import lombok.Getter;
import dev.havoc.havoctab.platforms.velocity.features.VelocityRedisSupport;
import dev.havoc.havoctab.platforms.velocity.features.VelocityTabExpansion;
import dev.havoc.havoctab.platforms.velocity.hook.MiniPlaceholdersHook;
import dev.havoc.havoctab.platforms.velocity.hook.VelocityPremiumVanishHook;
import dev.havoc.havoctab.shared.ProjectVariables;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.chat.TabTextColor;
import dev.havoc.havoctab.shared.chat.component.TabComponent;
import dev.havoc.havoctab.shared.chat.component.TabTextComponent;
import dev.havoc.havoctab.shared.features.injection.PipelineInjector;
import dev.havoc.havoctab.shared.features.proxy.ProxySupport;
import dev.havoc.havoctab.shared.placeholders.expansion.EmptyTabExpansion;
import dev.havoc.havoctab.shared.placeholders.expansion.TabExpansion;
import dev.havoc.havoctab.shared.platform.BossBar;
import dev.havoc.havoctab.shared.platform.Scoreboard;
import dev.havoc.havoctab.shared.platform.TabList;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import dev.havoc.havoctab.shared.platform.impl.AdventureBossBar;
import dev.havoc.havoctab.shared.platform.impl.DummyScoreboard;
import dev.havoc.havoctab.shared.proxy.ProxyPlatform;
import dev.havoc.havoctab.shared.util.ReflectionUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bstats.charts.SimplePie;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Velocity implementation of Platform
 */
@Getter
public class VelocityPlatform extends ProxyPlatform {

    @NotNull
    private final VelocityHavocTab plugin;

    /** Flag tracking presence of Velocity Scoreboard API */
    private boolean scoreboardAPI;

    /** Flag tracking presence of MiniPlaceholders */
    private final boolean miniPlaceholders;

    /** Plugin message channel */
    private final MinecraftChannelIdentifier MCI = MinecraftChannelIdentifier.from(TabConstants.PLUGIN_MESSAGE_CHANNEL_NAME);

    /** Logger for components */
    private final ComponentLogger logger = ComponentLogger.logger("HavocTab");

    /** List of custom commands registered to be able to unregister them on reload */
    private final List<String> customCommands = new ArrayList<>();

    /**
     * Constructs new instance with given plugin reference.
     *
     * @param   plugin
     *          Plugin instance
     */
    public VelocityPlatform(@NotNull VelocityHavocTab plugin) {
        this.plugin = plugin;
        this.miniPlaceholders = plugin.getServer().getPluginManager().isLoaded("miniplaceholders");
        loadVSAPIHook();
        if (plugin.getServer().getPluginManager().isLoaded("premiumvanish")) {
            new VelocityPremiumVanishHook().register();
        }
    }

    @Override
    public void registerUnknownPlaceholder(@NotNull String identifier) {
        if (miniPlaceholders && MiniPlaceholdersHook.isMiniPlaceholdersIdentifier(identifier)) {
            if (identifier.startsWith("<rel_")) {
                HavocTab.getInstance().getPlaceholderManager().registerRelationalPlaceholder(identifier, (viewer, target) ->
                        MiniPlaceholdersHook.parseRelational(identifier, ((VelocityTabPlayer) viewer).getPlayer(), ((VelocityTabPlayer) target).getPlayer()));
            } else if (identifier.startsWith("<server_")) {
                HavocTab.getInstance().getPlaceholderManager().registerServerPlaceholder(identifier,
                        () -> MiniPlaceholdersHook.parseGlobal(identifier));
            } else {
                HavocTab.getInstance().getPlaceholderManager().registerPlayerPlaceholder(identifier,
                        p -> MiniPlaceholdersHook.parsePlayer(identifier, ((VelocityTabPlayer) p).getPlayer()));
            }
            return;
        }
        super.registerUnknownPlaceholder(identifier);
    }

    @Override
    @NotNull
    public List<String> detectAdditionalPlaceholders(@NotNull String text) {
        if (!miniPlaceholders) return Collections.emptyList();
        return MiniPlaceholdersHook.detectPlaceholders(text);
    }

    @Override
    @NotNull
    public TabExpansion createTabExpansion() {
        if (miniPlaceholders) {
            return new VelocityTabExpansion();
        }
        return new EmptyTabExpansion();
    }

    private void loadVSAPIHook() {
        Optional<PluginContainer> vsapi = plugin.getServer().getPluginManager().getPlugin("velocity-scoreboard-api");
        if (vsapi.isEmpty()) {
            logWarn(new TabTextComponent("==============================================================================", TabTextColor.RED));
            logWarn(new TabTextComponent("Velocity does not have any sort of scoreboard API.", TabTextColor.RED));
            logWarn(new TabTextComponent("As a result, many features cannot be implemented using the standard Velocity API.", TabTextColor.RED));
            logWarn(new TabTextComponent("In order to enhance your experience, please consider installing VelocityScoreboardAPI " +
                    "(https://github.com/NEZNAMY/VelocityScoreboardAPI/releases/) plugin.", TabTextColor.RED));
            logWarn(new TabTextComponent("Until then, the following features will not work:", TabTextColor.RED));
            logWarn(new TabTextComponent("- scoreboard-teams", TabTextColor.RED));
            logWarn(new TabTextComponent("- belowname-objective", TabTextColor.RED));
            logWarn(new TabTextComponent("- playerlist-objective", TabTextColor.RED));
            logWarn(new TabTextComponent("- scoreboard", TabTextColor.RED));
            logWarn(new TabTextComponent("==============================================================================", TabTextColor.RED));
            return;
        }
        String vsapiVersion = vsapi.get().getDescription().getVersion().orElse("null");
        try {
            ScoreboardManager.getInstance();
            scoreboardAPI = true;
            if (vsapiVersion.startsWith("1.")) {
                logWarn(new TabTextComponent("Please update VelocityScoreboardAPI to version 2.0.0+ for optimal experience (" +
                        "current version: " + vsapiVersion + ").", TabTextColor.RED));
                return;
            }
        } catch (IllegalStateException ignored) {
            // Scoreboard API failed to enable due to an error
            return;
        }
        plugin.getServer().getEventManager().register(plugin, ObjectiveEvent.Display.class, e -> {
            HavocTab tab = HavocTab.getInstance();
            if (tab.isPluginDisabled()) return;
            tab.getCPUManager().runTask(() -> {
                TabPlayer player = tab.getPlayer(e.getPlayer().getUniqueId());
                if (player != null) tab.getFeatureManager().onDisplayObjective(player, e.getNewSlot().ordinal(), e.getObjectiveName());
            });
        });
        plugin.getServer().getEventManager().register(plugin, ObjectiveEvent.Unregister.class, e -> {
            HavocTab tab = HavocTab.getInstance();
            if (tab.isPluginDisabled()) return;
            tab.getCPUManager().runTask(() -> {
                TabPlayer player = tab.getPlayer(e.getPlayer().getUniqueId());
                if (player != null) tab.getFeatureManager().onObjective(player, Scoreboard.ObjectiveAction.UNREGISTER, e.getObjectiveName());
            });
        });
    }

    @Override
    public void loadPlayers() {
        for (Player p : plugin.getServer().getAllPlayers()) {
            HavocTab.getInstance().addPlayer(new VelocityTabPlayer(this, p));
        }
    }

    @Override
    @Nullable
    public ProxySupport getProxySupport(@NotNull String plugin, @NotNull String channelName) {
        if (plugin.equalsIgnoreCase("RedisBungee")) {
            if (ReflectionUtils.classExists("com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI") &&
                    RedisBungeeAPI.getRedisBungeeApi() != null) {
                return new VelocityRedisSupport(this.plugin, channelName);
            }
        }
        return null;
    }

    @Override
    public void logInfo(@NotNull TabComponent message) {
        logger.info(message.toAdventure());
    }

    @Override
    public void logWarn(@NotNull TabComponent message) {
        logger.warn(message.toAdventure());
    }

    @Override
    public void registerListener() {
        plugin.getServer().getEventManager().register(plugin, new VelocityEventListener());
    }

    @Override
    public void registerCommand() {
        CommandManager cmd = plugin.getServer().getCommandManager();
        cmd.register(cmd.metaBuilder(getCommand()).build(), new VelocityTabCommand());
    }

    @Override
    public void startMetrics() {
        plugin.getMetricsFactory().make(plugin, TabConstants.BSTATS_PLUGIN_ID_VELOCITY)
                .addCustomChart(new SimplePie(TabConstants.MetricsChart.GLOBAL_PLAYER_LIST_ENABLED,
                () -> HavocTab.getInstance().getFeatureManager().isFeatureEnabled(TabConstants.Feature.GLOBAL_PLAYER_LIST) ? "Yes" : "No"));
    }

    @Override
    @NotNull
    public File getDataFolder() {
        return plugin.getDataFolder().toFile();
    }

    @Override
    @NotNull
    public Component convertComponent(@NotNull TabComponent component) {
        return component.toAdventure();
    }

    @Override
    @NotNull
    public Scoreboard createScoreboard(@NotNull TabPlayer player) {
        if (scoreboardAPI) {
            return new VelocityScoreboard((VelocityTabPlayer) player);
        } else {
            return new DummyScoreboard(player);
        }
    }

    @Override
    @NotNull
    public BossBar createBossBar(@NotNull TabPlayer player) {
        return new AdventureBossBar(player);
    }

    @Override
    @NotNull
    public TabList createTabList(@NotNull TabPlayer player) {
        return new VelocityTabList((VelocityTabPlayer) player);
    }

    @Override
    public boolean supportsScoreboards() {
        return scoreboardAPI;
    }

    @Override
    @Nullable
    public PipelineInjector createPipelineInjector() {
        return new VelocityPipelineInjector();
    }

    @Override
    public void registerChannel() {
        plugin.getServer().getChannelRegistrar().register(MCI);
    }

    @Override
    @NotNull
    public String getCommand() {
        return "bhavoctab"; // Maybe change it to vtab one day?
    }

    @Override
    public void registerCustomCommand(@NotNull String commandName, @NotNull BiConsumer<TabPlayer, String[]> function) {
        CommandManager cmd = plugin.getServer().getCommandManager();
        CommandMeta meta = cmd.metaBuilder(commandName).build();
        customCommands.add(commandName);
        cmd.register(meta, (SimpleCommand) invocation -> {
            if (invocation.source() instanceof Player) {
                TabPlayer p = HavocTab.getInstance().getPlayer(((Player) invocation.source()).getUniqueId());
                if (p == null) return; //player not loaded correctly
                function.accept(p, invocation.arguments());
            } else {
                invocation.source().sendMessage(TabComponent.fromColoredText(
                        HavocTab.getInstance().getConfiguration().getMessages().getCommandOnlyFromGame()).toAdventure());
            }
        });
    }

    @Override
    public void unregisterAllCustomCommands() {
        for (String cmd : customCommands) {
            plugin.getServer().getCommandManager().unregister(cmd);
        }
        customCommands.clear();
    }

    @Override
    @NotNull
    public Object dump() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("server-type", "Velocity");
        map.put("server-name", plugin.getServer().getVersion().getName());
        map.put("server-version", plugin.getServer().getVersion().getVersion());
        map.put("tab-version", ProjectVariables.PLUGIN_VERSION);
        Optional<PluginContainer> vsapi = plugin.getServer().getPluginManager().getPlugin("velocity-scoreboard-api");
        String vsapiString;
        if (vsapi.isEmpty()) {
            vsapiString = "Not installed";
        } else if (!scoreboardAPI) {
            vsapiString = "Installed but failed to enable (version " + vsapi.get().getDescription().getVersion().orElse("null") + ")";
        } else {
            vsapiString = "Installed (version " + vsapi.get().getDescription().getVersion().orElse("null") + ")";
        }
        map.put("VelocityScoreboardAPI", vsapiString);
        Optional<PluginContainer> miniPlaceholdersPlugin = plugin.getServer().getPluginManager().getPlugin("miniplaceholders");
        map.put("MiniPlaceholders", miniPlaceholdersPlugin.isEmpty() ? "Not installed" :
                "Installed (version " + miniPlaceholdersPlugin.get().getDescription().getVersion().orElse("null") + ")");
        Map<String, Object> plugins = new LinkedHashMap<>();
        PluginContainer[] pluginArray = plugin.getServer().getPluginManager().getPlugins().toArray(new PluginContainer[0]);
        Arrays.sort(pluginArray, Comparator.comparing(p -> p.getDescription().getName().orElse("null"), String.CASE_INSENSITIVE_ORDER));
        for (PluginContainer p : pluginArray) {
            plugins.put(p.getDescription().getId(), p.getDescription().getVersion().orElse("null"));
        }
        map.put("plugins", plugins);
        return map;
    }
}
