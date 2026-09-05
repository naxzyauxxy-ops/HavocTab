package dev.havoc.havoctab.shared.features.playerlistobjective;

import lombok.RequiredArgsConstructor;
import dev.havoc.havoctab.shared.chat.component.TabComponent;
import dev.havoc.havoctab.shared.cpu.ThreadExecutor;
import dev.havoc.havoctab.shared.features.types.CustomThreaded;
import dev.havoc.havoctab.shared.features.types.RefreshableFeature;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Feature for updating playerlist objective title.
 */
@RequiredArgsConstructor
public class PlayerListObjectiveTitleRefresher extends RefreshableFeature implements CustomThreaded {

    @NotNull
    private final YellowNumber feature;

    @NotNull
    @Override
    public String getFeatureName() {
        return feature.getFeatureName();
    }

    @NotNull
    @Override
    public String getRefreshDisplayName() {
        return "Updating Playerlist Objective title";
    }

    @Override
    public void refresh(@NotNull TabPlayer refreshed, boolean force) {
        if (refreshed.playerlistObjectiveData.disabled.get()) return;
        refreshed.getScoreboard().updateObjective(
                YellowNumber.OBJECTIVE_NAME,
                feature.getCache().get(refreshed.playerlistObjectiveData.title.updateAndGet()),
                feature.getConfiguration().getHealthDisplay(),
                TabComponent.empty()
        );
    }

    @Override
    @NotNull
    public ThreadExecutor getCustomThread() {
        return feature.getCustomThread();
    }
}
