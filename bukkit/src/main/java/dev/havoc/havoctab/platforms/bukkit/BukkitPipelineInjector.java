package dev.havoc.havoctab.platforms.bukkit;

import io.netty.channel.Channel;
import dev.havoc.havoctab.platforms.bukkit.platform.BukkitPlatform;
import dev.havoc.havoctab.shared.features.injection.NettyPipelineInjector;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Pipeline injection for Bukkit 1.8+.
 */
public class BukkitPipelineInjector extends NettyPipelineInjector {

    /**
     * Constructs new instance
     */
    public BukkitPipelineInjector() {
        super("packet_handler");
    }

    @Override
    @NotNull
    protected Channel getChannel(@NotNull TabPlayer player) {
        return ((BukkitPlatform)player.getPlatform()).getServerVersionInfo().getImplementationProvider().getChannel(((BukkitTabPlayer) player).getPlayer());
    }
}
