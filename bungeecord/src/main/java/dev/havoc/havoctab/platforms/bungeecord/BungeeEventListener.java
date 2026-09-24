package dev.havoc.havoctab.platforms.bungeecord;

import dev.havoc.havoctab.shared.ProtocolVersion;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.data.Server;
import dev.havoc.havoctab.shared.platform.EventListener;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import dev.havoc.havoctab.shared.platform.decorators.SafeBossBar;
import dev.havoc.havoctab.shared.platform.decorators.SafeScoreboard;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.NotNull;

/**
 * The core for BungeeCord forwarding events into all enabled features
 */
public class BungeeEventListener implements EventListener<ProxiedPlayer>, Listener {

    /**
     * Listens to player disconnecting from the server.
     *
     * @param   e
     *          Disconnect event
     */
    @EventHandler
    public void onQuit(PlayerDisconnectEvent e) {
        quit(e.getPlayer().getUniqueId());
    }

    /**
     * Listens to player connecting to a backend server. This handles
     * both initial connections and server switch.
     *
     * @param   e
     *          Server switch event
     */
    @EventHandler
    public void onSwitch(ServerSwitchEvent e) {
        HavocTab tab = HavocTab.getInstance();
        if (tab.isPluginDisabled()) return;

        // Avoid 1.20.3+ client crash on scoreboard packets, do it sync to prevent packet being sent after event, but before processing
        // Avoid 1.20.5+ client disconnect with "Network Protocol Error"
        TabPlayer p = tab.getPlayer(e.getPlayer().getUniqueId());
        if (p != null && p.getVersionId() >= ProtocolVersion.V1_20_2.getNetworkId()) {
            ((SafeScoreboard<?>)p.getScoreboard()).setFrozen(true);
            ((SafeBossBar<?>)p.getBossBar()).freeze();
        }

        tab.getCPUManager().runTask(() -> {
            TabPlayer player = tab.getPlayer(e.getPlayer().getUniqueId());
            if (player == null) {
                player = createPlayer(e.getPlayer());

                // Things will get cleared immediately after, so no point in sending it, also someone said it fixed some warn from Geyser
                // Sending these packets before login packet will also crash the client on 1.20.3
                if (player.getVersionId() >= ProtocolVersion.V1_20_2.getNetworkId()) {
                    ((SafeScoreboard<?>)player.getScoreboard()).setFrozen(true);
                    ((SafeBossBar<?>)player.getBossBar()).freeze();
                }
                tab.getFeatureManager().onJoin(player);
            } else {
                tab.getFeatureManager().onServerChange(player.getUniqueId(), Server.byName(e.getPlayer().getServer().getInfo().getName()));
                if (player.getVersionId() < ProtocolVersion.V1_20_2.getNetworkId()) {
                    // For versions below 1.20.2 the tablist is already clean when this event is called
                    // For 1.20.2+ this event is called before, so we listen to Login packet instead
                    tab.getFeatureManager().onTabListClear(player);
                }
            }
        });
    }

    /**
     * Listens to plugin messages.
     *
     * @param   e
     *          Plugin message event
     */
    @EventHandler
    public void onPluginMessage(PluginMessageEvent e) {
        if (!e.getTag().equals(TabConstants.PLUGIN_MESSAGE_CHANNEL_NAME)) return;
        e.setCancelled(true); // Also cancel messages from players to prevent exploits
        if (HavocTab.getInstance().isPluginDisabled()) return;
        if (e.getReceiver() instanceof ProxiedPlayer) {
            pluginMessage(((ProxiedPlayer) e.getReceiver()).getUniqueId(), e.getData());
        }
    }

    @Override
    @NotNull
    public TabPlayer createPlayer(@NotNull ProxiedPlayer player) {
        return new BungeeTabPlayer((BungeePlatform) HavocTab.getInstance().getPlatform(), player);
    }
}