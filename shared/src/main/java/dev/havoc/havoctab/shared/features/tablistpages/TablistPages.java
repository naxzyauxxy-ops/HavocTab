package dev.havoc.havoctab.shared.features.tablistpages;

import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.ProtocolVersion;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.cpu.ThreadExecutor;
import dev.havoc.havoctab.shared.cpu.TimedCaughtTask;
import dev.havoc.havoctab.shared.features.PlaceholderManagerImpl;
import dev.havoc.havoctab.shared.features.types.CustomThreaded;
import dev.havoc.havoctab.shared.features.types.JoinListener;
import dev.havoc.havoctab.shared.features.types.Loadable;
import dev.havoc.havoctab.shared.features.types.QuitListener;
import dev.havoc.havoctab.shared.features.types.TabFeature;
import dev.havoc.havoctab.shared.features.types.UnLoadable;
import dev.havoc.havoctab.shared.placeholders.types.PlayerPlaceholderImpl;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import dev.havoc.havoctab.shared.util.PerformanceUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Cycles the tablist through pages when more players are online than the client can draw.
 * <p>
 * The Minecraft Java client renders a maximum of 80 tablist entries. Past that the server
 * can keep sending entries but the client simply will not draw them, so on a busy server
 * some players are invisible in the tab list no matter what the server does.
 * <p>
 * This feature works around that by showing a rotating window: everyone is split into
 * pages of at most {@code page-size}, and every {@code cycle-interval} the visible page
 * advances. Over one full rotation every online player has been shown.
 * <p>
 * Hiding is done with the tablist "listed" flag (1.19.3+) rather than by removing entries.
 * The entry stays on the client, which means skins stay cached, nametags and scoreboard
 * teams are untouched, and flipping a page is a single tiny packet per player. On older
 * clients, which have no listed flag, the feature stays off rather than removing and
 * re-adding entries several times a minute.
 */
@Getter
public class TablistPages extends TabFeature implements Loadable, UnLoadable, JoinListener, QuitListener, CustomThreaded {

    /** Feature configuration */
    @NotNull private final TablistPagesConfiguration configuration;

    /** Own thread, so page flips never touch the main thread */
    @NotNull private final ThreadExecutor customThread = new ThreadExecutor("HavocTab Tablist Pages Thread");

    /** Currently displayed page, shared by every viewer so the tab list looks the same to all */
    private volatile int currentPage;

    /** Total pages as of the last flip, used by the placeholders */
    private volatile int totalPages = 1;

    /** Status placeholders */
    @Nullable private PlayerPlaceholderImpl[] placeholders;

    /** Players currently hidden, tracked so unload can restore them */
    @NotNull private final List<TabPlayer> hidden = new ArrayList<>();

    /**
     * Constructs new instance with given configuration.
     *
     * @param   configuration
     *          Feature configuration
     */
    public TablistPages(@NotNull TablistPagesConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void load() {
        PlaceholderManagerImpl manager = HavocTab.getInstance().getPlaceholderManager();
        placeholders = new PlayerPlaceholderImpl[]{
                manager.registerPlayerPlaceholder(TabConstants.Placeholder.TABLIST_PAGE, -1,
                        p -> PerformanceUtil.toString(currentPage + 1)),
                manager.registerPlayerPlaceholder(TabConstants.Placeholder.TABLIST_PAGES, -1,
                        p -> PerformanceUtil.toString(totalPages))
        };

        customThread.repeatTask(new TimedCaughtTask(HavocTab.getInstance().getCpu(),
                this::flipPage, getFeatureName(), "Cycling tablist page"), configuration.getCycleInterval());
    }

    @Override
    public void unload() {
        // Put everyone back in the tab list before letting go
        customThread.execute(new TimedCaughtTask(HavocTab.getInstance().getCpu(), () -> {
            for (TabPlayer viewer : HavocTab.getInstance().getOnlinePlayers()) {
                if (!supports(viewer)) continue;
                for (TabPlayer target : HavocTab.getInstance().getOnlinePlayers()) {
                    viewer.getTabList().updateListed(target, true);
                }
            }
            hidden.clear();
        }, getFeatureName(), "Restoring tablist"));
    }

    @Override
    public void onJoin(@NotNull TabPlayer connectedPlayer) {
        customThread.execute(new TimedCaughtTask(HavocTab.getInstance().getCpu(),
                this::apply, getFeatureName(), TabConstants.CpuUsageCategory.PLAYER_JOIN));
    }

    @Override
    public void onQuit(@NotNull TabPlayer disconnectedPlayer) {
        customThread.execute(new TimedCaughtTask(HavocTab.getInstance().getCpu(),
                this::apply, getFeatureName(), TabConstants.CpuUsageCategory.PLAYER_QUIT));
    }

    /**
     * Advances to the next page and redraws.
     */
    private void flipPage() {
        currentPage++;
        apply();
    }

    /**
     * Recomputes which players belong on the current page and updates every viewer.
     * <p>
     * When everyone fits on one page this un-hides all players and does nothing else, so
     * a server that rarely fills up pays almost nothing for having this enabled.
     */
    private void apply() {
        TabPlayer[] online = HavocTab.getInstance().getOnlinePlayers();
        int pageSize = configuration.getPageSize();

        if (online.length <= pageSize) {
            currentPage = 0;
            totalPages = 1;
            if (!hidden.isEmpty()) {
                for (TabPlayer viewer : online) {
                    if (!supports(viewer)) continue;
                    for (TabPlayer target : online) {
                        viewer.getTabList().updateListed(target, true);
                    }
                }
                hidden.clear();
            }
            return;
        }

        // Stable order, so a player does not jump between pages every flip
        List<TabPlayer> sorted = new ArrayList<>(Arrays.asList(online));
        sorted.sort(Comparator.comparing(TabPlayer::getName, String.CASE_INSENSITIVE_ORDER));

        totalPages = (sorted.size() + pageSize - 1) / pageSize;
        if (currentPage >= totalPages) currentPage = 0;

        int from = currentPage * pageSize;
        int to = Math.min(from + pageSize, sorted.size());
        List<TabPlayer> visible = sorted.subList(from, to);

        hidden.clear();
        for (TabPlayer target : sorted) {
            if (!visible.contains(target)) hidden.add(target);
        }

        for (TabPlayer viewer : online) {
            if (!supports(viewer)) continue;
            for (TabPlayer target : sorted) {
                boolean listed = visible.contains(target)
                        || (configuration.isAlwaysShowSelf() && target == viewer);
                viewer.getTabList().updateListed(target, listed);
            }
        }

        if (placeholders != null) {
            for (TabPlayer viewer : online) {
                for (PlayerPlaceholderImpl placeholder : placeholders) {
                    placeholder.update(viewer);
                }
            }
        }
    }

    /**
     * Returns whether this viewer's client understands the listed flag.
     * <p>
     * The flag was added in 1.19.3. Below that the only way to hide someone is to remove
     * their tablist entry, which would churn skins and teams several times a minute, so
     * those players simply keep seeing the unpaginated list.
     *
     * @param   viewer
     *          Player to check
     * @return  {@code true} if pagination can be applied to this viewer
     */
    private boolean supports(@NotNull TabPlayer viewer) {
        return viewer.getVersionId() >= ProtocolVersion.V1_19_3.getNetworkId();
    }

    @NotNull
    @Override
    public String getFeatureName() {
        return "TablistPages";
    }
}
