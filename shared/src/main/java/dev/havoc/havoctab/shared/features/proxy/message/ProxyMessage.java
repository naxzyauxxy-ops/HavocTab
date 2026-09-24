package dev.havoc.havoctab.shared.features.proxy.message;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.cpu.ThreadExecutor;
import dev.havoc.havoctab.shared.features.proxy.ProxySupport;
import dev.havoc.havoctab.shared.platform.TabList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class ProxyMessage {

    @Nullable
    public ThreadExecutor getCustomThread() {
        return null;
    }

    public void writeUUID(@NotNull ByteArrayDataOutput out, @NotNull UUID id) {
        out.writeLong(id.getMostSignificantBits());
        out.writeLong(id.getLeastSignificantBits());
    }

    public UUID readUUID(@NotNull ByteArrayDataInput in) {
        return new UUID(in.readLong(), in.readLong());
    }

    public void writeSkin(@NotNull ByteArrayDataOutput out, @Nullable TabList.Skin skin) {
        out.writeBoolean(skin != null);
        if (skin != null) {
            out.writeUTF(skin.getValue());
            out.writeBoolean(skin.getSignature() != null);
            if (skin.getSignature() != null) {
                out.writeUTF(skin.getSignature());
            }
        }
    }

    @Nullable
    public TabList.Skin readSkin(@NotNull ByteArrayDataInput in) {
        if (!in.readBoolean()) return null;
        String value = in.readUTF();
        String signature = null;
        if (in.readBoolean()) {
            signature = in.readUTF();
        }
        return new TabList.Skin(value, signature);
    }

    public abstract void write(@NotNull ByteArrayDataOutput out);

    public abstract void process(@NotNull ProxySupport proxySupport);

    public void unknownPlayer(@NotNull String playerId, @NotNull String action) {
        HavocTab.getInstance().debug("[Proxy Support] Unable to process " + action + " of proxy player " + playerId + ", because no such player exists. Queueing data.");
    }
}
