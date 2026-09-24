package dev.havoc.havoctab.shared.task;

import lombok.RequiredArgsConstructor;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants.CpuUsageCategory;
import dev.havoc.havoctab.shared.proxy.ProxyTabPlayer;
import dev.havoc.havoctab.shared.proxy.message.outgoing.OutgoingMessage;

/**
 * Task for encoding and sending plugin message to a player.
 */
@RequiredArgsConstructor
public class PluginMessageEncodeTask implements Runnable {

    /** Player to send plugin message to */
    private final ProxyTabPlayer player;

    /** Plugin message to encode and send */
    private final OutgoingMessage message;

    @Override
    public void run() {
        long time = System.nanoTime();
        byte[] msg = message.write().toByteArray();
        HavocTab.getInstance().getCpu().addTime("Plugin message handling", CpuUsageCategory.PLUGIN_MESSAGE_ENCODE, System.nanoTime() - time);
        time = System.nanoTime();
        player.sendPluginMessage(msg);
        HavocTab.getInstance().getCpu().addTime("Plugin message handling", CpuUsageCategory.PLUGIN_MESSAGE_SEND, System.nanoTime() - time);
    }
}
