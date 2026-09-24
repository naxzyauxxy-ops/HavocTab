package dev.havoc.havoctab.shared.proxy.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import org.jetbrains.annotations.NotNull;

public interface OutgoingMessage {

    @NotNull
    ByteArrayDataOutput write();
}
