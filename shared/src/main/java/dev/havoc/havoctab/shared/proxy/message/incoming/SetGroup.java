package dev.havoc.havoctab.shared.proxy.message.incoming;

import com.google.common.io.ByteArrayDataInput;
import dev.havoc.havoctab.shared.proxy.ProxyTabPlayer;
import org.jetbrains.annotations.NotNull;

public class SetGroup implements IncomingMessage {

    private String group;

    @Override
    public void read(@NotNull ByteArrayDataInput in) {
        group = in.readUTF();
    }

    @Override
    public void process(@NotNull ProxyTabPlayer player) {
        player.setGroup(group);
    }
}
