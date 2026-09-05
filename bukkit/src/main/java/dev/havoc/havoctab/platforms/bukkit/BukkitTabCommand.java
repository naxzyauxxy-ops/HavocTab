package dev.havoc.havoctab.platforms.bukkit;

import dev.havoc.havoctab.platforms.bukkit.platform.BukkitPlatform;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.chat.component.TabComponent;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Command handler for /tab command
 */
public class BukkitTabCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (HavocTab.getInstance().isPluginDisabled()) {
            for (TabComponent message : HavocTab.getInstance().getDisabledCommand().execute(args, sender.hasPermission(TabConstants.Permission.COMMAND_RELOAD), sender.hasPermission(TabConstants.Permission.COMMAND_ALL))) {
                sender.sendMessage(((BukkitPlatform)HavocTab.getInstance().getPlatform()).toBukkitFormat(message));
            }
        } else {
            TabPlayer p = null;
            if (sender instanceof Player) {
                p = HavocTab.getInstance().getPlayer(((Player)sender).getUniqueId());
                if (p == null) return true; //player not loaded correctly
            }
            HavocTab.getInstance().getCommand().execute(p, args);
        }
        return false;
    }

    @Override
    @NotNull
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        TabPlayer p = null;
        if (sender instanceof Player) {
            p = HavocTab.getInstance().getPlayer(((Player)sender).getUniqueId());
            if (p == null) return Collections.emptyList(); //player not loaded correctly
        }
        return HavocTab.getInstance().getCommand().complete(p, args);
    }
}