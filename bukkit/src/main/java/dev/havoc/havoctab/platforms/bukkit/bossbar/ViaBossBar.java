package dev.havoc.havoctab.platforms.bukkit.bossbar;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.legacy.bossbar.BossBar;
import com.viaversion.viaversion.api.legacy.bossbar.BossColor;
import com.viaversion.viaversion.api.legacy.bossbar.BossStyle;
import lombok.RequiredArgsConstructor;
import dev.havoc.havoctab.api.bossbar.BarColor;
import dev.havoc.havoctab.api.bossbar.BarStyle;
import dev.havoc.havoctab.platforms.bukkit.BukkitTabPlayer;
import dev.havoc.havoctab.shared.chat.component.TabComponent;
import dev.havoc.havoctab.shared.platform.decorators.SafeBossBar;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Handler for 1.9+ players on 1.8 server using ViaVersion API.
 */
@RequiredArgsConstructor
public class ViaBossBar extends SafeBossBar<BossBar> {

    /** Style array for fast access */
    private static final BossStyle[] styles = BossStyle.values();
    
    /** Player this handler belongs to */
    @NotNull
    private final BukkitTabPlayer player;

    @Override
    @NotNull
    public BossBar constructBossBar(@NotNull UUID id, @NotNull TabComponent title, float progress, @NotNull BarColor color, @NotNull BarStyle style) {
        return Via.getAPI().legacyAPI().createLegacyBossBar(
                title.toLegacyText(),
                progress,
                BossColor.valueOf(color.name()),
                styles[style.ordinal()]
        );
    }

    @Override
    public void show(@NotNull BossBarInfo bar) {
        bar.getBossBar().addPlayer(player.getPlayer().getUniqueId());
    }

    @Override
    public void updateTitle(@NotNull BossBarInfo bar) {
        bar.getBossBar().setTitle(bar.getTitle().toLegacyText());
    }

    @Override
    public void updateProgress(@NotNull BossBarInfo bar) {
        bar.getBossBar().setHealth(bar.getProgress());
    }

    @Override
    public void updateStyle(@NotNull BossBarInfo bar) {
        bar.getBossBar().setStyle(styles[bar.getStyle().ordinal()]);
    }

    @Override
    public void updateColor(@NotNull BossBarInfo bar) {
        bar.getBossBar().setColor(BossColor.valueOf(bar.getColor().name()));
    }

    @Override
    public void hide(@NotNull BossBarInfo bar) {
        bar.getBossBar().removePlayer(player.getPlayer().getUniqueId());
    }
}
