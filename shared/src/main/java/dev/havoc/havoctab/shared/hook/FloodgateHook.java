package dev.havoc.havoctab.shared.hook;

import lombok.Getter;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.util.ReflectionUtils;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Class for hooking into floodgate to detect bedrock players and
 * adapt features for them for best experience.
 */
public class FloodgateHook {

    /** Instance of the class */
    @Getter
    private static final FloodgateHook instance = new FloodgateHook();

    /** Flag tracking if floodgate is installed or not */
    @Getter
    private final boolean installed = ReflectionUtils.classExists("org.geysermc.floodgate.api.FloodgateApi");

    /**
     * Returns {@code true} if this player is a bedrock player,
     * {@code false} if not.
     *
     * @param   uniqueId
     *          UUID of player
     * @param   name
     *          Player's name, used in debug message
     * @return  {@code true} if bedrock player, {@code false} if not
     */
    public boolean isFloodgatePlayer(@NotNull UUID uniqueId, @NotNull String name) {
        if (!installed) return false;
        if (FloodgateApi.getInstance() == null) {
            HavocTab.getInstance().debug("Floodgate is installed, but API returned null. Could not check player " + name);
            return false;
        }
        boolean bedrock = FloodgateApi.getInstance().isFloodgatePlayer(uniqueId);
        HavocTab.getInstance().debug("Floodgate returned bedrock status " + String.valueOf(bedrock).toUpperCase() + " for player " + name);
        return bedrock;
    }
}
