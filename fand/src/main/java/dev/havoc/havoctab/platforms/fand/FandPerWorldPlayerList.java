package dev.havoc.havoctab.platforms.fand;

import io.fand.api.Fand;
import io.fand.api.entity.Player;
import io.fand.api.plugin.PluginContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.data.World;
import dev.havoc.havoctab.shared.features.PerWorldPlayerListConfiguration;
import dev.havoc.havoctab.shared.features.types.JoinListener;
import dev.havoc.havoctab.shared.features.types.Loadable;
import dev.havoc.havoctab.shared.features.types.QuitListener;
import dev.havoc.havoctab.shared.features.types.TabFeature;
import dev.havoc.havoctab.shared.features.types.UnLoadable;
import dev.havoc.havoctab.shared.features.types.VanishListener;
import dev.havoc.havoctab.shared.features.types.WorldSwitchListener;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;

/** Per-world player-list visibility backed by Fand's scoped tab-list service. */
public final class FandPerWorldPlayerList extends TabFeature implements
        JoinListener, QuitListener, Loadable, UnLoadable, VanishListener, WorldSwitchListener {

    private final PluginContext context;
    private final PerWorldPlayerListConfiguration configuration;
    private final Set<VisibilityPair> hiddenPairs = ConcurrentHashMap.newKeySet();

    public FandPerWorldPlayerList(
            @NotNull PluginContext context,
            @NotNull PerWorldPlayerListConfiguration configuration
    ) {
        this.context = context;
        this.configuration = configuration;
    }

    @Override
    public void load() {
        for (Player player : Fand.server().players()) {
            checkPlayer(player);
        }
    }

    @Override
    public void unload() {
        for (VisibilityPair pair : List.copyOf(hiddenPairs)) {
            show(pair);
        }
        hiddenPairs.clear();
    }

    @Override
    public void onJoin(@NotNull TabPlayer connectedPlayer) {
        checkPlayer(((FandTabPlayer) connectedPlayer).getPlayer());
    }

    @Override
    public void onQuit(@NotNull TabPlayer disconnectedPlayer) {
        UUID playerId = disconnectedPlayer.getUniqueId();
        Player disconnected = ((FandTabPlayer) disconnectedPlayer).getPlayer();
        for (VisibilityPair pair : List.copyOf(hiddenPairs)) {
            if (pair.viewerId().equals(playerId) || pair.targetId().equals(playerId)) {
                release(pair, disconnected);
            }
        }
    }

    @Override
    public void onWorldChange(@NotNull TabPlayer changed, @NotNull World from, @NotNull World to) {
        checkPlayer(((FandTabPlayer) changed).getPlayer());
    }

    @Override
    public void onVanishStatusChange(@NotNull TabPlayer player) {
        checkPlayer(((FandTabPlayer) player).getPlayer());
    }

    @Override
    @NotNull
    public String getFeatureName() {
        return "Per world PlayerList";
    }

    private void checkPlayer(Player changed) {
        for (Player other : Fand.server().players()) {
            if (other.uniqueId().equals(changed.uniqueId())) {
                continue;
            }
            updateVisibility(changed, other);
            updateVisibility(other, changed);
        }
    }

    private void updateVisibility(Player viewer, Player target) {
        var pair = new VisibilityPair(viewer.uniqueId(), target.uniqueId());
        if (shouldSee(viewer, target)) {
            show(pair);
        } else if (hiddenPairs.add(pair)) {
            context.tabLists().setVisible(viewer, target, false);
        }
    }

    private void show(VisibilityPair pair) {
        if (!hiddenPairs.remove(pair)) {
            return;
        }
        var viewer = Fand.server().player(pair.viewerId()).orElse(null);
        var target = Fand.server().player(pair.targetId()).orElse(null);
        if (viewer != null && target != null) {
            context.tabLists().setVisible(viewer, target, true);
        }
    }

    private void release(VisibilityPair pair, Player disconnected) {
        if (!hiddenPairs.remove(pair)) {
            return;
        }
        var viewer = player(pair.viewerId(), disconnected);
        var target = player(pair.targetId(), disconnected);
        if (viewer != null && target != null) {
            context.tabLists().setVisible(viewer, target, true);
        }
    }

    private static Player player(UUID playerId, Player disconnected) {
        if (playerId.equals(disconnected.uniqueId())) {
            return disconnected;
        }
        TabPlayer tabPlayer = HavocTab.getInstance().getPlayer(playerId);
        if (tabPlayer instanceof FandTabPlayer fandPlayer) {
            return fandPlayer.getPlayer();
        }
        return Fand.server().player(playerId).orElse(null);
    }

    private boolean shouldSee(Player viewer, Player target) {
        TabPlayer tabViewer = HavocTab.getInstance().getPlayer(viewer.uniqueId());
        TabPlayer tabTarget = HavocTab.getInstance().getPlayer(target.uniqueId());
        if (tabViewer != null && tabTarget != null && !tabViewer.canSee(tabTarget)) {
            return false;
        }
        if (configuration.isAllowBypassPermission()
                && viewer.can(TabConstants.Permission.PER_WORLD_PLAYERLIST_BYPASS)) {
            return true;
        }
        String viewerWorld = viewer.world().name();
        if (configuration.getIgnoredWorlds().contains(viewerWorld)) {
            return true;
        }
        return computeWorldGroup(viewerWorld).equals(computeWorldGroup(target.world().name()));
    }

    @NotNull
    private String computeWorldGroup(@NotNull String worldName) {
        for (Map.Entry<String, List<String>> group : configuration.getSharedWorlds().entrySet()) {
            List<String> worlds = group.getValue();
            if (worlds == null) {
                continue;
            }
            for (String world : worlds) {
                if (HavocTab.getInstance().getDataManager().matchesPattern(worldName, world)) {
                    return group.getKey();
                }
            }
        }
        return worldName + "-default";
    }

    private record VisibilityPair(UUID viewerId, UUID targetId) {
    }
}
