package dev.havoc.havoctab.platforms.fand;

import io.fand.api.entity.Player;
import io.fand.api.event.EventPriority;
import io.fand.api.event.player.PlayerChangedWorldEvent;
import io.fand.api.event.player.PlayerDisguiseStateChangeEvent;
import io.fand.api.event.player.PlayerGameModeChangeEvent;
import io.fand.api.event.player.PlayerJoinEvent;
import io.fand.api.event.player.PlayerQuitEvent;
import io.fand.api.event.player.PlayerRespawnEvent;
import io.fand.api.event.player.PlayerVanishStateChangeEvent;
import io.fand.api.plugin.PluginContext;
import java.util.UUID;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.platform.EventListener;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Bridges Fand player lifecycle events into HavocTab. */
public final class FandEventListener implements EventListener<Player> {

    private final PluginContext context;

    public FandEventListener(@NotNull PluginContext context) {
        this.context = context;
    }

    public void register() {
        context.events().subscribe(PlayerJoinEvent.class, event -> join(event.player()));
        context.events().subscribe(PlayerQuitEvent.class, event -> quit(event.player().uniqueId()));
        context.events().subscribe(PlayerChangedWorldEvent.class,
                event -> worldChange(event.player().uniqueId(), event.toWorld().name()));
        context.events().subscribe(PlayerRespawnEvent.class, EventPriority.OBSERVER, event -> {
            replacePlayer(event.player().uniqueId(), event.player());
            worldChange(event.player().uniqueId(), event.respawnLocation().world().name());
        });
        context.events().subscribe(PlayerGameModeChangeEvent.class,
                event -> gameModeChange(event.player().uniqueId()));
        context.events().subscribe(PlayerVanishStateChangeEvent.class,
                this::vanishStateChange);
        context.events().subscribe(PlayerDisguiseStateChangeEvent.class,
                event -> disguiseStateChange(event.player().uniqueId()));
    }

    @Override
    @NotNull
    public TabPlayer createPlayer(@NotNull Player player) {
        return new FandTabPlayer((FandPlatform) HavocTab.getInstance().getPlatform(), player);
    }

    private void gameModeChange(UUID playerId) {
        context.scheduler().runMain(() -> {
            FandTabPlayer player = player(playerId);
            if (player != null) {
                HavocTab.getInstance().getFeatureManager().onGameModeChange(player);
            }
        });
    }

    private void vanishStateChange(PlayerVanishStateChangeEvent event) {
        FandTabPlayer player = player(event.player().uniqueId());
        if (player == null) {
            return;
        }
        player.updateVanishState(event.vanished());
        HavocTab.getInstance().getFeatureManager().onVanishStatusChange(player);
    }

    private void disguiseStateChange(UUID playerId) {
        FandTabPlayer player = player(playerId);
        if (player != null) {
            HavocTab.getInstance().getFeatureManager().onDisguiseStatusChange(player);
        }
    }

    @Nullable
    private static FandTabPlayer player(UUID playerId) {
        HavocTab tab = HavocTab.getInstance();
        if (tab == null || tab.isPluginDisabled()) {
            return null;
        }
        TabPlayer player = tab.getPlayer(playerId);
        return player instanceof FandTabPlayer fandPlayer ? fandPlayer : null;
    }
}
