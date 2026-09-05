package dev.havoc.havoctab.platforms.bungeecord.injection;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import dev.havoc.havoctab.platforms.bungeecord.BungeeTabPlayer;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.config.files.Config;
import dev.havoc.havoctab.shared.features.injection.NettyPipelineInjector;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import net.md_5.bungee.UserConnection;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Pipeline injection to secure proper functionality
 * of some features by preventing other plugins
 * from overriding it.
 */
public class BungeePipelineInjector extends NettyPipelineInjector {

    /** Whether ByteBuf deserialization should be enabled or not */
    private final boolean byteBufDeserialization;

    /**
     * Constructs new instance of the feature
     */
    public BungeePipelineInjector() {
        super("inbound-boss");
        Config config = HavocTab.getInstance().getConfiguration().getConfig();
        byteBufDeserialization = (config.getTeams() != null) || config.getScoreboard() != null;
    }

    @Override
    @NotNull
    public Function<TabPlayer, ChannelDuplexHandler> getChannelFunction() {
        return byteBufDeserialization ? DeserializableBungeeChannelDuplexHandler::new : BungeeChannelDuplexHandler::new;
    }

    @Override
    @NotNull
    protected Channel getChannel(@NotNull TabPlayer player) {
        return ((UserConnection) ((BungeeTabPlayer) player).getPlayer()).getCh().getHandle();
    }
}