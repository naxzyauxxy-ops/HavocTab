package dev.havoc.havoctab.shared.features.belowname;

import lombok.RequiredArgsConstructor;
import dev.havoc.havoctab.shared.cpu.ThreadExecutor;
import dev.havoc.havoctab.shared.features.types.CustomThreaded;
import dev.havoc.havoctab.shared.features.types.RefreshableFeature;
import dev.havoc.havoctab.shared.platform.Scoreboard;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Feature for updating belowname title.
 */
@RequiredArgsConstructor
public class BelowNameTitleRefresher extends RefreshableFeature implements CustomThreaded {

    @NotNull
    private final BelowName feature;

    @NotNull
    @Override
    public String getFeatureName() {
        return feature.getFeatureName();
    }

    @NotNull
    @Override
    public String getRefreshDisplayName() {
        return "Updating BelowName title";
    }

    @Override
    public void refresh(@NotNull TabPlayer refreshed, boolean force) {
        if (refreshed.belowNameData.disabled.get()) return;
        refreshed.getScoreboard().updateObjective(
                BelowName.OBJECTIVE_NAME,
                feature.getCache().get(refreshed.belowNameData.title.updateAndGet()),
                Scoreboard.HealthDisplay.INTEGER,
                feature.getCache().get(refreshed.belowNameData.defaultNumberFormat.updateAndGet())
        );
    }

    @Override
    @NotNull
    public ThreadExecutor getCustomThread() {
        return feature.getCustomThread();
    }
}
