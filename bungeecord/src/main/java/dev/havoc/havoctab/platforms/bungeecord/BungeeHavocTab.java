package dev.havoc.havoctab.platforms.bungeecord;

import com.google.common.base.Function;
import io.netty.buffer.ByteBuf;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.util.ReflectionUtils;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.protocol.DefinedPacket;

/**
 * Main class for BungeeCord.
 */
public class BungeeHavocTab extends Plugin {

    @Override
    public void onEnable() {
        if (isCompatible()) {
            HavocTab.create(new BungeePlatform(this));
        } else {
            logIncompatibleVersionWarning();
        }
    }

    private boolean isCompatible() {
        return ReflectionUtils.methodExists(DefinedPacket.class, "readOptional", Function.class, ByteBuf.class);
    }

    private void logIncompatibleVersionWarning() {
        int buildNumber = 2068;
        String releaseDate = "May 9th, 2026";
        String oldTabVersion = "6.0.2";
        getLogger().severe("§c====================================================================================================");
        getLogger().severe(String.format("§cThe plugin requires BungeeCord build #%d (released on %s) and up (or an equivalent fork) to work.", buildNumber, releaseDate));
        getLogger().severe(String.format("§cIf you are using a fork that did not update to the new BungeeCord version yet, stay on HavocTab v%s, which supports older builds.", oldTabVersion));
        getLogger().severe("§c====================================================================================================");
    }

    @Override
    public void onDisable() {
        if (HavocTab.getInstance() != null) HavocTab.getInstance().unload();
    }
}