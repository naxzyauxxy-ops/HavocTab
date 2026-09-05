package dev.havoc.havoctab.platforms.velocity;

import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import io.netty.channel.Channel;
import dev.havoc.havoctab.shared.features.injection.NettyPipelineInjector;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Pipeline injector for Velocity.
 */
public class VelocityPipelineInjector extends NettyPipelineInjector {

    /**
     * Constructs new instance.
     */
    public VelocityPipelineInjector() {
        super("handler");
    }

    @Override
    @NotNull
    protected Channel getChannel(@NotNull TabPlayer player) {
        return ((ConnectedPlayer) player.getPlayer()).getConnection().getChannel();
    }
}
