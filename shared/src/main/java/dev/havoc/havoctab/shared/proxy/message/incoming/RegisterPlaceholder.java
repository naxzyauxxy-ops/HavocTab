package dev.havoc.havoctab.shared.proxy.message.incoming;

import com.google.common.io.ByteArrayDataInput;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.proxy.ProxyTabPlayer;
import org.jetbrains.annotations.NotNull;

public class RegisterPlaceholder implements IncomingMessage {

    private String identifier;

    @Override
    public void read(@NotNull ByteArrayDataInput in) {
        identifier = in.readUTF();
    }

    @Override
    public void process(@NotNull ProxyTabPlayer player) {
        HavocTab.getInstance().getPlaceholderManager().addUsedPlaceholder(identifier);
    }
}
