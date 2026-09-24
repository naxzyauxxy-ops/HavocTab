package dev.havoc.havoctab.shared.features.pingspoof;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.cpu.ThreadExecutor;
import dev.havoc.havoctab.shared.features.types.*;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import dev.havoc.havoctab.shared.platform.decorators.TrackedTabList;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This feature hides real ping of players in connection bar and
 * replaces it with a custom fake value.
 */
@Getter
@RequiredArgsConstructor
public class PingSpoof extends TabFeature implements JoinListener, Loadable, UnLoadable, CustomThreaded, Dumpable {

    @Getter
    private final ThreadExecutor customThread = new ThreadExecutor("HavocTab Ping Spoof Thread");

    /** Value to display as ping instead of real ping */
    private final PingSpoofConfiguration configuration;

    @Override
    public void load() {
        TrackedTabList.setForcedLatency(configuration.getValue());
        updateAll(false);
    }

    @Override
    public void unload() {
        TrackedTabList.setForcedLatency(null);
        updateAll(true);
    }

    @Override
    public void onJoin(@NotNull TabPlayer connectedPlayer) {
        for (TabPlayer all : HavocTab.getInstance().getOnlinePlayers()) {
            connectedPlayer.getTabList().updateLatency(all, configuration.getValue());
            all.getTabList().updateLatency(connectedPlayer, configuration.getValue());
        }
    }

    private void updateAll(boolean realPing) {
        for (TabPlayer viewer : HavocTab.getInstance().getOnlinePlayers()) {
            for (TabPlayer target : HavocTab.getInstance().getOnlinePlayers()) {
                viewer.getTabList().updateLatency(target, realPing ? target.getPing() : configuration.getValue());
            }
        }
    }

    @NotNull
    @Override
    public String getFeatureName() {
        return "Ping spoof";
    }

    @Override
    @NotNull
    public Object dump(@NotNull TabPlayer player) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("configuration", configuration.getSection().getMap());
        return map;
    }
}
