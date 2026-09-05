package dev.havoc.havoctab.platforms.fand;

import io.fand.api.plugin.Plugin;
import io.fand.api.plugin.PluginContext;
import dev.havoc.havoctab.shared.HavocTab;

/** Fand plugin entry point. */
public final class FandHavocTab implements Plugin {

    @Override
    public void onEnable(PluginContext context) {
        HavocTab.create(new FandPlatform(context));
    }

    @Override
    public void onDisable(PluginContext context) {
        if (HavocTab.getInstance() != null) {
            HavocTab.getInstance().unload();
        }
    }
}
