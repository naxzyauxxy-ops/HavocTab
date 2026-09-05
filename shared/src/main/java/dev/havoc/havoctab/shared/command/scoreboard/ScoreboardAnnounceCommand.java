package dev.havoc.havoctab.shared.command.scoreboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import dev.havoc.havoctab.shared.platform.TabPlayer;
import dev.havoc.havoctab.api.scoreboard.Scoreboard;
import dev.havoc.havoctab.api.scoreboard.ScoreboardManager;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.command.SubCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handler for "/tab scoreboard announce &lt;name&gt; &lt;length&gt;" subcommand
 */
public class ScoreboardAnnounceCommand extends SubCommand {

    /**
     * Constructs new instance
     */
    public ScoreboardAnnounceCommand() {
        super("announce", TabConstants.Permission.COMMAND_SCOREBOARD_ANNOUNCE);
    }

    @Override
    public void execute(@Nullable TabPlayer sender, @NotNull String[] args) {
        ScoreboardManager feature = HavocTab.getInstance().getFeatureManager().getFeature(TabConstants.Feature.SCOREBOARD);
        if (feature == null) {
            sendMessage(sender, getMessages().getScoreboardFeatureNotEnabled());
            return;
        }
        if (args.length != 2) {
            sendMessage(sender, getMessages().getScoreboardAnnounceCommandUsage());
            return;
        }
        String scoreboard = args[0];
        int duration;
        try {
            duration = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sendMessage(sender, getMessages().getInvalidNumber(args[1]));
            return;
        }
        Scoreboard sb = feature.getRegisteredScoreboards().get(scoreboard);
        if (sb == null) {
            sendMessage(sender, getMessages().getScoreboardNotFound(scoreboard));
            return;
        }
        feature.announceScoreboard(sb.getName(), duration);
    }

    @Override
    public @NotNull List<String> complete(@Nullable TabPlayer sender, @NotNull String[] arguments) {
        ScoreboardManager s = HavocTab.getInstance().getFeatureManager().getFeature(TabConstants.Feature.SCOREBOARD);
        if (s == null) return Collections.emptyList();
        List<String> suggestions = new ArrayList<>();
        if (arguments.length == 1) {
            for (String bar : s.getRegisteredScoreboards().keySet()) {
                if (bar.toLowerCase().startsWith(arguments[0].toLowerCase())) suggestions.add(bar);
            }
        } else if (arguments.length == 2 && s.getRegisteredScoreboards().get(arguments[0]) != null) {
            for (String time : Arrays.asList("5", "10", "30", "60", "120")) {
                if (time.startsWith(arguments[1])) suggestions.add(time);
            }
        }
        return suggestions;
    }
}