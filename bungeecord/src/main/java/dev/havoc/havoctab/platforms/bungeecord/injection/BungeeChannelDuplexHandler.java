package dev.havoc.havoctab.platforms.bungeecord.injection;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import dev.havoc.havoctab.shared.ProtocolVersion;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.cpu.CpuManager;
import dev.havoc.havoctab.shared.cpu.TimedCaughtTask;
import dev.havoc.havoctab.shared.features.injection.NettyPipelineInjector.TabChannelDuplexHandler;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import dev.havoc.havoctab.shared.platform.decorators.SafeBossBar;
import dev.havoc.havoctab.shared.platform.decorators.SafeScoreboard;
import net.md_5.bungee.protocol.packet.Login;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Channel duplex handler for BungeeCord, which in addition checks for Login packet.
 */
public class BungeeChannelDuplexHandler extends TabChannelDuplexHandler {

    /**
     * Constructs new instance with given player
     *
     * @param   player
     *          player to inject
     */
    public BungeeChannelDuplexHandler(TabPlayer player) {
        super(player);
    }

    @Override
    public void write(@NotNull ChannelHandlerContext context, @Nullable Object packet, @NotNull ChannelPromise channelPromise) {
        if (packet instanceof Login) {
            ((SafeScoreboard<?>)player.getScoreboard()).setFrozen(true);
            CpuManager cpu = HavocTab.getInstance().getCpu();
            cpu.getProcessingThread().executeLater(new TimedCaughtTask(cpu, () -> {
                ((SafeScoreboard<?>)player.getScoreboard()).setFrozen(false);
                player.getScoreboard().resend();
                if (player.getVersionId() >= ProtocolVersion.V1_20_2.getNetworkId()) {
                    // For 1.20.2+ we need to do this, because server switch event is called before tablist is cleared
                    HavocTab.getInstance().getFeatureManager().onTabListClear(player);
                    ((SafeBossBar<?>)player.getBossBar()).unfreezeAndResend();
                }
            }, "Pipeline injection", TabConstants.CpuUsageCategory.PACKET_LOGIN), 200);
        }
        super.write(context, packet, channelPromise);
    }
}