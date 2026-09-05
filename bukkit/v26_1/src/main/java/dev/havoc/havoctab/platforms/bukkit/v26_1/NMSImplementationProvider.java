package dev.havoc.havoctab.platforms.bukkit.v26_1;

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.SneakyThrows;
import dev.havoc.havoctab.platforms.bukkit.BukkitTabPlayer;
import dev.havoc.havoctab.platforms.bukkit.provider.ComponentConverter;
import dev.havoc.havoctab.platforms.bukkit.provider.ImplementationProvider;
import dev.havoc.havoctab.shared.platform.Scoreboard;
import dev.havoc.havoctab.shared.platform.TabList;
import dev.havoc.havoctab.shared.platform.TabListEntryTracker;
import dev.havoc.havoctab.shared.util.ReflectionUtils;
import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

/**
 * Implementation provider using direct NMS code for 26.1.x.
 */
@Getter
public class NMSImplementationProvider implements ImplementationProvider {

    /** Field is somehow private */
    private static final Field networkManager = ReflectionUtils.getOnlyField(ServerCommonPacketListenerImpl.class, Connection.class);

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
    @SneakyThrows
    public Channel getChannel(@NotNull Player player) {
        return ((Connection)networkManager.get(((CraftPlayer)player).getHandle().connection)).channel;
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