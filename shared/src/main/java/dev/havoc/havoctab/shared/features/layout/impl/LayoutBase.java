package dev.havoc.havoctab.shared.features.layout.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import dev.havoc.havoctab.shared.features.layout.LayoutManagerImpl;
import dev.havoc.havoctab.shared.features.layout.pattern.LayoutPattern;
import dev.havoc.havoctab.shared.features.layout.impl.common.PlayerSlot;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for all layout implementations.
 */
@Getter
@RequiredArgsConstructor
public abstract class LayoutBase {

    @NotNull
    protected final LayoutManagerImpl manager;

    @NotNull
    protected final LayoutPattern pattern;

    @NotNull
    protected final TabPlayer viewer;

    /**
     * Sends the layout to viewer.
     */
    public abstract void send();

    /**
     * Removes all entries added by this layout from viewer's tablist.
     */
    public abstract void destroy();

    /**
     * Ticks all players. This may end up moving players into different groups.
     */
    public abstract void tick();

    /**
     * Returns slot in which specified player is present. If player is not found, {@code null} is returned.
     *
     * @param   target
     *          Player to search for
     * @return  Slot of specified player or {@code null} if not found
     */
    @Nullable
    public abstract PlayerSlot getSlot(@NotNull TabPlayer target);

    /**
     * Processes player join.
     *
     * @param   player
     *          Player who joined
     */
    public void onJoin(@NotNull TabPlayer player) {
        tick();
    }
}
