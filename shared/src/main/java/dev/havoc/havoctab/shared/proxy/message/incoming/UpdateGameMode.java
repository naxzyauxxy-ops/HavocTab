package dev.havoc.havoctab.shared.proxy.message.incoming;

import com.google.common.io.ByteArrayDataInput;
import dev.havoc.havoctab.shared.proxy.ProxyTabPlayer;
import org.jetbrains.annotations.NotNull;

public class UpdateGameMode implements IncomingMessage {

    private int gameMode;

    @Override
    public void read(@NotNull ByteArrayDataInput in) {
       gameMode = in.readInt();
    }

    @Override
    public void process(@NotNull ProxyTabPlayer player) {
        player.setGamemode(gameMode);
    }
}
