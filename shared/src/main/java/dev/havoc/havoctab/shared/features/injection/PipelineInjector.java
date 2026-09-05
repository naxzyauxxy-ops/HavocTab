package dev.havoc.havoctab.shared.features.injection;

import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.features.types.JoinListener;
import dev.havoc.havoctab.shared.features.types.Loadable;
import dev.havoc.havoctab.shared.features.types.TabFeature;
import dev.havoc.havoctab.shared.features.types.UnLoadable;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Packet intercepting to secure proper functionality of some features:
 * TabList names - anti-override
 * NameTags - anti-override
 * Scoreboard - disabling tab's scoreboard to prevent conflict
 * PingSpoof - full feature functionality
 * NickCompatibility - Detect name changes from other plugins
 */
public abstract class PipelineInjector extends TabFeature implements JoinListener, Loadable, UnLoadable {

    @NotNull
    @Override
    public String getFeatureName() {
        return "Pipeline injection";
    }

    /**
     * Injects handler into player's channel.
     *
     * @param   player
     *          Player to inject
     */
    public abstract void inject(@NotNull TabPlayer player);

    /**
     * Un-injects handler from player's channel.
     *
     * @param   player
     *          Player to remove handler from
     */
    public abstract void uninject(@NotNull TabPlayer player);

    @Override
    public void load() {
        for (TabPlayer p : HavocTab.getInstance().getOnlinePlayers()) {
            inject(p);
        }
    }

    @Override
    public void unload() {
        for (TabPlayer p : HavocTab.getInstance().getOnlinePlayers()) {
            uninject(p);
        }
    }

    @Override
    public void onJoin(@NotNull TabPlayer connectedPlayer) {
        inject(connectedPlayer);
    }
}