package dev.havoc.havoctab.platforms.neoforge;

import dev.havoc.havoctab.platforms.neoforge.hook.LuckPermsAPIHook;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.chat.component.TabComponent;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Command handler for plugin's command for Fabric.
 */
public class NeoForgeTabCommand extends NeoForgeCommand {

    /**
     * Constructs new instance with given command name.
     *
     * @param   commandName
     *         Command name
     */
    public NeoForgeTabCommand(@NotNull String commandName) {
        super(commandName);
    }

    @Override
    public int execute(@NotNull CommandSourceStack source, @NotNull String[] args) {
        if (HavocTab.getInstance().isPluginDisabled()) {
            boolean hasReloadPermission = LuckPermsAPIHook.hasPermission(source, TabConstants.Permission.COMMAND_RELOAD);
            boolean hasAdminPermission = LuckPermsAPIHook.hasPermission(source, TabConstants.Permission.COMMAND_ALL);
            for (TabComponent message : HavocTab.getInstance().getDisabledCommand().execute(args, hasReloadPermission, hasAdminPermission)) {
                source.sendSystemMessage(message.convert());
            }
        } else {
            if (source.getEntity() == null) {
                HavocTab.getInstance().getCommand().execute(null, args);
            } else {
                TabPlayer player = HavocTab.getInstance().getPlayer(source.getEntity().getUUID());
                if (player != null) HavocTab.getInstance().getCommand().execute(player, args);
            }
        }
        return 0;
    }

    @NotNull
    @Override
    public List<String> complete(@NotNull CommandSourceStack sender, @NotNull String[] args) {
        TabPlayer player = null;
        if (sender.getEntity() != null) {
            player = HavocTab.getInstance().getPlayer(sender.getEntity().getUUID());
            if (player == null) return Collections.emptyList();
        }
        return HavocTab.getInstance().getCommand().complete(player, args);
    }
}
