package dev.havoc.havoctab.shared.proxy.message.incoming;

import com.google.common.io.ByteArrayDataInput;
import dev.havoc.havoctab.shared.proxy.ProxyTabPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Incoming plugin message notifying proxy whether player is invisible or not.
 */
public class Invisible implements IncomingMessage {

    /** Invisibility status */
    private boolean invisible;

    @Override
    public void read(@NotNull ByteArrayDataInput in) {
        invisible = in.readBoolean();
    }

    @Override
    public void process(@NotNull ProxyTabPlayer player) {
        player.setInvisibilityPotion(invisible);
    }
}
