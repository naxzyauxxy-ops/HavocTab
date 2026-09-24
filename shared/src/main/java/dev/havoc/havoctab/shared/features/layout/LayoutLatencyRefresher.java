package dev.havoc.havoctab.shared.features.layout;

import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.features.layout.impl.common.PlayerSlot;
import dev.havoc.havoctab.shared.features.types.RefreshableFeature;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Layout sub-feature updating latency of layout entries to match player latencies.
 */
public class LayoutLatencyRefresher extends RefreshableFeature {

    /**
     * Constructs new instance.
     */
    public LayoutLatencyRefresher() {
        addUsedPlaceholder(TabConstants.Placeholder.PING);
    }

    @NotNull
    @Override
    public String getFeatureName() {
        return "Layout";
    }

    @NotNull
    @Override
    public String getRefreshDisplayName() {
        return "Updating latency";
    }

    @Override
    public void refresh(@NotNull TabPlayer p, boolean force) {
        int ping = p.getPing();
        for (TabPlayer all : HavocTab.getInstance().getOnlinePlayers()) {
            if (all.layoutData.currentLayout == null) continue;
            PlayerSlot slot = all.layoutData.currentLayout.view.getSlot(p);
            if (slot == null) continue;
            all.getTabList().updateLatency(slot.getUniqueId(), ping);
        }
    }
}
