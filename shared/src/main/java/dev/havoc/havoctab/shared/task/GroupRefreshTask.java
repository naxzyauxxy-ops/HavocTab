package dev.havoc.havoctab.shared.task;

import lombok.RequiredArgsConstructor;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.cpu.CpuManager;
import dev.havoc.havoctab.shared.cpu.TimedCaughtTask;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Task for refreshing permission groups of players.
 */
@RequiredArgsConstructor
public class GroupRefreshTask implements Runnable {

    /** Function for getting group of a player */
    @NotNull
    private final Function<TabPlayer, String> detectGroup;

    @Override
    public void run() {
        for (TabPlayer all : HavocTab.getInstance().getOnlinePlayers()) {
            String oldGroup = all.getPermissionGroup();
            String newGroup = detectGroup.apply(all);
            if (!oldGroup.equals(newGroup)) {
                // Back to main thread to avoid concurrency issues
                CpuManager cpu = HavocTab.getInstance().getCpu();
                cpu.getProcessingThread().execute(new TimedCaughtTask(cpu, () -> all.setGroup(newGroup), "Permission group refreshing", "Applying changes"));
            }
        }
    }
}
