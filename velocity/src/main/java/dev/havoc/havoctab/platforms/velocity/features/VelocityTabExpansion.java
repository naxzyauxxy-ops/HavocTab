package dev.havoc.havoctab.platforms.velocity.features;

import com.velocitypowered.api.proxy.Player;
import io.github.miniplaceholders.api.Expansion;
import lombok.Getter;
import dev.havoc.havoctab.api.placeholder.Placeholder;
import dev.havoc.havoctab.platforms.velocity.hook.MiniPlaceholdersHook;
import dev.havoc.havoctab.shared.ProjectVariables;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.features.PlaceholderManagerImpl;
import dev.havoc.havoctab.shared.placeholders.expansion.TabExpansion;
import dev.havoc.havoctab.shared.placeholders.types.RelationalPlaceholderImpl;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import dev.havoc.havoctab.shared.util.cache.StringToComponentCache;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * HavocTab's expansion for MiniPlaceholders.
 */
@Getter
public class VelocityTabExpansion implements TabExpansion {

    private static final StringToComponentCache COLORED_TEXT_CACHE = new StringToComponentCache("MiniPlaceholders expansion", 1000);

    /** List of all placeholders offered by the plugin for command suggestions */
    @NotNull
    private final List<String> placeholders = Collections.unmodifiableList(Arrays.asList(
            "<" + ProjectVariables.PLUGIN_ID + ":tabprefix>",
            "<" + ProjectVariables.PLUGIN_ID + ":tabsuffix>",
            "<" + ProjectVariables.PLUGIN_ID + ":tagprefix>",
            "<" + ProjectVariables.PLUGIN_ID + ":tagsuffix>",
            "<" + ProjectVariables.PLUGIN_ID + ":customtabname>",
            "<" + ProjectVariables.PLUGIN_ID + ":tabprefix_raw>",
            "<" + ProjectVariables.PLUGIN_ID + ":tabsuffix_raw>",
            "<" + ProjectVariables.PLUGIN_ID + ":tagprefix_raw>",
            "<" + ProjectVariables.PLUGIN_ID + ":tagsuffix_raw>",
            "<" + ProjectVariables.PLUGIN_ID + ":customtabname_raw>",
            "<" + ProjectVariables.PLUGIN_ID + ":scoreboard_name>",
            "<" + ProjectVariables.PLUGIN_ID + ":scoreboard_visible>",
            "<" + ProjectVariables.PLUGIN_ID + ":bossbar_visible>",
            "<" + ProjectVariables.PLUGIN_ID + ":nametag_visibility>",
            "<" + ProjectVariables.PLUGIN_ID + ":replace:placeholder>",
            "<" + ProjectVariables.PLUGIN_ID + ":placeholder:placeholder>"
    ));

    /** Registered expansion instance */
    @NotNull
    private final Expansion expansion;

    /**
     * Constructs new instance and registers placeholders into MiniPlaceholders.
     */
    public VelocityTabExpansion() {
        Expansion.Builder builder = Expansion.builder(ProjectVariables.PLUGIN_ID)
                .version(ProjectVariables.PLUGIN_VERSION)
                .author(ProjectVariables.PLUGIN_AUTHOR);

        List<String> internalPlaceholders = Arrays.asList(
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
        for (String placeholder : internalPlaceholders) {
            builder.audiencePlaceholder(Player.class, placeholder, (player, queue, ctx) ->
                    createTag(getExpansionValue(player, placeholder))
            );
        }

        builder.audiencePlaceholder(Player.class, "replace", (player, queue, ctx) -> {
            if (!queue.hasNext()) {
                return Tag.selfClosingInserting(Component.text("<No placeholder>"));
            }
            String arg = queue.pop().value();
            String resolved = MiniPlaceholdersHook.parseText(toMiniPlaceholder(arg), player);
            return createTag(HavocTab.getInstance().getPlaceholderManager().findReplacement(toTabIdentifier(arg), resolved));
        });

        builder.audiencePlaceholder(Player.class, "placeholder", (player, queue, ctx) -> {
            if (!queue.hasNext()) {
                return Tag.selfClosingInserting(Component.text("<No placeholder>"));
            }
            String requestedPlaceholder = toTabIdentifier(queue.pop().value());
            PlaceholderManagerImpl pm = HavocTab.getInstance().getPlaceholderManager();
            pm.addUsedPlaceholder(requestedPlaceholder, pm);
            TabPlayer tabPlayer = HavocTab.getInstance().getPlayer(player.getUniqueId());
            if (tabPlayer == null) {
                return Tag.selfClosingInserting(Component.text("<Player is not loaded>"));
            }
            return createTag(pm.getPlaceholder(requestedPlaceholder).parse(tabPlayer));
        });

        builder.relationalPlaceholder("replace", (viewer, target, queue, ctx) -> {
            if (!queue.hasNext()) {
                return Tag.selfClosingInserting(Component.text("<No placeholder>"));
            }
            if (!(viewer instanceof Player) || !(target instanceof Player)) {
                return Tag.selfClosingInserting(Component.text("<Players must be online>"));
            }
            String arg = queue.pop().value();
            String resolved = MiniPlaceholdersHook.parseRelational(toMiniPlaceholder(arg), (Player) viewer, (Player) target);
            return createTag(HavocTab.getInstance().getPlaceholderManager().findReplacement(toTabIdentifier(arg), resolved));
        });

        builder.relationalPlaceholder("placeholder", (viewer, target, queue, ctx) -> {
            if (!queue.hasNext()) {
                return Tag.selfClosingInserting(Component.text("<No placeholder>"));
            }
            if (!(viewer instanceof Player) || !(target instanceof Player)) {
                return Tag.selfClosingInserting(Component.text("<Players must be online>"));
            }
            String requestedPlaceholder = toTabIdentifier(queue.pop().value());
            PlaceholderManagerImpl pm = HavocTab.getInstance().getPlaceholderManager();
            pm.addUsedPlaceholder(requestedPlaceholder, pm);
            Placeholder placeholder = pm.getPlaceholder(requestedPlaceholder);
            if (placeholder instanceof RelationalPlaceholderImpl rel) {
                TabPlayer v = HavocTab.getInstance().getPlayer(((Player) viewer).getUniqueId());
                TabPlayer t = HavocTab.getInstance().getPlayer(((Player) target).getUniqueId());
                if (v == null || t == null) {
                    return Tag.selfClosingInserting(Component.text("<Player is not loaded>"));
                }
                return createTag(rel.getLastValue(v, t));
            }
            return Tag.selfClosingInserting(Component.text("<Not a relational placeholder: " + requestedPlaceholder + ">"));
        });

        expansion = builder.build();
        expansion.register();
    }

    @NotNull
    private static String toMiniPlaceholder(@NotNull String arg) {
        return arg.startsWith("<") ? arg : "<" + arg + ">";
    }

    @NotNull
    private static String toTabIdentifier(@NotNull String arg) {
        String name = arg.startsWith("<") && arg.endsWith(">") ? arg.substring(1, arg.length() - 1) : arg;
        return "%" + name + "%";
    }

    @NotNull
    private static Tag createTag(@NotNull String value) {
        return Tag.selfClosingInserting(COLORED_TEXT_CACHE.get(value).toAdventure());
    }

    @NotNull
    private static String getExpansionValue(@NotNull Player player, @NotNull String identifier) {
        TabPlayer tabPlayer = HavocTab.getInstance().getPlayer(player.getUniqueId());
        if (tabPlayer == null) return "<Player is not loaded>";
        String value = tabPlayer.expansionData.getValue(identifier);
        return value == null ? "" : value;
    }

    @Override
    public void unregisterExpansion() {
        expansion.unregister();
    }
}
