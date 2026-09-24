package dev.havoc.havoctab.platforms.neoforge;

import io.netty.channel.Channel;
import dev.havoc.havoctab.shared.features.injection.NettyPipelineInjector;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Pipeline injector for NeoForge.
 */
public class NeoForgePipelineInjector extends NettyPipelineInjector {

    /**
     * Constructs new instance.
     */
    public NeoForgePipelineInjector() {
        super("packet_handler");
    }

    @Override
    @NotNull
    protected Channel getChannel(@NotNull TabPlayer player) {
        return ((NeoForgeTabPlayer)player).getPlayer().connection.getConnection().channel();
    }
}
