package dev.havoc.havoctab.platforms.fabric;

import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.platform.EventListener;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Event listener for Fabric.
 */
public class FabricEventListener implements EventListener<ServerPlayer> {

    /**
     * Registers all event listeners.
     */
    public void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((connection, server) -> quit(connection.player.getUUID()));
        ServerPlayConnectionEvents.JOIN.register((connection, sender, server) -> {
            HavocTab.getInstance().addTablistTracker(
                    connection.player.getUUID(),
                    new FabricTabListEntryTracker(connection.player.connection.connection.channel)
            );
            join(connection.player);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            replacePlayer(newPlayer.getUUID(), newPlayer);
            // respawning from death & taking end portal in the end does not call world change event
            worldChange(newPlayer.getUUID(), FabricHavocTab.getLevelName(newPlayer.level()));
        });
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
                (player, origin, destination) -> worldChange(player.getUUID(), FabricHavocTab.getLevelName(destination)));
    }

    @Override
    @NotNull
    public TabPlayer createPlayer(@NotNull ServerPlayer player) {
        return new FabricTabPlayer((FabricPlatform) HavocTab.getInstance().getPlatform(), player);
    }
}
