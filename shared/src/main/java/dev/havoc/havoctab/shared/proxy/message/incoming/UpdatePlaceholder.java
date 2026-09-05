package dev.havoc.havoctab.shared.proxy.message.incoming;

import com.google.common.io.ByteArrayDataInput;
import dev.havoc.havoctab.api.placeholder.PlayerPlaceholder;
import dev.havoc.havoctab.api.placeholder.RelationalPlaceholder;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.placeholders.PlaceholderReference;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import dev.havoc.havoctab.shared.proxy.ProxyTabPlayer;
import org.jetbrains.annotations.NotNull;

public class UpdatePlaceholder implements IncomingMessage {

    private String identifier;
    private String target; // rel only
    private String value;

    @Override
    public void read(@NotNull ByteArrayDataInput in) {
        identifier = in.readUTF();
        if (identifier.startsWith("%rel_")) target = in.readUTF();
        value = in.readUTF();
    }

    @Override
    public void process(@NotNull ProxyTabPlayer player) {
        player.getPlaceholders().put(identifier, value);
        // Ignore placeholders that were not registered with this reload
        // (for example, a condition was used in config but not defined, but now it is defined).
        // It is also in bridge memory, but bridge will not return the correct value, so ignore it.
        if (!HavocTab.getInstance().getPlaceholderManager().getBridgePlaceholders().containsKey(identifier)) return;

        PlaceholderReference placeholder = HavocTab.getInstance().getPlaceholderManager().getPlaceholderRaw(identifier);
        if (placeholder == null) return;
        if (placeholder.getHandle() instanceof RelationalPlaceholder) {
            TabPlayer other = HavocTab.getInstance().getPlayer(target);
            if (other != null) { // Backend player did not connect via this proxy if null
                ((RelationalPlaceholder)placeholder.getHandle()).updateValue(player, other, value);
            }
        } else if (placeholder.getHandle() instanceof PlayerPlaceholder) {
            ((PlayerPlaceholder)placeholder.getHandle()).updateValue(player, value);
        }
    }
}
