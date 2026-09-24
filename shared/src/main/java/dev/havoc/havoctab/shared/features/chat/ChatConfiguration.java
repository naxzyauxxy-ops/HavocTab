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

    /** Settings of @mentions */
    @NotNull private final MentionSettings mentions;

    /** Settings of the chat filter */
    @NotNull private final FilterSettings filter;

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
                "hover", "click-command", "public-chat", "chat-format", "block", "mentions", "filter"));

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
                BlockSettings.fromSection(section.getConfigurationSection("block")),
                MentionSettings.fromSection(section.getConfigurationSection("mentions")),
                FilterSettings.fromSection(section.getConfigurationSection("filter"))
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

    /**
     * Settings of @mentions - highlighting a player's name in chat and pinging them.
     */
    @Getter
    @RequiredArgsConstructor
    public static class MentionSettings {

        /** Whether mentions are detected at all */
        private final boolean enabled;

        /** If true, only "@Name" counts as a mention; if false, a bare "Name" does too */
        private final boolean requireAtSymbol;

        /** How the mentioned name is rendered inside the message, %player% is the name */
        @NotNull private final String highlight;

        /** Action bar shown to the mentioned player, %player% is the sender */
        @NotNull private final String actionBar;

        /** Whether a sound plays for the mentioned player */
        private final boolean soundEnabled;

        /** Sound to play, either a Bukkit enum name or a namespaced sound key */
        @NotNull private final String soundName;

        /** Sound volume */
        private final float soundVolume;

        /** Sound pitch */
        private final float soundPitch;

        /** Settings of the per-player "notify me about mentions" toggle */
        @NotNull private final ToggleSettings toggle;

        /**
         * Loads mention settings from the given section.
         *
         * @param   section
         *          Section to load from
         * @return  Loaded settings
         */
        @NotNull
        public static MentionSettings fromSection(@NotNull ConfigurationSection section) {
            // "allow-self-mention" is still accepted so older configs do not warn about an
            // unknown key, but it is no longer read - self-mentions are always ignored.
            section.checkForUnknownKey(Arrays.asList("enabled", "require-at-symbol", "highlight",
                    "action-bar", "allow-self-mention", "sound", "toggle"));

            ConfigurationSection sound = section.getConfigurationSection("sound");
            sound.checkForUnknownKey(Arrays.asList("enabled", "name", "volume", "pitch"));

            return new MentionSettings(
                    section.getBoolean("enabled", true),
                    section.getBoolean("require-at-symbol", false),
                    section.getString("highlight", "&#f40d0d&l%player%"),
                    section.getString("action-bar", "&fYou have been mentioned by &#f40d0d%player%"),
                    sound.getBoolean("enabled", true),
                    sound.getString("name", "ENTITY_EXPERIENCE_ORB_PICKUP"),
                    sound.getNumber("volume", 1.0f).floatValue(),
                    sound.getNumber("pitch", 1.2f).floatValue(),
                    ToggleSettings.fromSection(section.getConfigurationSection("toggle"), "/mentions")
            );
        }
    }

    /**
     * Settings of the chat filter.
     */
    @Getter
    @RequiredArgsConstructor
    public static class FilterSettings {

        /** Whether any filtering happens */
        private final boolean enabled;

        /** Permission that skips every check */
        @NotNull private final String bypassPermission;

        /** Rate limiting, repeat, caps and character run settings */
        @NotNull private final SpamSettings spam;

        /** Blocked word settings */
        @NotNull private final WordSettings blockedWords;

        /** Advertising settings */
        @NotNull private final AdvertisingSettings advertising;

        /**
         * Loads filter settings from the given section.
         *
         * @param   section
         *          Section to load from
         * @return  Loaded settings
         */
        @NotNull
        public static FilterSettings fromSection(@NotNull ConfigurationSection section) {
            section.checkForUnknownKey(Arrays.asList("enabled", "bypass-permission", "spam",
                    "blocked-words", "advertising"));
            return new FilterSettings(
                    section.getBoolean("enabled", true),
                    section.getString("bypass-permission", "havoctab.chat.bypassfilter"),
                    SpamSettings.fromSection(section.getConfigurationSection("spam")),
                    WordSettings.fromSection(section.getConfigurationSection("blocked-words")),
                    AdvertisingSettings.fromSection(section.getConfigurationSection("advertising"))
            );
        }
    }

    /**
     * Anti-spam settings.
     */
    @Getter
    @RequiredArgsConstructor
    public static class SpamSettings {

        /** Whether spam checks run */
        private final boolean enabled;

        /** Minimum milliseconds between two messages, 0 to disable */
        private final int cooldownMilliseconds;

        /** Whether repeating a recent message is blocked */
        private final boolean blockRepeats;

        /** How many recent messages are remembered per player */
        private final int repeatHistory;

        /** Percentage of letters allowed to be uppercase before the message is lowercased */
        private final int maxCapsPercent;

        /** Minimum length before the caps rule applies */
        private final int capsMinLength;

        /** Runs of the same character longer than this are collapsed, 0 to disable */
        private final int maxRepeatedCharacters;

        /**
         * Loads spam settings from the given section.
         *
         * @param   section
         *          Section to load from
         * @return  Loaded settings
         */
        @NotNull
        public static SpamSettings fromSection(@NotNull ConfigurationSection section) {
            section.checkForUnknownKey(Arrays.asList("enabled", "cooldown-milliseconds", "block-repeats",
                    "repeat-history", "max-caps-percent", "caps-min-length", "max-repeated-characters"));
            return new SpamSettings(
                    section.getBoolean("enabled", true),
                    section.getInt("cooldown-milliseconds", 1500),
                    section.getBoolean("block-repeats", true),
                    section.getInt("repeat-history", 3),
                    section.getInt("max-caps-percent", 70),
                    section.getInt("caps-min-length", 8),
                    section.getInt("max-repeated-characters", 4)
            );
        }
    }

    /**
     * Blocked word settings.
     */
    @Getter
    @RequiredArgsConstructor
    public static class WordSettings {

        /** Whether blocked word checks run */
        private final boolean enabled;

        /** BLOCK to refuse the message, CENSOR to replace the word */
        @NotNull private final String action;

        /** What a censored word is replaced with */
        @NotNull private final String censorReplacement;

        /** Words to block */
        @NotNull private final List<String> words;

        /** Phrases removed before matching, to stop false positives */
        @NotNull private final List<String> allowedPhrases;

        /**
         * Loads blocked word settings from the given section.
         *
         * @param   section
         *          Section to load from
         * @return  Loaded settings
         */
        @NotNull
        public static WordSettings fromSection(@NotNull ConfigurationSection section) {
            section.checkForUnknownKey(Arrays.asList("enabled", "action", "censor-replacement",
                    "words", "allowed-phrases"));
            return new WordSettings(
                    section.getBoolean("enabled", true),
                    section.getString("action", "BLOCK"),
                    section.getString("censor-replacement", "***"),
                    section.getStringList("words", Collections.emptyList()),
                    section.getStringList("allowed-phrases", Collections.emptyList())
            );
        }
    }

    /**
     * Anti-advertising settings.
     */
    @Getter
    @RequiredArgsConstructor
    public static class AdvertisingSettings {

        /** Whether advertising checks run */
        private final boolean enabled;

        /** BLOCK to refuse the message, CENSOR to let it through unchanged */
        @NotNull private final String action;

        /** Whether IPv4 addresses are blocked */
        private final boolean blockIps;

        /** Whether domain names are blocked */
        private final boolean blockDomains;

        /** Whether Discord invites are blocked */
        private final boolean blockDiscordInvites;

        /** Regular expression matching a domain, empty to disable */
        @NotNull private final String domainPattern;

        /** Anything containing one of these is allowed through */
        @NotNull private final List<String> allowed;

        /**
         * Loads advertising settings from the given section.
         *
         * @param   section
         *          Section to load from
         * @return  Loaded settings
         */
        @NotNull
        public static AdvertisingSettings fromSection(@NotNull ConfigurationSection section) {
            section.checkForUnknownKey(Arrays.asList("enabled", "action", "block-ips", "block-domains",
                    "block-discord-invites", "domain-pattern", "allowed"));
            return new AdvertisingSettings(
                    section.getBoolean("enabled", true),
                    section.getString("action", "BLOCK"),
                    section.getBoolean("block-ips", true),
                    section.getBoolean("block-domains", true),
                    section.getBoolean("block-discord-invites", true),
                    section.getString("domain-pattern",
                            "\\b[a-z0-9][a-z0-9-]*\\.(?:com|net|org|gg|xyz|io|me|tv|club|store|shop|fun|online|site|pro|co|uk|eu|ru|de|nl|ca|au|info|biz|link|live|cc|to|ly|sh|dev|app|mc|us)\\b"),
                    section.getStringList("allowed", Collections.emptyList())
            );
        }
    }
}
