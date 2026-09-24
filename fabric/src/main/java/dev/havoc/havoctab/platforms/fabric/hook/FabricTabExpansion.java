package dev.havoc.havoctab.platforms.fabric.hook;

import eu.pb4.placeholders.api.*;
import lombok.Getter;
import dev.havoc.havoctab.shared.ProjectVariables;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.features.PlaceholderManagerImpl;
import dev.havoc.havoctab.shared.placeholders.expansion.TabExpansion;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.List;

/**
 * HavocTab's expansion for Text PlaceholderAPI
 */
@Getter
public class FabricTabExpansion implements TabExpansion {

    /**
     * Constructs new instance and registers internal placeholders.
     */
    public FabricTabExpansion() {
        List<String> placeholders = Arrays.asList(
                "tabprefix",
                "tabsuffix",
                "tagprefix",
                "tagsuffix",
                "customtabname",
                "tabprefix_raw",
                "tabsuffix_raw",
                "tagprefix_raw",
                "tagsuffix_raw",
                "customtabname_raw",
                "scoreboard_name",
                "scoreboard_visible",
                "bossbar_visible",
                "nametag_visibility"
        );
        for (String placeholder : placeholders) {
            registerPlaceholder(placeholder, (ctx, arg) -> {
                if (!ctx.hasPlayer()) return PlaceholderResult.invalid("No player!");
                TabPlayer player = HavocTab.getInstance().getPlayer(ctx.player().getUUID());
                return PlaceholderResult.value(player.expansionData.getValue(placeholder));
            });
        }

        registerPlaceholder("replace", (ctx, arg) -> {
            if (!ctx.hasPlayer()) return PlaceholderResult.invalid("No player!");
            if (arg == null) return PlaceholderResult.invalid("No placeholder!");

            String text = "%" + arg + "%";
            String textBefore;
            do {
                textBefore = text;
                for (String placeholder : PlaceholderManagerImpl.detectPlaceholders(text)) {
                    text = text.replace(placeholder, HavocTab.getInstance().getPlaceholderManager().findReplacement(
                            placeholder,
                            PlaceholderAPIHook.parsePlaceholders(placeholder, (ServerPlayer) ctx.player())
                    ));
                }
            } while (!textBefore.equals(text));

            return PlaceholderResult.value(text);
        });

        registerPlaceholder("placeholder", (ctx, arg) -> {
            if (arg == null) return PlaceholderResult.invalid("No placeholder!");

            TabPlayer player = ctx.hasPlayer() ? HavocTab.getInstance().getPlayer(ctx.player().getUUID()) : null;

            String placeholder = "%"+arg+"%";
            PlaceholderManagerImpl manager = HavocTab.getInstance().getPlaceholderManager();
            manager.addUsedPlaceholder(placeholder, manager);
            return PlaceholderResult.value(manager.getPlaceholder(placeholder).getLastValue(player));
        });
    }

    private void registerPlaceholder(String identifier, Placeholder.Handler<ServerPlaceholderContext, String> handler) {
        Placeholders.registerServer(Identifier.tryParse(ProjectVariables.PLUGIN_ID+":"+identifier), handler);
    }

    @Override
    public void unregisterExpansion() {}
}
