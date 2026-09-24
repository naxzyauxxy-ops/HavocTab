package dev.havoc.havoctab.shared.event.impl;

import lombok.Data;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import dev.havoc.havoctab.api.event.player.PlayerLoadEvent;
import org.jetbrains.annotations.NotNull;

@Data
public class PlayerLoadEventImpl implements PlayerLoadEvent {

    @NotNull private final TabPlayer player;
    private final boolean join;
}
