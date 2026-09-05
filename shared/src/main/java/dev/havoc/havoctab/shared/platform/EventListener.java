package dev.havoc.havoctab.shared.platform;

import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants.CpuUsageCategory;
import dev.havoc.havoctab.shared.cpu.TimedCaughtTask;
import dev.havoc.havoctab.shared.data.World;
import dev.havoc.havoctab.shared.task.PluginMessageDecodeTask;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Class for methods called by platform's event listener.
 *
 * @param   <T>
 *          Platform's player class
 */
public interface EventListener<T> {

    /**
     * Processes player join by forwarding it to all features.
     *
     * @param   player
     *          Player who joined
     */
    default void join(@NotNull T player) {
        if (HavocTab.getInstance().isPluginDisabled()) return;
        HavocTab.getInstance().getCPUManager().runTask(() ->
                HavocTab.getInstance().getFeatureManager().onJoin(createPlayer(player)));
    }

    /**
     * Processes player quit by forwarding it to all features.
     *
     * @param   player
     *          UUID of player who left
     */
    default void quit(@NotNull UUID player) {
        if (HavocTab.getInstance().isPluginDisabled()) return;
        HavocTab.getInstance().getCPUManager().runTask(() ->
                HavocTab.getInstance().getFeatureManager().onQuit(HavocTab.getInstance().getPlayer(player)));
    }

    /**
     * Processes world change by forwarding it to all features.
     *
     * @param   player
     *          Player who switched world
     * @param   world
     *          New world
     */
    default void worldChange(@NotNull UUID player, @NotNull String world) {
        if (HavocTab.getInstance().isPluginDisabled()) return;
        HavocTab.getInstance().getCPUManager().runTask(() ->
                HavocTab.getInstance().getFeatureManager().onWorldChange(player, World.byName(world)));
    }

    /**
     * Processes plugin message.
     *
     * @param   player
     *          UUID of player who received message
     * @param   message
     *          The message
     */
    default void pluginMessage(@NotNull UUID player, byte[] message) {
        HavocTab.getInstance().getCpu().getPluginMessageDecodeThread().execute(new TimedCaughtTask(HavocTab.getInstance().getCpu(), new PluginMessageDecodeTask(player, message),
                "Plugin message handling", CpuUsageCategory.PLUGIN_MESSAGE_DECODE));
    }

    /**
     * Replaces player object reference. Needed on modded servers,
     * as vanilla replaces player object on respawn, and they
     * preserve that behavior.
     *
     * @param   player
     *          UUID of affected player
     * @param   newPlayer
     *          New player object
     */
    default void replacePlayer(UUID player, T newPlayer) {
        if (HavocTab.getInstance().isPluginDisabled()) return;
        TabPlayer p = HavocTab.getInstance().getPlayer(player);
        if (p == null) return;
        p.setPlayer(newPlayer);
    }

    /**
     * Creates new TabPlayer instance from given player object.
     *
     * @param   player
     *          Platform's player object
     * @return  New TabPlayer from given player object
     */
    @NotNull
    TabPlayer createPlayer(@NotNull T player);
}
