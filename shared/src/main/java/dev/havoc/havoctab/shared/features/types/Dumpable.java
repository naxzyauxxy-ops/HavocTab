package dev.havoc.havoctab.shared.features.types;

import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for features that support dumping data of a player.
 */
public interface Dumpable {

    /**
     * Dumps data of the given player.
     *
     * @param   player
     *          Player to dump data for
     * @return  Dumped data
     */
    @NotNull
    Object dump(@NotNull TabPlayer player);
}
