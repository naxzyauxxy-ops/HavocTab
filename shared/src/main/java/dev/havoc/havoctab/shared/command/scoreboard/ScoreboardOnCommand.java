package dev.havoc.havoctab.shared.command.scoreboard;

import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.command.SubCommand;
import dev.havoc.havoctab.shared.features.scoreboard.ScoreboardManagerImpl;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Handler for "/tab scoreboard on [player] [options]" subcommand
 */
public class ScoreboardOnCommand extends SubCommand {

    /**
     * Constructs new instance
     */
    public ScoreboardOnCommand() {
        super("on", TabConstants.Permission.COMMAND_SCOREBOARD_TOGGLE);
    }

    @Override
    public void execute(@Nullable TabPlayer sender, @NotNull String[] args) {
        ScoreboardManagerImpl scoreboard = HavocTab.getInstance().getFeatureManager().getFeature(TabConstants.Feature.SCOREBOARD);
        if (scoreboard == null) {
            sendMessage(sender, getMessages().getScoreboardFeatureNotEnabled());
            return;
        }
        TabPlayer target = sender;
        if (args.length > 0) {
            if (hasPermission(sender, TabConstants.Permission.COMMAND_SCOREBOARD_TOGGLE_OTHER)) {
                target = HavocTab.getInstance().getPlayer(args[0]);
                if (target == null) {
                    sendMessage(sender, getMessages().getPlayerNotFound(args[0]));
                    return;
                }
            } else {
                sendMessage(sender, getMessages().getNoPermission());
                return;
            }
        } else if (target == null) {
            sendMessage(null, getMessages().getCommandOnlyFromGame());
            return;
        }
        boolean silent = args.length == 2 && args[1].equals("-s");
        scoreboard.setScoreboardVisible(target, true, !silent);
    }

    @Override
    public @NotNull List<String> complete(@Nullable TabPlayer sender, @NotNull String[] arguments) {
        if (arguments.length == 1) return getOnlinePlayers(arguments[0]);
        if (arguments.length == 2) return getStartingArgument(Collections.singletonList("-s"), arguments[1]);
        return Collections.emptyList();
    }
}