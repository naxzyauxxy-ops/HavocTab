package dev.havoc.havoctab.shared.platform.impl;

import dev.havoc.havoctab.api.bossbar.BarColor;
import dev.havoc.havoctab.api.bossbar.BarStyle;
import dev.havoc.havoctab.shared.chat.component.TabComponent;
import dev.havoc.havoctab.shared.platform.decorators.SafeBossBar;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Dummy implementation when no suitable one is available.
 */
public class DummyBossBar extends SafeBossBar<Object> {

    @Override
    @NotNull
    public Object constructBossBar(@NotNull UUID id, @NotNull TabComponent title, float progress, @NotNull BarColor color, @NotNull BarStyle style) {
        return new Object();
    }

    @Override
    public void show(@NotNull BossBarInfo bar) {
        // Do nothing
    }

    @Override
    public void updateTitle(@NotNull BossBarInfo bar) {
        // Do nothing
    }

    @Override
    public void updateProgress(@NotNull BossBarInfo bar) {
        // Do nothing
    }

    @Override
    public void updateStyle(@NotNull BossBarInfo bar) {
        // Do nothing
    }

    @Override
    public void updateColor(@NotNull BossBarInfo bar) {
        // Do nothing
    }

    @Override
    public void hide(@NotNull BossBarInfo bar) {
        // Do nothing
    }
}
