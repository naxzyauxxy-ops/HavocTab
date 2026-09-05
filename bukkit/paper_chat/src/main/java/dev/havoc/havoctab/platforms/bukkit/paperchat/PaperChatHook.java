package dev.havoc.havoctab.platforms.bukkit.paperchat;

import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.platforms.bukkit.chat.BukkitBlockListMenu;
import dev.havoc.havoctab.shared.features.chat.ChatManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Entry point for HavocTab's Paper-only chat support.
 * <p>
 * Constructed reflectively by the Bukkit platform when the server actually has Paper's
 * chat and dialog classes. Nothing else in the plugin references this class directly, so
 * on Spigot or older Paper builds it is simply never loaded.
 */
public class PaperChatHook {

    /**
     * Registers the chat listener and the block list dialog.
     *
     * @param   plugin
     *          Plugin instance to register listeners under
     */
    public PaperChatHook(@NotNull JavaPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(new PaperChatListener(), plugin);

        if (BlockListDialog.isSupported()) {
            BlockListDialog dialog = new BlockListDialog();
            Bukkit.getPluginManager().registerEvents(dialog, plugin);
            // Registered as a provider, not as the block list view itself. The Bukkit menu
            // stays in charge of deciding who gets a dialog and who gets the chest GUI.
            BukkitBlockListMenu.setDialogOpener(dialog);
        }
    }

    /**
     * Returns the currently loaded chat feature, or {@code null} when chat is disabled
     * in config. Looked up per event rather than cached, so {@code /havoctab reload}
     * swapping the feature instance is handled for free.
     *
     * @return  Active chat feature, or {@code null}
     */
    @Nullable
    static ChatManager feature() {
        HavocTab instance = HavocTab.getInstance();
        if (instance == null) return null;
        return instance.getFeatureManager().getFeature(TabConstants.Feature.CHAT);
    }
}
