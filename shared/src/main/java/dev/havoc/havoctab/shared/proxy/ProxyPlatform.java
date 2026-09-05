package dev.havoc.havoctab.shared.proxy;

import lombok.Getter;
import dev.havoc.havoctab.api.placeholder.Placeholder;
import dev.havoc.havoctab.shared.GroupManager;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.features.PerWorldPlayerListConfiguration;
import dev.havoc.havoctab.shared.features.PlaceholderManagerImpl;
import dev.havoc.havoctab.shared.features.types.TabFeature;
import dev.havoc.havoctab.shared.hook.LuckPermsHook;
import dev.havoc.havoctab.shared.placeholders.UniversalPlaceholderRegistry;
import dev.havoc.havoctab.shared.placeholders.types.PlayerPlaceholderImpl;
import dev.havoc.havoctab.shared.platform.Platform;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import dev.havoc.havoctab.shared.proxy.message.outgoing.RegisterPlaceholder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Abstract class containing common variables and methods
 * shared between proxies.
 */
@Getter
public abstract class ProxyPlatform implements Platform {

    @Override
    public @NotNull GroupManager detectPermissionPlugin() {
        if (LuckPermsHook.getInstance().isInstalled() &&
                !HavocTab.getInstance().getConfiguration().getConfig().isBukkitPermissions()) {
            return new GroupManager("LuckPerms", LuckPermsHook.getInstance().getGroupFunction());
        }
        return new GroupManager("Vault through Bridge", TabPlayer::getGroup);
    }

    @Override
    public void registerUnknownPlaceholder(@NotNull String identifier) {
        PlaceholderManagerImpl pl = HavocTab.getInstance().getPlaceholderManager();
        Placeholder placeholder;
        int refresh = pl.getConfiguration().getRefreshInterval(identifier);
        if (identifier.startsWith("%rel_")) {
            placeholder = pl.registerRelationalBridgePlaceholder(identifier, refresh);
        } else {
            placeholder = pl.registerBridgePlaceholder(identifier, refresh);
        }
        for (TabPlayer all : HavocTab.getInstance().getOnlinePlayers()) {
            ((ProxyTabPlayer)all).sendPluginMessage(new RegisterPlaceholder(placeholder.getIdentifier(), refresh));
            Map<String, String> bridgePlaceholders = ((ProxyTabPlayer)all).getPlaceholders();
            // TODO do relational placeholders as well
            if (placeholder instanceof PlayerPlaceholderImpl && bridgePlaceholders.containsKey(identifier)) {
                ((PlayerPlaceholderImpl)placeholder).updateValue(all, bridgePlaceholders.get(identifier));
            }
        }
    }

    @Override
    public void registerPlaceholders() {
        HavocTab.getInstance().getPlaceholderManager().registerServerPlaceholder(TabConstants.Placeholder.TPS, -1,
                () -> "\"tps\" is a backend-only placeholder as the proxy does not tick anything. If you wish to display TPS of " +
                        "the server player is connected to, use placeholders from PlaceholderAPI and install TAB-Bridge for forwarding support to the proxy.");
        new UniversalPlaceholderRegistry().registerPlaceholders(HavocTab.getInstance().getPlaceholderManager());
    }

    @Override
    public @Nullable TabFeature getPerWorldPlayerList(@NotNull PerWorldPlayerListConfiguration configuration) { return null; }

    @Override
    public boolean isProxy() {
        return true;
    }

    /**
     * Registers plugin's plugin message channel
     */
    public abstract void registerChannel();
}
