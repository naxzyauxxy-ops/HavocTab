package dev.havoc.havoctab.shared.command;

import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handler for "/tab debug" subcommand
 */
public class DebugCommand extends SubCommand {

    /**
     * Constructs new instance
     */
    public DebugCommand() {
        super("debug", TabConstants.Permission.COMMAND_DEBUG);
    }

    @Override
    public void execute(@Nullable TabPlayer sender, @NotNull String[] args) {
        sendMessage(sender, "&cThe debug command was removed in favor of a new \"/" +
                HavocTab.getInstance().getPlatform().getCommand() + " dump <player>\" command.");
    }
}
