package dev.havoc.havoctab.platforms.fand;

import io.fand.api.command.CommandContext;
import io.fand.api.entity.Player;
import java.util.Collections;
import java.util.List;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.chat.component.TabComponent;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;

/** Handler for HavocTab's main command on Fand. */
public final class FandTabCommand extends FandCommand {

    public FandTabCommand(@NotNull String commandName) {
        super(commandName);
    }

    @Override
    protected void execute(@NotNull CommandContext context, @NotNull String[] arguments) {
        if (HavocTab.getInstance().isPluginDisabled()) {
            boolean reload = context.sender().can(TabConstants.Permission.COMMAND_RELOAD);
            boolean admin = context.sender().can(TabConstants.Permission.COMMAND_ALL);
            for (TabComponent message : HavocTab.getInstance().getDisabledCommand().execute(arguments, reload, admin)) {
                context.sender().sendMessage(message.toAdventure());
            }
            return;
        }

        if (context.sender() instanceof Player player) {
            TabPlayer tabPlayer = HavocTab.getInstance().getPlayer(player.uniqueId());
            if (tabPlayer != null) {
                HavocTab.getInstance().getCommand().execute(tabPlayer, arguments);
            }
        } else {
            HavocTab.getInstance().getCommand().execute(null, arguments);
        }
    }

    @Override
    @NotNull
    protected List<String> complete(@NotNull CommandContext context, @NotNull String[] arguments) {
        TabPlayer tabPlayer = null;
        if (context.sender() instanceof Player player) {
            tabPlayer = HavocTab.getInstance().getPlayer(player.uniqueId());
            if (tabPlayer == null) {
                return Collections.emptyList();
            }
        }
        return HavocTab.getInstance().getCommand().complete(tabPlayer, arguments);
    }
}
