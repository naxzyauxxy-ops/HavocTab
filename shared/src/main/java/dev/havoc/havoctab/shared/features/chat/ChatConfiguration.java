package dev.havoc.havoctab.shared.features.chat;

import dev.havoc.havoctab.shared.config.file.ConfigurationSection;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Configuration of HavocTab's chat feature ({@code chat} section of config.yml).
 */
@Getter
@RequiredArgsConstructor
public class ChatConfiguration {

    @NotNull private final ConfigurationSection section;

    /** Whether HavocTab formats chat at all. When false, only the toggles and blocking work. */
    private final boolean formatEnabled;

    /** Format used for viewers who have the chat format enabled */
    @NotNull private final String format;

    /** Format used for viewers who turned the chat format off with the toggle command */
    @NotNull private final String plainFormat;

    /** Whether the hover card is attached to the formatted message */
    private final boolean hoverEnabled;

    /** Lines of the hover card, parsed for the message sender */
    @NotNull private final List<String> hoverLore;

    /** Command run when the message is clicked, empty to disable */
    @NotNull private final String clickCommand;

    /** Settings of the public chat visibility toggle */
    @NotNull private final ToggleSettings publicChat;

    /** Settings of the chat format visibility toggle */
    @NotNull private final ToggleSettings chatFormat;

    /** Settings of the block list */
    @NotNull private final BlockSettings block;

    /**
     * Loads the configuration from the given section. A missing section is valid and
     * results in every option using its default.
     *
     * @param   section
     *          Section to load from
     * @return  Loaded configuration
     */
    @NotNull
    public static ChatConfiguration fromSection(@NotNull ConfigurationSection section) {
        section.checkForUnknownKey(Arrays.asList("enabled", "format-enabled", "format", "plain-format",
                "hover", "click-command", "public-chat", "chat-format", "block"));

        ConfigurationSection hover = section.getConfigurationSection("hover");
        hover.checkForUnknownKey(Arrays.asList("enabled", "lore"));

        return new ChatConfiguration(
                section,
                section.getBoolean("format-enabled", true),
                section.getString("format", "&f%chatprefix%%player%&#f40d0d: &f%message%"),
                section.getString("plain-format", "&f%player%&7: &f%message%"),
                hover.getBoolean("enabled", true),
                hover.getStringList("lore", Collections.emptyList()),
                section.getString("click-command", ""),
                ToggleSettings.fromSection(section.getConfigurationSection("public-chat"), "/publicchat"),
                ToggleSettings.fromSection(section.getConfigurationSection("chat-format"), "/chatformat"),
                BlockSettings.fromSection(section.getConfigurationSection("block"))
        );
    }

    /**
     * Settings shared by the two simple on/off chat toggles.
     */
    @Getter
    @RequiredArgsConstructor
    public static class ToggleSettings {

        /** Whether the toggle command is registered */
        private final boolean enabled;

        /** Command players run to flip the setting */
        @NotNull private final String toggleCommand;

        /** Whether the choice is saved and restored on rejoin */
        private final boolean rememberToggleChoice;

        /** Whether the feature starts hidden/disabled for players who never toggled */
        private final boolean hiddenByDefault;

        /**
         * Loads toggle settings from the given section.
         *
         * @param   section
         *          Section to load from
         * @param   defaultCommand
         *          Command to use when none is configured
         * @return  Loaded settings
         */
        @NotNull
        public static ToggleSettings fromSection(@NotNull ConfigurationSection section, @NotNull String defaultCommand) {
            section.checkForUnknownKey(Arrays.asList("enabled", "toggle-command", "remember-toggle-choice", "hidden-by-default"));
            return new ToggleSettings(
                    section.getBoolean("enabled", true),
                    section.getString("toggle-command", defaultCommand),
                    section.getBoolean("remember-toggle-choice", true),
                    section.getBoolean("hidden-by-default", false)
            );
        }
    }

    /**
     * Settings of the per-player block list.
     */
    @Getter
    @RequiredArgsConstructor
    public static class BlockSettings {

        /** Whether blocking is available */
        private final boolean enabled;

        /** Commands that block a player, first entry is the primary one */
        @NotNull private final List<String> blockCommands;

        /** Commands that unblock a player */
        @NotNull private final List<String> unblockCommands;

        /** Commands that open the block list menu */
        @NotNull private final List<String> listCommands;

        /** Maximum number of players one viewer may block, 0 for unlimited */
        private final int maxBlocked;

        /**
         * Which block list menu to show: AUTO (native dialog for Java players, chest GUI
         * for Bedrock), DIALOG, GUI or CHAT.
         */
        @NotNull private final String menuType;

        /** Whether blocking someone also stops their private messages */
        private final boolean blockPrivateMessages;

        /**
         * Command names (without slash) treated as private message commands. The first
         * argument of these is checked against the block list.
         */
        @NotNull private final List<String> privateMessageCommands;

        /**
         * Loads block settings from the given section.
         *
         * @param   section
         *          Section to load from
         * @return  Loaded settings
         */
        @NotNull
        public static BlockSettings fromSection(@NotNull ConfigurationSection section) {
            section.checkForUnknownKey(Arrays.asList("enabled", "block-commands", "unblock-commands",
                    "list-commands", "max-blocked", "menu-type", "block-private-messages", "private-message-commands"));
            return new BlockSettings(
                    section.getBoolean("enabled", true),
                    section.getStringList("block-commands", Arrays.asList("/block", "/ignore")),
                    section.getStringList("unblock-commands", Arrays.asList("/unblock", "/unignore")),
                    section.getStringList("list-commands", Arrays.asList("/blocklist", "/ignorelist")),
                    section.getInt("max-blocked", 100),
                    section.getString("menu-type", "AUTO"),
                    section.getBoolean("block-private-messages", true),
                    section.getStringList("private-message-commands", Arrays.asList(
                            "msg", "tell", "w", "whisper", "pm", "m", "message",
                            "emsg", "etell", "epm", "dm"))
            );
        }
    }
}
