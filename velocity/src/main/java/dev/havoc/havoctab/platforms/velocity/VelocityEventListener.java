package dev.havoc.havoctab.platforms.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import dev.havoc.havoctab.shared.ProtocolVersion;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.data.Server;
import dev.havoc.havoctab.shared.platform.EventListener;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import dev.havoc.havoctab.shared.platform.decorators.SafeBossBar;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The core for Velocity forwarding events into all enabled features
 */
public class VelocityEventListener implements EventListener<Player> {

    /** Map for tracking online players */
    private final Map<Player, UUID> players = new ConcurrentHashMap<>();

    /**
     * Listens to player disconnecting from the server.
     *
     * @param   e
     *          Disconnect event
     */
    @Subscribe
    public void onQuit(@NotNull DisconnectEvent e) {
        if (HavocTab.getInstance().isPluginDisabled()) return;
        // Check if the player was actually connected to the server in the first place to avoid processing
        // disconnect of an existing player who is still there (because players are mapped by UUID in HavocTab)
        UUID id = players.remove(e.getPlayer());
        if (id != null) quit(id);
    }

    /**
     * Freezes Boss bar for 1.20.2+ players due to bug with adventure that causes disconnect
     * on 1.20.5+ with "Network Protocol Error"
     *
     * @param   e
     *          Event fired before player switches server for proper freezing
     */
    @Subscribe
    public void preConnect(@NotNull ServerPreConnectEvent e) {
        if (HavocTab.getInstance().isPluginDisabled()) return;
        if (e.getResult().isAllowed()) {
            TabPlayer p = HavocTab.getInstance().getPlayer(e.getPlayer().getUniqueId());
            if (p != null && p.getVersionId() >= ProtocolVersion.V1_20_2.getNetworkId()) {
                ((SafeBossBar<?>)p.getBossBar()).freeze();
            }
        }
    }

    /**
     * Listens to player connecting to a backend server. This handles
     * both initial connections and server switch.
     *
     * @param   e
     *          Server switch event
     */
    @Subscribe
    public void onConnect(@NotNull ServerPostConnectEvent e) {
        HavocTab tab = HavocTab.getInstance();
        if (tab.isPluginDisabled()) return;
        tab.getCPUManager().runTask(() -> {
            TabPlayer player = tab.getPlayer(e.getPlayer().getUniqueId());
            if (player == null) {
                players.put(e.getPlayer(), e.getPlayer().getUniqueId());
                tab.getFeatureManager().onJoin(createPlayer(e.getPlayer()));
            } else {
                if (!(player.getScoreboard() instanceof VelocityScoreboard)) player.getScoreboard().resend();
                tab.getFeatureManager().onServerChange(
                        player.getUniqueId(),
                        Server.byName(e.getPlayer().getCurrentServer().map(s -> s.getServerInfo().getName()).orElse("null"))
                );
                tab.getFeatureManager().onTabListClear(player);
                if (player.getVersionId() >= ProtocolVersion.V1_20_2.getNetworkId()) {
                    ((SafeBossBar<?>)player.getBossBar()).unfreezeAndSynchronize();
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
    @Subscribe
    public void onPluginMessageEvent(@NotNull PluginMessageEvent e) {
        if (!e.getIdentifier().getId().equals(TabConstants.PLUGIN_MESSAGE_CHANNEL_NAME)) return;
        e.setResult(PluginMessageEvent.ForwardResult.handled()); // Also cancel messages from players to prevent exploits
        if (HavocTab.getInstance().isPluginDisabled()) return;
        if (e.getTarget() instanceof Player) {
            pluginMessage(((Player) e.getTarget()).getUniqueId(), e.getData());
        }
    }

    @Override
    @NotNull
    public TabPlayer createPlayer(@NotNull Player player) {
        return new VelocityTabPlayer((VelocityPlatform) HavocTab.getInstance().getPlatform(), player);
    }
}