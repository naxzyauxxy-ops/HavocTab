package dev.havoc.havoctab.shared.command;

import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.data.Server;
import dev.havoc.havoctab.shared.data.World;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Handler for "/tab playeruuid" subcommand
 */
public class PlayerUUIDCommand extends PropertyCommand {

    /**
     * Constructs new instance
     */
    public PlayerUUIDCommand() {
        super("playeruuid");
    }

    @Override
    public void execute(@Nullable TabPlayer sender, @NotNull String[] args) {
        //<uuid> <property> [value...]
        if (args.length <= 1) {
            help(sender);
            return;
        }

        TabPlayer player = HavocTab.getInstance().getPlayer(args[0]);
        if (player == null) {
            sendMessage(sender, getMessages().getPlayerNotFound(args[0]));
            return;
        }
        String type = args[1].toLowerCase();
        if ("remove".equals(type)) {
            remove(sender, player);
            return;
        }
        trySaveEntity(sender, args);
    }

    private void remove(@Nullable TabPlayer sender, @NotNull TabPlayer changed) {
        if (hasPermission(sender, TabConstants.Permission.COMMAND_DATA_REMOVE)) {
            HavocTab.getInstance().getConfiguration().getUsers().remove(changed.getUniqueId().toString());
            HavocTab.getInstance().getFeatureManager().onGroupChange(changed);
            sendMessage(sender, getMessages().getPlayerDataRemoved(changed.getName() + "(" + changed.getUniqueId() + ")"));
        } else {
            sendMessage(sender, getMessages().getNoPermission());
        }
    }

    @Override
    public void saveEntity(@Nullable TabPlayer sender, @NotNull String playerName, @NotNull String type, @NotNull String value, @Nullable Server server, @Nullable World world) {
        TabPlayer player = HavocTab.getInstance().getPlayer(playerName);
        if (!value.isEmpty()) {
            sendMessage(sender, getMessages().getPlayerValueAssigned(type, value, playerName + "(" + player.getUniqueId() + ")"));
        } else {
            sendMessage(sender, getMessages().getPlayerValueRemoved(type, playerName + "(" + player.getUniqueId() + ")"));
        }
        String[] property = HavocTab.getInstance().getConfiguration().getUsers().getProperty(player.getUniqueId().toString(), type, server, world);
        if (property.length > 0 && String.valueOf(value.isEmpty() ? null : value).equals(String.valueOf(property[0]))) return;
        HavocTab.getInstance().getConfiguration().getUsers().setProperty(player.getUniqueId().toString(), type, server, world, value.isEmpty() ? null : value);
        HavocTab.getInstance().getFeatureManager().onGroupChange(player);
    }
    
    @Override
    public @NotNull List<String> complete(@Nullable TabPlayer sender, @NotNull String[] arguments) {
        if (arguments.length == 1) return getOnlinePlayers(arguments[0]);
        return super.complete(sender, arguments);
    }
}