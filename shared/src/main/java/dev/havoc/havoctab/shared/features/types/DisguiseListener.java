package dev.havoc.havoctab.shared.features.types;

import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;

/** Listener for platform disguise status changes. */
public interface DisguiseListener {

    void onDisguiseStatusChange(@NotNull TabPlayer player);
}
