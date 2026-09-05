package dev.havoc.havoctab.platforms.neoforge;

import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.platform.EventListener;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Event listener for NeoForge.
 */
public class NeoForgeEventListener implements EventListener<ServerPlayer> {

    /**
     * Registers all event listeners.
     */
    public void register() {
        IEventBus eventBus = NeoForge.EVENT_BUS;
        eventBus.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            HavocTab.getInstance().addTablistTracker(
                    event.getEntity().getUUID(),
                    new NeoForgeTabListEntryTracker(((ServerPlayer) event.getEntity()).connection.getConnection().channel())
            );
            join((ServerPlayer) event.getEntity());
        });
        eventBus.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> quit(event.getEntity().getUUID()));
        eventBus.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            replacePlayer(player.getUUID(), player);
            worldChange(player.getUUID(), NeoForgeHavocTab.getLevelName(player.level()));
        });
        eventBus.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> worldChange(event.getEntity().getUUID(), NeoForgeHavocTab.getLevelName(event.getEntity().level())));
    }

    @Override
    @NotNull
    public TabPlayer createPlayer(@NotNull ServerPlayer player) {
        return new NeoForgeTabPlayer((NeoForgePlatform) HavocTab.getInstance().getPlatform(), player);
    }
}
