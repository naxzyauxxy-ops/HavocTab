package dev.havoc.havoctab.shared.features.proxy.message;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import lombok.ToString;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.data.Server;
import dev.havoc.havoctab.shared.features.proxy.ProxyPlayer;
import dev.havoc.havoctab.shared.features.proxy.ProxySupport;
import dev.havoc.havoctab.shared.features.proxy.QueuedData;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Message sent from another proxy to switch a player to a different server.
 */
@AllArgsConstructor
@ToString
public class ServerSwitch extends ProxyMessage {

    @NotNull private final UUID playerId;
    @NotNull private final Server newServer;

    /**
     * Creates new instance and reads data from byte input.
     *
     * @param   in
     *          Input stream to read from
     */
    public ServerSwitch(@NotNull ByteArrayDataInput in) {
        playerId = readUUID(in);
        newServer = Server.byName(in.readUTF());
    }

    @Override
    public void write(@NotNull ByteArrayDataOutput out) {
        writeUUID(out, playerId);
        out.writeUTF(newServer.getName());
    }

    @Override
    public void process(@NotNull ProxySupport proxySupport) {
        ProxyPlayer target = proxySupport.getProxyPlayers().get(playerId);
        if (target == null) {
            unknownPlayer(playerId.toString(), "server switch");
            QueuedData data = proxySupport.getQueuedData().computeIfAbsent(playerId, k -> new QueuedData());
            data.setServer(newServer);
            return;
        }
        target.setServer(newServer);
        HavocTab.getInstance().getFeatureManager().onServerSwitch(target);
    }
}
