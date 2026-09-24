package dev.havoc.havoctab.shared.command;

import dev.havoc.havoctab.shared.platform.TabPlayer;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handler for "/tab groups" subcommand
 */
public class GroupsCommand extends SubCommand {

    /**
     * Constructs new instance
     */
    public GroupsCommand() {
        super("groups", TabConstants.Permission.COMMAND_GROUP_LIST);
    }

    @Override
    public void execute(@Nullable TabPlayer sender, @NotNull String[] args) {
        sendMessage(sender, "&3Configured groups:");
        sendMessage(sender, "&9" + String.join(", &9", HavocTab.getInstance().getConfiguration().getGroups().getAllEntries()));
    }
}