package dev.havoc.havoctab.platforms.bukkit.features;

import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import dev.havoc.havoctab.api.placeholder.Placeholder;
import dev.havoc.havoctab.shared.ProjectVariables;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.features.PlaceholderManagerImpl;
import dev.havoc.havoctab.shared.placeholders.expansion.TabExpansion;
import dev.havoc.havoctab.shared.placeholders.types.RelationalPlaceholderImpl;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import dev.havoc.havoctab.shared.util.ReflectionUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * HavocTab's expansion for PlaceholderAPI
 */
@Getter
public class BukkitTabExpansion extends PlaceholderExpansion implements TabExpansion, Relational {

    /** List of all placeholders offered by the plugin for command suggestions */
    @NotNull
    private final List<String> placeholders = Collections.unmodifiableList(Arrays.asList(
            "%tab_tabprefix%",
            "%tab_tabsuffix%",
            "%tab_tagprefix%",
            "%tab_tagsuffix%",
            "%tab_customtabname%",
            "%tab_tabprefix_raw%",
            "%tab_tabsuffix_raw%",
            "%tab_tagprefix_raw%",
            "%tab_tagsuffix_raw%",
            "%tab_customtabname_raw%",
            "%tab_scoreboard_name%",
            "%tab_scoreboard_visible%",
            "%tab_bossbar_visible%",
            "%tab_nametag_visibility%",
            "%tab_replace_<placeholder>%",
            "%tab_placeholder_<placeholder>%"
    ));

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return ProjectVariables.PLUGIN_AUTHOR;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return ProjectVariables.PLUGIN_ID;
    }

    @Override
    @NotNull
    public String getVersion() {
        return ProjectVariables.PLUGIN_VERSION;
    }

    @Override
    @NotNull
    public List<String> getPlaceholders() {
        return placeholders;
    }

    @Override
    @Nullable
    public String onPlaceholderRequest(@Nullable Player player, @NotNull String identifier) {
        if (identifier.startsWith("replace_")) {
            String text = "%" + identifier.substring(8) + "%";
            String textBefore;
            do {
                textBefore = text;
                for (String placeholder : PlaceholderManagerImpl.detectPlaceholders(text)) {
                    text = text.replace(placeholder, HavocTab.getInstance().getPlaceholderManager().findReplacement(placeholder,
                            PlaceholderAPI.setPlaceholders(player, placeholder)));
                }
            } while (!textBefore.equals(text));
            return text;
        }
        if (player == null) return "<Player cannot be null>";
        TabPlayer p = HavocTab.getInstance().getPlayer(player.getUniqueId());
        if (p == null) return "<Player is not loaded>";
        if (identifier.startsWith("placeholder_")) {
            String requestedPlaceholder = "%" + identifier.substring("placeholder_".length()) + "%";
            PlaceholderManagerImpl pm = HavocTab.getInstance().getPlaceholderManager();
            pm.addUsedPlaceholder(requestedPlaceholder, pm);
            return pm.getPlaceholder(requestedPlaceholder).parse(p);
        }
        return p.expansionData.getValue(identifier);
    }

    @Override
    public String onPlaceholderRequest(@NotNull Player viewer, @NotNull Player target, @NotNull String identifier) {
        if (identifier.startsWith("replace_")) {
            String text = "%" + identifier.substring(8) + "%";
            String textBefore;
            do {
                textBefore = text;
                for (String placeholder : PlaceholderManagerImpl.detectPlaceholders(text)) {
                    text = text.replace(placeholder, HavocTab.getInstance().getPlaceholderManager().findReplacement(placeholder,
                            PlaceholderAPI.setRelationalPlaceholders(viewer, target, placeholder)));
                }
            } while (!textBefore.equals(text));
            return text;
        }
        if (identifier.startsWith("placeholder_")) {
            String requestedPlaceholder = "%" + identifier.substring("placeholder_".length()) + "%";
            PlaceholderManagerImpl pm = HavocTab.getInstance().getPlaceholderManager();
            pm.addUsedPlaceholder(requestedPlaceholder, pm);
            Placeholder placeholder = pm.getPlaceholder(requestedPlaceholder);
            if (placeholder instanceof RelationalPlaceholderImpl) {
                RelationalPlaceholderImpl rel = (RelationalPlaceholderImpl) placeholder;
                TabPlayer v = HavocTab.getInstance().getPlayer(viewer.getUniqueId());
                TabPlayer t = HavocTab.getInstance().getPlayer(target.getUniqueId());
                if (v == null || t == null) return "<Player is not loaded>";
                return rel.getLastValue(v, t);
            } else {
                return "<Not a relational placeholder: " + requestedPlaceholder + ">";
            }
        }
        return "<Unknown identifier: \"" + identifier + "\">";
    }

    @Override
    @SuppressWarnings({"UnstableApiUsage", "deprecation"})
    public void unregisterExpansion() {
        if (ReflectionUtils.methodExists(PlaceholderExpansion.class, "unregister")) {
            // Added in 2.10.7 (Jul 28, 2020)
            unregister();
        } else {
            PlaceholderAPI.unregisterExpansion(this);
        }
    }
}
