package dev.havoc.havoctab.shared.features.types;

import dev.havoc.havoctab.shared.data.Server;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for features listening to players switching servers
 */
public interface ServerSwitchListener {

    /**
     * Called when player switched server
     *
     * @param   changed
     *          Player who changed server
     * @param   from
     *          Name of previous server
     * @param   to
     *          Name of new server
     */
    void onServerChange(@NotNull TabPlayer changed, @NotNull Server from, @NotNull Server to);
}
