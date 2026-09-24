package dev.havoc.havoctab.platforms.bungeecord;

import dev.havoc.havoctab.shared.ProtocolVersion;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.chat.component.TabComponent;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

/**
 * Command handler for plugin's command on BungeeCord.
 */
public class BungeeTabCommand extends Command implements TabExecutor {

    /**
     * Constructs new instance.
     *
     * @param   command
     *          Command to register
     */
    public BungeeTabCommand(@NotNull String command) {
        super(command, null);
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (HavocTab.getInstance().isPluginDisabled()) {
            for (TabComponent message : HavocTab.getInstance().getDisabledCommand().execute(args, sender.hasPermission(TabConstants.Permission.COMMAND_RELOAD), sender.hasPermission(TabConstants.Permission.COMMAND_ALL))) {
                if (sender instanceof ProxiedPlayer) {
                    sender.sendMessage(((BungeePlatform)HavocTab.getInstance().getPlatform()).transformComponent(
                            message,
                            ProtocolVersion.fromNetworkId(((ProxiedPlayer)sender).getPendingConnection().getVersion())
                    ));
                } else {
                    // Bungee console does not actually support components, internal toLegacyText is called when using component
                    sender.sendMessage(message.toLegacyText());
                }
            }
        } else {
            TabPlayer p = null;
            if (sender instanceof ProxiedPlayer) {
                p = HavocTab.getInstance().getPlayer(((ProxiedPlayer)sender).getUniqueId());
                if (p == null) return; //player not loaded correctly
            }
            HavocTab.getInstance().getCommand().execute(p, args);
        }
    }

    @Override
    @NotNull
    public Iterable<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        TabPlayer p = null;
        if (sender instanceof ProxiedPlayer) {
            p = HavocTab.getInstance().getPlayer(((ProxiedPlayer)sender).getUniqueId());
            if (p == null) return Collections.emptyList(); //player not loaded correctly
        }
        return HavocTab.getInstance().getCommand().complete(p, args);
    }
}
