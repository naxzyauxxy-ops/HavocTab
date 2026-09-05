package dev.havoc.havoctab.platforms.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.chat.component.TabComponent;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Command handler for plugin's command for Velocity.
 */
public class VelocityTabCommand implements SimpleCommand {

    @Override
    public void execute(@NotNull Invocation invocation) {
        CommandSource sender = invocation.source();
        if (HavocTab.getInstance().isPluginDisabled()) {
            for (TabComponent message : HavocTab.getInstance().getDisabledCommand().execute(invocation.arguments(), sender.hasPermission(TabConstants.Permission.COMMAND_RELOAD), sender.hasPermission(TabConstants.Permission.COMMAND_ALL))) {
                sender.sendMessage(message.toAdventure());
            }
        } else {
            TabPlayer p = null;
            if (sender instanceof Player) {
                p = HavocTab.getInstance().getPlayer(((Player)sender).getUniqueId());
                if (p == null) return; //player not loaded correctly
            }
            HavocTab.getInstance().getCommand().execute(p, invocation.arguments());
        }
    }

    @Override
    @NotNull
    public List<String> suggest(@NotNull Invocation invocation) {
        TabPlayer p = null;
        if (invocation.source() instanceof Player) {
            p = HavocTab.getInstance().getPlayer(((Player)invocation.source()).getUniqueId());
            if (p == null) return Collections.emptyList(); //player not loaded correctly
        }
        return HavocTab.getInstance().getCommand().complete(p, invocation.arguments());
    }
}
