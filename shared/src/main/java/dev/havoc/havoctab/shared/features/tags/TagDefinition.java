package dev.havoc.havoctab.shared.features.tags;

import dev.havoc.havoctab.shared.config.file.ConfigurationSection;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * One tag defined in tags.yml.
 */
@Getter
@RequiredArgsConstructor
public class TagDefinition {

    /** Key in tags.yml, used in commands and in the permission node */
    @NotNull private final String id;

    /**
     * The tag itself as it appears in chat, tablist or wherever the placeholder is used.
     * <p>
     * Supports everything HavocTab's text parser does: legacy {@code &3} codes, hex
     * {@code &#f40d0d}, gradients, MiniMessage tags and custom fonts via
     * {@code <font:namespace:id>text</font>}.
     */
    @NotNull private final String display;

    /** Name shown on the GUI item */
    @NotNull private final String name;

    /** Extra lore lines shown above the status block on the GUI item */
    @NotNull private final List<String> lore;

    /** Material of the GUI item */
    @NotNull private final String material;

    /** Permission required to own this tag */
    @NotNull private final String permission;

    /** Whether this tag appears in the Limited Tags sub-menu instead of the main list */
    private final boolean limited;

    /** Sort weight, lower shows first */
    private final int weight;

    /**
     * Loads one tag from its configuration section.
     *
     * @param   id
     *          Key of the tag in tags.yml
     * @param   section
     *          Section to load from
     * @return  Loaded tag
     */
    @NotNull
    public static TagDefinition fromSection(@NotNull String id, @NotNull ConfigurationSection section) {
        section.checkForUnknownKey(Arrays.asList("display", "name", "lore", "material", "permission",
                "limited", "weight"));
        return new TagDefinition(
                id,
                section.getString("display", "&7[" + id + "]"),
                section.getString("name", "&f" + id),
                section.getStringList("lore", Collections.emptyList()),
                section.getString("material", "NAME_TAG"),
                section.getString("permission", "havoctab.tag." + id),
                section.getBoolean("limited", false),
                section.getInt("weight", 100)
        );
    }
}
