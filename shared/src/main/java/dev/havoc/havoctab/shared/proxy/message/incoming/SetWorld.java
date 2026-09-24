package dev.havoc.havoctab.shared.proxy.message.incoming;

import com.google.common.io.ByteArrayDataInput;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.data.World;
import dev.havoc.havoctab.shared.proxy.ProxyTabPlayer;
import org.jetbrains.annotations.NotNull;

public class SetWorld implements IncomingMessage {

    private World world;

    @Override
    public void read(@NotNull ByteArrayDataInput in) {
        world = World.byName(in.readUTF());
    }

    @Override
    public void process(@NotNull ProxyTabPlayer player) {
        HavocTab.getInstance().getFeatureManager().onWorldChange(player.getUniqueId(), world);
    }
}
