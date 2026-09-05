package dev.havoc.havoctab.shared.config.converter;

import lombok.NonNull;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.chat.TabTextColor;
import dev.havoc.havoctab.shared.chat.component.TabTextComponent;
import dev.havoc.havoctab.shared.config.file.ConfigurationFile;
import dev.havoc.havoctab.shared.config.file.ConfigurationSection;
import dev.havoc.havoctab.shared.placeholders.conditions.Condition;
import dev.havoc.havoctab.shared.placeholders.conditions.ConditionsSection;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Config converter that uses new config-version field.
 */
public class ModernConverter {

    /** Converters from one config version to another */
    private final Map<Integer, Consumer<ConfigurationFile>> converters = new HashMap<>();

    /**
     * Constructs new instance and initializes all converters.
     */
    public ModernConverter() {
        converters.put(0, config -> {
            Map<String, Object> components = new LinkedHashMap<>();
            components.put("minimessage-support", true);
            components.put("disable-shadow-for-heads", true);
            config.set("components", components);
        });
        converters.put(1, config -> {
            // Warn users
            HavocTab.getInstance().getPlatform().logWarn(new TabTextComponent("Please note that header/footer conversion may not be 100% accurate and will not convert" +
                    " per-group settings and per-user settings." +
                    " Review your config to make sure it is set up the way you want.", TabTextColor.RED));

            // Read old data
            ConfigurationSection headerFooter = config.getConfigurationSection("header-footer");
            boolean enabled = headerFooter.getBoolean("enabled", true);
            List<String> defaultHeader = headerFooter.getStringList("header", new ArrayList<>());
            List<String> defaultFooter = headerFooter.getStringList("footer", new ArrayList<>());
            String disableCondition = headerFooter.getString("disable-condition", "%world%=disabledworld");
            ConfigurationSection perWorld = headerFooter.getConfigurationSection("per-world");
            ConfigurationSection perServer = headerFooter.getConfigurationSection("per-server");

            // Write new data
            Map<String, Object> designs = new LinkedHashMap<>();

            for (Object world : perWorld.getKeys()) {
                ConfigurationSection worldSection = perWorld.getConfigurationSection(world.toString());
                Map<String, Object> design = new LinkedHashMap<>();
                design.put("display-condition", Arrays.stream(world.toString().split(";")).map(part -> {
                    if (part.endsWith("*")) {
                        return "%world%|-" + part.substring(0, part.length() - 1);
                    } else if (part.startsWith("*")) {
                        return "%world%-|" + part.substring(1);
                    } else {
                        return "%world%=" + part;
                    }
                }).collect(Collectors.joining("|")));
                List<String> header = worldSection.getStringList("header");
                if (header == null) header = new ArrayList<>();
                List<String> footer = worldSection.getStringList("footer");
                if (footer == null) footer = new ArrayList<>();
                design.put("header", header);
                design.put("footer", footer);
                designs.put("world-" + world, design);
            }

            for (Object server : perServer.getKeys()) {
                ConfigurationSection serverSection = perServer.getConfigurationSection(server.toString());
                Map<String, Object> design = new LinkedHashMap<>();
                design.put("display-condition", Arrays.stream(server.toString().split(";")).map(part -> {
                    if (part.endsWith("*")) {
                        return "%server%|-" + part.substring(0, part.length() - 1);
                    } else if (part.startsWith("*")) {
                        return "%server%-|" + part.substring(1);
                    } else {
                        return "%server%=" + part;
                    }
                }).collect(Collectors.joining("|")));
                List<String> header = serverSection.getStringList("header");
                if (header == null) header = new ArrayList<>();
                List<String> footer = serverSection.getStringList("footer");
                if (footer == null) footer = new ArrayList<>();
                design.put("header", header);
                design.put("footer", footer);
                designs.put("server-" + server, design);
            }

            Map<String, Object> defaultDesign = new LinkedHashMap<>();
            if (!disableCondition.isEmpty()) {
                ConditionsSection conditions = ConditionsSection.fromSection(config.getConfigurationSection("conditions"));
                ConditionsSection.ConditionDefinition namedCondition = conditions.getConditions().get(disableCondition);
                if (namedCondition != null) {
                    // Named condition
                    defaultDesign.put("display-condition", "%condition:" + namedCondition.getName() + "%=" + namedCondition.getNo());
                } else {
                    // Short format
                    defaultDesign.put("display-condition", new Condition(disableCondition).invert().toShortFormat());
                }
            }
            defaultDesign.put("header", defaultHeader);
            defaultDesign.put("footer", defaultFooter);
            designs.put("default", defaultDesign);

            Map<String, Object> newHeaderFooter = new LinkedHashMap<>();
            newHeaderFooter.put("enabled", enabled);
            newHeaderFooter.put("designs", designs);
            config.set("header-footer", newHeaderFooter);
        });
        converters.put(2, config -> {
            config.removeOption("scoreboard.use-numbers");
            config.removeOption("scoreboard.static-number");
        });
        converters.put(3, config -> config.set("proxy-support.channel-name-suffix", "custom"));
        converters.put(4, config -> {
            ConfigurationSection layout = config.getConfigurationSection("layout");
            ConfigurationSection layouts = layout.getConfigurationSection("layouts");
            for (Object layoutName : layouts.getKeys()) {
                ConfigurationSection singleLayout = layouts.getConfigurationSection(layoutName.toString());
                singleLayout.put("display-condition", singleLayout.getString("condition"));
                singleLayout.remove("condition");
                ConfigurationSection groups = singleLayout.getConfigurationSection("groups");
                for (Object groupName : groups.getKeys()) {
                    ConfigurationSection singleGroup = groups.getConfigurationSection(groupName.toString());
                    singleGroup.put("display-condition", singleGroup.getString("condition"));
                    singleGroup.remove("condition");
                }
            }
        });
        converters.put(5, config -> {
            config.getConfigurationSection("placeholders").put("locale", "en-US");
        });
        converters.put(6, config -> {
            config.getConfigurationSection("belowname-objective").put("view-distance", 10);
        });
        converters.put(7, config -> {
            // HavocTab: add client-side display settings section
            Map<String, Object> ranks = new LinkedHashMap<>();
            ranks.put("enabled", true);
            ranks.put("toggle-command", "/ranks");
            ranks.put("remember-toggle-choice", true);
            ranks.put("hidden-by-default", false);
            ranks.put("hide-tablist-prefix-suffix", true);
            ranks.put("hide-nametag-prefix-suffix", true);
            ranks.put("require-permission", false);

            Map<String, Object> belowName = new LinkedHashMap<>();
            belowName.put("enabled", true);
            belowName.put("toggle-command", "/belowname");
            belowName.put("remember-toggle-choice", true);
            belowName.put("hidden-by-default", false);
            belowName.put("require-permission", false);

            Map<String, Object> clientDisplay = new LinkedHashMap<>();
            clientDisplay.put("placeholder-value-on", "&aON");
            clientDisplay.put("placeholder-value-off", "&cOFF");
            clientDisplay.put("ranks", ranks);
            clientDisplay.put("belowname", belowName);

            config.set("client-display-settings", clientDisplay);
        });
        converters.put(8, config -> {
            // HavocTab: add chat section, disabled by default so it cannot fight
            // with whatever plugin currently formats chat.
            Map<String, Object> hover = new LinkedHashMap<>();
            hover.put("enabled", true);
            hover.put("lore", Arrays.asList(
                    "%chatprefix%%player%",
                    "&7&m----------",
                    "&f%player%",
                    "&7&m----------"));

            Map<String, Object> publicChat = new LinkedHashMap<>();
            publicChat.put("enabled", true);
            publicChat.put("toggle-command", "/publicchat");
            publicChat.put("remember-toggle-choice", true);
            publicChat.put("hidden-by-default", false);

            Map<String, Object> chatFormat = new LinkedHashMap<>();
            chatFormat.put("enabled", true);
            chatFormat.put("toggle-command", "/chatformat");
            chatFormat.put("remember-toggle-choice", true);
            chatFormat.put("hidden-by-default", false);

            Map<String, Object> block = new LinkedHashMap<>();
            block.put("enabled", true);
            block.put("block-commands", Arrays.asList("/block", "/ignore"));
            block.put("unblock-commands", Arrays.asList("/unblock", "/unignore"));
            block.put("list-commands", Arrays.asList("/blocklist", "/ignorelist"));
            block.put("max-blocked", 100);
            block.put("menu-type", "AUTO");
            block.put("block-private-messages", true);
            block.put("private-message-commands", Arrays.asList(
                    "msg", "tell", "w", "whisper", "pm", "m", "message", "emsg", "etell", "epm", "dm"));

            Map<String, Object> chat = new LinkedHashMap<>();
            chat.put("enabled", false);
            chat.put("format-enabled", true);
            chat.put("format", "&f%chatprefix%%player%&7: &f%message%");
            chat.put("plain-format", "&f%player%&7: &f%message%");
            chat.put("hover", hover);
            chat.put("click-command", "");
            chat.put("public-chat", publicChat);
            chat.put("chat-format", chatFormat);
            chat.put("block", block);

            config.set("chat", chat);
        });
    }

    /**
     * Converts config to the latest version.
     *
     * @param   config
     *          config file to convert
     */
    public void convert(@NonNull ConfigurationFile config) {
        int configVersion = config.getInt("config-version", 0);
        while (converters.containsKey(configVersion)) {
            HavocTab.getInstance().getPlatform().logInfo(new TabTextComponent("Performing configuration conversion from config version " + configVersion + " to " + (configVersion + 1), TabTextColor.YELLOW));
            converters.get(configVersion).accept(config);
            configVersion++;
            config.set("config-version", configVersion);
        }
    }
}
