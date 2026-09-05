package dev.havoc.havoctab.shared.command.scoreboard;

import dev.havoc.havoctab.api.scoreboard.Scoreboard;
import dev.havoc.havoctab.api.scoreboard.ScoreboardManager;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.command.SubCommand;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Handler for "/tab scoreboard show &lt;name&gt; [player]" subcommand
 */
public class ScoreboardShowCommand extends SubCommand {

    /**
     * Constructs new instance
     */
    public ScoreboardShowCommand() {
        super("show", TabConstants.Permission.COMMAND_SCOREBOARD_SHOW);
    }

    @Override
    public void execute(@Nullable TabPlayer sender, @NotNull String[] args) {
        ScoreboardManager scoreboard = HavocTab.getInstance().getFeatureManager().getFeature(TabConstants.Feature.SCOREBOARD);
        if (scoreboard == null) {
            sendMessage(sender, getMessages().getScoreboardFeatureNotEnabled());
            return;
        }
        if (args.length == 0 || args.length > 2) {
            sendMessage(sender, getMessages().getScoreboardShowUsage());
            return;
        }
        Scoreboard sb = scoreboard.getRegisteredScoreboards().get(args[0]);
        if (sb == null) {
            sendMessage(sender, getMessages().getScoreboardNotFound(args[0]));
            return;
        }
        TabPlayer target;
        if (args.length == 1) {
            if (!hasPermission(sender,TabConstants.Permission.COMMAND_SCOREBOARD_SHOW)) {
                sendMessage(sender, getMessages().getNoPermission());
                return;
            }
            if (sender == null) {
                sendMessage(null, getMessages().getCommandOnlyFromGame());
                return;
            }
            target = sender;
        } else {
            if (!hasPermission(sender,TabConstants.Permission.COMMAND_SCOREBOARD_SHOW_OTHER)) {
                sendMessage(sender, getMessages().getNoPermission());
                return;
            }
            target = HavocTab.getInstance().getPlayer(args[1]);
            if (target == null) {
                sendMessage(sender, getMessages().getPlayerNotFound(args[1]));
                return;
            }
        }
        scoreboard.showScoreboard(target, sb);
    }

    @Override
    public @NotNull List<String> complete(@Nullable TabPlayer sender, @NotNull String[] arguments) {
        ScoreboardManager scoreboard = HavocTab.getInstance().getFeatureManager().getFeature(TabConstants.Feature.SCOREBOARD);
        if (scoreboard == null) return Collections.emptyList();
        if (arguments.length == 1) return getStartingArgument(scoreboard.getRegisteredScoreboards().keySet(), arguments[0]);
        if (arguments.length == 2) return getOnlinePlayers(arguments[1]);
        return Collections.emptyList();
    }
}