package dev.havoc.havoctab.platforms.forge;

import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.platform.EventListener;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Event listener for Forge.
 */
public class ForgeEventListener implements EventListener<ServerPlayer> {

    /**
     * Registers all event listeners.
     */
    public void register() {
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(event -> {
            HavocTab.getInstance().addTablistTracker(
                    event.getEntity().getUUID(),
                    new ForgeTabListEntryTracker(((ServerPlayer) event.getEntity()).connection.getConnection().channel())
            );
            join((ServerPlayer) event.getEntity());
        });
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(event -> quit(event.getEntity().getUUID()));
        PlayerEvent.PlayerRespawnEvent.BUS.addListener(event -> {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            replacePlayer(player.getUUID(), player);
            worldChange(player.getUUID(), ForgeHavocTab.getLevelName(player.level()));
        });
        PlayerEvent.PlayerChangedDimensionEvent.BUS.addListener(event -> worldChange(event.getEntity().getUUID(), ForgeHavocTab.getLevelName(event.getEntity().level())));
    }

    @Override
    @NotNull
    public TabPlayer createPlayer(@NotNull ServerPlayer player) {
        return new ForgeTabPlayer((ForgePlatform) HavocTab.getInstance().getPlatform(), player);
    }
}
