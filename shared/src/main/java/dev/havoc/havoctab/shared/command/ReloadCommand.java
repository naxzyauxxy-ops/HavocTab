package dev.havoc.havoctab.shared.command;

import dev.havoc.havoctab.shared.platform.TabPlayer;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handler for "/tab reload" subcommand
 */
public class ReloadCommand extends SubCommand {

    /**
     * Constructs new instance
     */
    public ReloadCommand() {
        super("reload", TabConstants.Permission.COMMAND_RELOAD);
    }

    @Override
    public void execute(@Nullable TabPlayer sender, @NotNull String[] args) {
        HavocTab.getInstance().unload();
        sendMessage(sender, HavocTab.getInstance().load());
    }
}