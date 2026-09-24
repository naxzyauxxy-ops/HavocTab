package dev.havoc.havoctab.shared;

import lombok.Getter;
import dev.havoc.havoctab.shared.cpu.TimedCaughtTask;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import dev.havoc.havoctab.shared.task.GroupRefreshTask;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Permission group manager retrieving groups from permission plugin
 */
@Getter
public class GroupManager {

    /** Permission plugin's name */
    @NotNull private final String permissionPlugin;

    /** Group retrieve function */
    @NotNull private final Function<TabPlayer, String> groupFunction;

    /** Function for retrieving player's group */
    private final Function<TabPlayer, String> detectGroup = HavocTab.getInstance().getConfiguration().getConfig().isGroupsByPermissions() ? this::getByPermission : this::getByPrimary;

    /**
     * Constructs new instance with given permission plugin and registers group placeholder.
     *
     * @param   permissionPlugin
     *          Name of detected permission plugin
     * @param   groupFunction
     *          Function returning group of a player
     */
    public GroupManager(@NotNull String permissionPlugin, @NotNull Function<TabPlayer, String> groupFunction) {
        this.permissionPlugin = permissionPlugin;
        this.groupFunction = groupFunction;
        HavocTab.getInstance().getCpu().getGroupRefreshingThread().repeatTask(new TimedCaughtTask(HavocTab.getInstance().getCpu(), new GroupRefreshTask(detectGroup),
                "Permission group refreshing", "Periodic task"), HavocTab.getInstance().getConfiguration().getConfig().getPermissionRefreshInterval());
    }

    /**
     * Detects player's permission group using configured method and returns it
     *
     * @param   player
     *          Player to detect permission group of
     * @return  Detected permission group
     */
    @NotNull
    public String detectPermissionGroup(@NotNull TabPlayer player) {
        return detectGroup.apply(player);
    }

    /**
     * Returns player's permission group from detected permission plugin
     *
     * @param   player
     *          Player to get permission group of
     * @return  Permission group from permission plugin
     */
    @NotNull
    private String getByPrimary(@NotNull TabPlayer player) {
        try {
            String group = groupFunction.apply(player);
            if (group != null) return group;
            HavocTab.getInstance().getErrorManager().nullGroupReturned(permissionPlugin, player);
        } catch (Exception e) {
            HavocTab.getInstance().getErrorManager().groupRetrieveException(permissionPlugin, player, e);
        }
        return TabConstants.NO_GROUP;
    }

    /**
     * Returns player's permission group based on highest permission
     * or {@link TabConstants#NO_GROUP} if player has no permission.
     *
     * @param   player
     *          Player to get permission group of
     * @return  Highest permission group player has permission for
     *          or {@link TabConstants#NO_GROUP} if player does not have any
     */
    @NotNull
    private String getByPermission(@NotNull TabPlayer player) {
        for (String group : HavocTab.getInstance().getConfiguration().getConfig().getPrimaryGroupFindingList()) {
            if (player.hasPermission(TabConstants.Permission.GROUP_PREFIX + group)) {
                return group;
            }
        }
        return TabConstants.NO_GROUP;
    }
}