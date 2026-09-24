package dev.havoc.havoctab.shared.features.types;

import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Listener for features listening to player group change.
 */
public interface GroupListener {

    /**
     * Called when primary group of a player changes.
     *
     * @param   player
     *          Player whose primary group changed
     */
    void onGroupChange(@NotNull TabPlayer player);
}
