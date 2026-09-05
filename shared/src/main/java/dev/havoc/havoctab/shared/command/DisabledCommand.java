package dev.havoc.havoctab.shared.command;

import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.chat.TabTextColor;
import dev.havoc.havoctab.shared.chat.component.TabComponent;
import dev.havoc.havoctab.shared.chat.component.TabTextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Command processor when HavocTab is disabled due to broken configuration file
 */
public class DisabledCommand {

    @NotNull
    private final List<TabComponent> noPermission = Collections.singletonList(new TabTextComponent(
            "I'm sorry, but you do not have permission to perform this command. " +
                    "Please contact the server administrators if you believe that this is in error.",
            TabTextColor.RED
    ));

    /**
     * Performs command and return messages to be sent back
     *
     * @param   args
     *          command arguments
     * @param   hasReloadPermission
     *          if player has permission to reload or not
     * @param   hasAdminPermission
     *          if player has admin permission or not
     * @return  list of messages to send back
     */
    public List<TabComponent> execute(@NotNull String[] args, boolean hasReloadPermission, boolean hasAdminPermission) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (hasReloadPermission) {
                return Collections.singletonList(TabComponent.fromColoredText(HavocTab.getInstance().load()));
            } else {
                //cannot take message from file when syntax is broken
                return noPermission;
            }
        } else {
            if (hasAdminPermission) {
                return Collections.unmodifiableList(Arrays.asList(
                        new TabTextComponent("HavocTab is disabled due to an error. Check console for more details.", TabTextColor.RED),
                        new TabTextComponent("After fixing it, run /" + HavocTab.getInstance().getPlatform().getCommand() + " reload", TabTextColor.RED)
                ));
            } else {
                return Collections.emptyList();
            }
        }
    }
}
