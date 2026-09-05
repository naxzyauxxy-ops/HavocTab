package dev.havoc.havoctab.platforms.forge;

import io.netty.channel.Channel;
import dev.havoc.havoctab.shared.features.injection.NettyPipelineInjector;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Pipeline injector for Forge.
 */
public class ForgePipelineInjector extends NettyPipelineInjector {

    /**
     * Constructs new instance.
     */
    public ForgePipelineInjector() {
        super("packet_handler");
    }

    @Override
    @NotNull
    protected Channel getChannel(@NotNull TabPlayer player) {
        return ((ForgeTabPlayer)player).getPlayer().connection.getConnection().channel();
    }
}
