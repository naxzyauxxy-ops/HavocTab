package dev.havoc.havoctab.shared.proxy.message.incoming;

import com.google.common.io.ByteArrayDataInput;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.placeholders.types.PlayerPlaceholderImpl;
import dev.havoc.havoctab.shared.proxy.ProxyTabPlayer;
import org.jetbrains.annotations.NotNull;

public class Vanished implements IncomingMessage {

    private boolean vanished;

    @Override
    public void read(@NotNull ByteArrayDataInput in) {
        vanished = in.readBoolean();
    }

    @Override
    public void process(@NotNull ProxyTabPlayer player) {
        if (player.isVanished() != vanished) {
            player.setVanished(vanished);
            HavocTab.getInstance().getFeatureManager().onVanishStatusChange(player);
            ((PlayerPlaceholderImpl) HavocTab.getInstance().getPlaceholderManager().getPlaceholder(TabConstants.Placeholder.VANISHED))
                    .updateValue(player, Boolean.toString(player.isVanished()));
        }
    }
}
