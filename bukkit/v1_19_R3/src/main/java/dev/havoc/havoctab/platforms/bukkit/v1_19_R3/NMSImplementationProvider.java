package dev.havoc.havoctab.platforms.bukkit.v1_19_R3;

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
import net.minecraft.network.NetworkManager;
import net.minecraft.server.network.PlayerConnection;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

/**
 * Implementation provider using direct NMS code for 1.19.4.
 */
@Getter
public class NMSImplementationProvider implements ImplementationProvider {

    /** Field is somehow private */
    private static final Field networkManager = ReflectionUtils.getOnlyField(PlayerConnection.class, NetworkManager.class);

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
        return ((NetworkManager)networkManager.get(((CraftPlayer)player).getHandle().b)).m;
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