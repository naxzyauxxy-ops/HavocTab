package dev.havoc.havoctab.shared.backend;

import dev.havoc.havoctab.shared.GroupManager;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.features.PlaceholderManagerImpl;
import dev.havoc.havoctab.shared.features.proxy.ProxySupport;
import dev.havoc.havoctab.shared.hook.LuckPermsHook;
import dev.havoc.havoctab.shared.placeholders.UniversalPlaceholderRegistry;
import dev.havoc.havoctab.shared.platform.Platform;
import dev.havoc.havoctab.shared.util.PerformanceUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for backend platforms with a few default implementations, as well as new methods.
 */
public interface BackendPlatform extends Platform {

    @Override
    @NotNull default GroupManager detectPermissionPlugin() {
        if (LuckPermsHook.getInstance().isInstalled()) {
            return new GroupManager("LuckPerms", LuckPermsHook.getInstance().getGroupFunction());
        }
        return new GroupManager("None", p -> TabConstants.NO_GROUP);
    }

    @Override
    default ProxySupport getProxySupport(@NotNull String plugin, @NotNull String channelName) { return null; }

    @Override
    default void registerPlaceholders() {
        UniversalPlaceholderRegistry registry = new UniversalPlaceholderRegistry();
        PlaceholderManagerImpl manager = HavocTab.getInstance().getPlaceholderManager();
        manager.registerPlayerPlaceholder(TabConstants.Placeholder.HEALTH,
                p -> PerformanceUtil.toString((int) Math.ceil(((BackendTabPlayer)p).getHealth())));
        manager.registerPlayerPlaceholder(TabConstants.Placeholder.DISPLAY_NAME,
                p -> ((BackendTabPlayer)p).getDisplayName());
        manager.registerServerPlaceholder(TabConstants.Placeholder.TPS,
                () -> registry.getDecimal2().format(Math.min(20, getTPS())));
        manager.registerServerPlaceholder(TabConstants.Placeholder.MSPT,
                () -> registry.getDecimal2().format(getMSPT()));
        manager.registerPlayerPlaceholder(TabConstants.Placeholder.DEATHS,
                p -> PerformanceUtil.toString(((BackendTabPlayer)p).getDeaths()));
        registry.registerPlaceholders(manager);
    }

    @Override
    default boolean isProxy() {
        return false;
    }

    @Override
    @NotNull
    default String getCommand() {
        return "havoctab";
    }

    /**
     * Registers a dummy placeholder implementation for specified identifier in case
     * no placeholder plugin was found.
     *
     * @param   identifier
     *          Placeholder identifier to register
     */
    default void registerDummyPlaceholder(@NotNull String identifier) {
        if (identifier.startsWith("%rel_")) { // To prevent placeholder identifier check from throwing
            HavocTab.getInstance().getPlaceholderManager().registerRelationalPlaceholder(identifier, -1, (viewer, target) -> identifier);
        } else {
            HavocTab.getInstance().getPlaceholderManager().registerServerPlaceholder(identifier, -1, () -> identifier);
        }
    }

    /**
     * Returns server's TPS for {@link TabConstants.Placeholder#TPS} placeholder
     *
     * @return  server's TPS
     */
    double getTPS();

    /**
     * Returns server's MSPT for {@link TabConstants.Placeholder#MSPT} placeholder
     *
     * @return  server's MSPT
     */
    double getMSPT();
}
