package dev.havoc.havoctab.shared.features.clientdisplay;

import dev.havoc.havoctab.shared.config.file.ConfigurationSection;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Configuration of HavocTab's client-side display settings
 * ({@code client-display-settings} section of config.yml).
 */
@Getter
@RequiredArgsConstructor
public class ClientDisplayConfiguration {

    @NotNull private final ConfigurationSection section;

    /** Settings of the per-player rank (prefix/suffix) visibility toggle */
    @NotNull private final RankSettings ranks;

    /** Settings of the per-player belowname visibility toggle */
    @NotNull private final BelowNameSettings belowName;

    /** Text the status placeholders return when the feature is visible */
    @NotNull private final String valueOn;

    /** Text the status placeholders return when the feature is hidden */
    @NotNull private final String valueOff;

    /**
     * Loads the configuration from the given section. A completely missing section is
     * valid and results in every option using its default value.
     *
     * @param   section
     *          Section to load from
     * @return  Loaded configuration
     */
    @NotNull
    public static ClientDisplayConfiguration fromSection(@NotNull ConfigurationSection section) {
        section.checkForUnknownKey(Arrays.asList("ranks", "belowname", "placeholder-value-on", "placeholder-value-off"));
        return new ClientDisplayConfiguration(
                section,
                RankSettings.fromSection(section.getConfigurationSection("ranks")),
                BelowNameSettings.fromSection(section.getConfigurationSection("belowname")),
                section.getString("placeholder-value-on", "&aON"),
                section.getString("placeholder-value-off", "&cOFF")
        );
    }

    /**
     * Settings of the client-side rank visibility toggle.
     */
    @Getter
    @RequiredArgsConstructor
    public static class RankSettings {

        /** Whether the toggle is available at all */
        private final boolean enabled;

        /** Command players run to toggle rank visibility for themselves */
        @NotNull private final String toggleCommand;

        /** Whether the choice should be saved to playerdata.yml and restored on join */
        private final boolean rememberToggleChoice;

        /** Whether ranks should start hidden for players who never toggled */
        private final boolean hiddenByDefault;

        /** Whether hiding ranks also removes tablist prefixes/suffixes */
        private final boolean hideTablist;

        /** Whether hiding ranks also removes nametag (above head) prefixes/suffixes */
        private final boolean hideNameTags;

        /** Whether the toggle command requires a permission node */
        private final boolean requirePermission;

        /**
         * Loads settings from the given section.
         *
         * @param   section
         *          Section to load from
         * @return  Loaded settings
         */
        @NotNull
        public static RankSettings fromSection(@NotNull ConfigurationSection section) {
            section.checkForUnknownKey(Arrays.asList("enabled", "toggle-command", "remember-toggle-choice",
                    "hidden-by-default", "hide-tablist-prefix-suffix", "hide-nametag-prefix-suffix", "require-permission"));
            return new RankSettings(
                    section.getBoolean("enabled", true),
                    section.getString("toggle-command", "/ranks"),
                    section.getBoolean("remember-toggle-choice", true),
                    section.getBoolean("hidden-by-default", false),
                    section.getBoolean("hide-tablist-prefix-suffix", true),
                    section.getBoolean("hide-nametag-prefix-suffix", true),
                    section.getBoolean("require-permission", false)
            );
        }
    }

    /**
     * Settings of the client-side belowname visibility toggle.
     */
    @Getter
    @RequiredArgsConstructor
    public static class BelowNameSettings {

        /** Whether the toggle is available at all */
        private final boolean enabled;

        /** Command players run to toggle belowname visibility for themselves */
        @NotNull private final String toggleCommand;

        /** Whether the choice should be saved to playerdata.yml and restored on join */
        private final boolean rememberToggleChoice;

        /** Whether belowname should start hidden for players who never toggled */
        private final boolean hiddenByDefault;

        /** Whether the toggle command requires a permission node */
        private final boolean requirePermission;

        /**
         * Loads settings from the given section.
         *
         * @param   section
         *          Section to load from
         * @return  Loaded settings
         */
        @NotNull
        public static BelowNameSettings fromSection(@NotNull ConfigurationSection section) {
            section.checkForUnknownKey(Arrays.asList("enabled", "toggle-command", "remember-toggle-choice",
                    "hidden-by-default", "require-permission"));
            return new BelowNameSettings(
                    section.getBoolean("enabled", true),
                    section.getString("toggle-command", "/belowname"),
                    section.getBoolean("remember-toggle-choice", true),
                    section.getBoolean("hidden-by-default", false),
                    section.getBoolean("require-permission", false)
            );
        }
    }
}
