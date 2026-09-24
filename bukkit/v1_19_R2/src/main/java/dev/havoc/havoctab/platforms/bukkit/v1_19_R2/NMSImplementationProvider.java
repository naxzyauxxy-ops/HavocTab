package dev.havoc.havoctab.platforms.bukkit.v1_19_R2;

import io.netty.channel.Channel;
import lombok.Getter;
import dev.havoc.havoctab.platforms.bukkit.BukkitTabPlayer;
import dev.havoc.havoctab.platforms.bukkit.provider.ComponentConverter;
import dev.havoc.havoctab.platforms.bukkit.provider.ImplementationProvider;
import dev.havoc.havoctab.shared.platform.Scoreboard;
import dev.havoc.havoctab.shared.platform.TabList;
import dev.havoc.havoctab.shared.platform.TabListEntryTracker;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation provider using direct NMS code for 1.19.3.
 */
@Getter
public class NMSImplementationProvider implements ImplementationProvider {

    @NotNull
    private final ComponentConverter<?> componentConverter = new NMSComponentConverter();
    
    @Override
    @NotNull
    public Scoreboard newScoreboard(@NotNull BukkitTabPlayer player) {
        return new NMSPacketScoreboard(player);
    }

    @Override
    @NotNull
    public TabList newTabList(@NotNull BukkitTabPlayer player) {
        return new NMSPacketTabList(player);
    }

    @Override
    @NotNull
    public Channel getChannel(@NotNull Player player) {
        return ((CraftPlayer)player).getHandle().b.b.m;
    }

    @Override
    @NotNull
    public TabListEntryTracker newTabListEntryTracker(@NotNull Player player) {
        return new NMSTabListEntryTracker(getChannel(player));
    }

    @Override
    public int getPing(@NotNull BukkitTabPlayer player) {
        return player.getPlayer().getPing();
    }
}