package dev.havoc.havoctab.shared.task;

import lombok.RequiredArgsConstructor;
import dev.havoc.havoctab.shared.proxy.ProxyTabPlayer;
import dev.havoc.havoctab.shared.proxy.message.incoming.IncomingMessage;

/**
 * Task for processing incoming plugin messages from backend server.
 */
@RequiredArgsConstructor
public class PluginMessageProcessTask implements Runnable {

    /** Decoded plugin message */
    private final IncomingMessage message;

    /** Player who received the plugin message */
    private final ProxyTabPlayer player;

    @Override
    public void run() {
        message.process(player);
    }
}
