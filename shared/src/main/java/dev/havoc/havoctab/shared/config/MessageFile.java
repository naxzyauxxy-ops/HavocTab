package dev.havoc.havoctab.shared.config;

import lombok.Getter;
import lombok.NonNull;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.config.file.YamlConfigurationFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class MessageFile extends YamlConfigurationFile {

    private final String bossBarNotEnabled = getString("bossbar-feature-not-enabled", "&cThis command requires the bossbar feature to be enabled.");
    private final String bossBarAnnounceCommandUsage = getString("bossbar-announce-command-usage", "Usage: /tab bossbar announce <bar name> <length>");
    private final String bossBarAlreadyAnnounced = getString("bossbar-already-announced", "&cThis bossbar is already being announced");
    private final String parseCommandUsage = getString("parse-command-usage", "Usage: /tab parse <player> <placeholder>");
    private final String sendCommandUsage = getString("send-command-usage", "Usage: /tab send <type> <player> <bar name> <length>\nCurrently supported types: &lbar");
    private final String sendBarCommandUsage = getString("send-bar-command-usage", "Usage: /tab send bar <player> <bar name> <length>");
    private final String teamFeatureRequired = getString("team-feature-required", "This command requires scoreboard teams feature enabled");
    private final String collisionCommandUsage = getString("collision-command-usage", "Usage: /tab setcollision <player> <true/false>");
    private final String noPermission = getString("no-permission", "&cI'm sorry, but you do not have permission to perform this command. Please contact the server administrators if you believe that this is in error.");
    private final String commandOnlyFromGame = getString("command-only-from-game", "&cThis command must be ran from the game");
    private final String scoreboardFeatureNotEnabled = getString("scoreboard-feature-not-enabled", "&4This command requires the scoreboard feature to be enabled.");
    private final String scoreboardAnnounceCommandUsage = getString("scoreboard-announce-command-usage", "Usage: /tab scoreboard announce <scoreboard name> <length>");
    private final String reloadSuccess = getString("reload-success", "&3[HavocTab] Successfully reloaded");
    private final String reloadFailBrokenFile = getString("reload-fail-file", "&3[HavocTab] &4Failed to reload, file %file% has broken syntax. Check console for more info.");
    private final String scoreboardOn = getString("scoreboard-toggle-on", "&2Scoreboard enabled");
    private final String scoreboardOff = getString("scoreboard-toggle-off", "&7Scoreboard disabled");
    private final String bossBarOn = getString("bossbar-toggle-on", "&2Bossbar is now visible");
    private final String bossBarOff = getString("bossbar-toggle-off", "&7Bossbar is no longer visible. Magic!");
    private final String ranksToggleOn = getString("ranks-toggle-on", "&aRanks are now &aON &7- you can see rank prefixes again.");
    private final String ranksToggleOff = getString("ranks-toggle-off", "&cRanks are now &cOFF &7- only you stopped seeing them.");
    private final String belowNameToggleOn = getString("belowname-toggle-on", "&aBelowname is now &aON &7- you can see the text under nametags again.");
    private final String belowNameToggleOff = getString("belowname-toggle-off", "&cBelowname is now &cOFF &7- only you stopped seeing it.");
    private final String publicChatOn = getString("publicchat-toggle-on", "&aPublic chat is now &aON&7.");
    private final String publicChatOff = getString("publicchat-toggle-off", "&cPublic chat is now &cOFF &7- you will not see public messages.");
    private final String chatFormatOn = getString("chatformat-toggle-on", "&aChat format is now &aON&7.");
    private final String chatFormatOff = getString("chatformat-toggle-off", "&cChat format is now &cOFF &7- you will see plain chat.");
    private final String tagsNoneEquipped = getString("tags-none-equipped", "&7None");
    private final String tagsSearchPrompt = getString("tags-search-prompt", "&7Type a tag name in chat, or &fcancel &7to go back.");
    private final String tagsStatsName = getString("tags-stats-name", "&e&lTAG STATISTICS");
    private final String tagsLimitedName = getString("tags-limited-name", "&d&lLIMITED TAGS");
    private final String tagsBackName = getString("tags-back-name", "&7&lBACK");
    private final String tagsPreviousName = getString("tags-previous-name", "&c&lPREVIOUS PAGE");
    private final String tagsNextName = getString("tags-next-name", "&a&lNEXT PAGE");
    private final String tagsClearName = getString("tags-clear-name", "&c&lCLEAR TAG");
    private final String tagsSearchName = getString("tags-search-name", "&6&lSEARCH TAGS");
    private final String tagsFavouritesName = getString("tags-favourites-name", "&e&lFAVOURITES");
    private final String tagCleared = getString("tag-cleared", "&7Tag removed");
    private final String chatBlockSelf = getString("chat-block-self", "&cYou cannot block yourself.");
    private final String filterRepeat = getString("filter-repeat", "&cPlease do not repeat the same message.");
    private final String filterBlockedWord = getString("filter-blocked-word", "&cYour message contains language that is not allowed here.");
    private final String filterAdvertising = getString("filter-advertising", "&cAdvertising other servers or links is not allowed.");
    private final String mentionsOn = getString("mentions-toggle-on", "&aMention pings are now &aON&7.");
    private final String mentionsOff = getString("mentions-toggle-off", "&cMention pings are now &cOFF &7- you will not be notified when someone says your name.");
    private final String chatBlockListEmpty = getString("chat-blocklist-empty", "&7You have not blocked anyone.");
    private final String blockListTitle = getString("chat-blocklist-title", "&#f40d0dBlocked Players");
    private final String blockListBody = getString("chat-blocklist-body", "&7Click a player below to unblock them.");
    private final String blockListEmptyBody = getString("chat-blocklist-empty-body", "&7You have not blocked anyone yet.\n&8Use &f/block <player> &8to block someone.");
    private final String blockListClose = getString("chat-blocklist-close", "&7Close");
    private final String scoreboardShowUsage = getString("scoreboard-show-usage", "Usage: /tab scoreboard show <scoreboard> [player]");
    private final String bossBarNotMarkedAsAnnouncement = getString("bossbar-not-marked-as-announcement", "&cThis bossbar is not marked as an announcement bar and is therefore " +
            "already displayed permanently (if display condition is met)");
    private final List<String> helpMenu = getStringList("help-menu", Arrays.asList("&m                                                                                "
            ," &8>> &3&l/tab reload"
            ,"    &7Reloads plugin and config"
            ," &8>> &3&l/tab &9group&3/&9player &3<name> &9<property> &3<value...>"
            ,"    &7Do &8/tab group/player &7to show properties"
            ," &8>> &3&l/tab parse <player> <placeholder> "
            ,"    &7Test if a placeholder works"
            ," &8>> &3&l/tab dump <player>"
            ,"    &7Dumps information about player and server"
            ," &8>> &3&l/tab cpu"
            ,"    &7shows CPU usage of the plugin"
            ," &8>> &3&l/tab group/player <name> remove"
            ,"    &7Clears all data about player/group"
            ," &8>> &3&l/tab nametag"
            ,"    &7Nametag-related commands"
            ,"&m                                                                                "));
    private final List<String> mySQLHelpMenu = getStringList("mysql-help-menu", Arrays.asList(
            "/tab mysql upload - uploads data from files to mysql",
            "/tab mysql download - downloads data from mysql to files"
    ));
    private final String mySQLFailNotEnabled = getString("mysql-fail-not-enabled", "&cCannot download/upload data from/to MySQL, because it's disabled.");
    private final String mySQLFailError = getString("mysql-fail-error", "MySQL download / upload failed due to an error. Check console for more info.");
    private final String mySQLDownloadSuccess = getString("mysql-download-success", "&aMySQL data downloaded successfully.");
    private final String mySQLUploadSuccess = getString("mysql-upload-success", "&aMySQL data uploaded successfully.");
    private final List<String> scoreboardHelpMenu = getStringList("scoreboard-help-menu", Arrays.asList(
            "/tab scoreboard [on/off/toggle] [player] [options]",
            "/tab scoreboard show <name> [player]",
            "/tab scoreboard announce <name> <length>"
    ));
    private final List<String> bossbarHelpMenu = getStringList("bossbar-help-menu", Arrays.asList(
            "/tab bossbar [on/off/toggle] [player] [options]",
            "/tab bossbar send <name> [player]",
            "/tab bossbar announce <name> <length>"
    ));

    // ------------------
    // Nametags
    // ------------------

    private final List<String> nameTagHelpMenu = getStringList("nametag.help-menu", Arrays.asList(
            "/tab nametag <show/opaque/hide/toggle> [player] [viewer] [-s] - Toggles nametag of specified player",
            "/tab nametag <showview/opaqueview/hideview/toggleview> [viewer] [-s] - Toggles nametag VIEW of specified player"
    ));
    private final String nameTagFeatureNotEnabled = getString("nametag.feature-not-enabled", "&cThis command requires nametag feature to be enabled.");
    private final String nameTagViewHidden = getString("nametag.view-hidden", "&aNametags of all players were hidden to you");
    private final String nameTagViewShown = getString("nametag.view-shown", "&aNametags of all players were shown to you");
    private final String nameTagTargetHidden = getString("nametag.player-hidden", "&aYour nametag was hidden");
    private final String nameTagTargetShown = getString("nametag.player-shown", "&aYour nametag was shown");
    private final String nameTagOpaqueViewShown = getString("nametag.opaque-view-enabled", "&aYou will no longer see nametags of players behind walls");
    private final String nameTagOpaqueTargetShown = getString("nametag.opaque-player-enabled", "&aYour nametag will no longer be visible through walls");
    private final String nameTagNoArgFromConsole = getString("nametag.no-arg-from-console", "&cYou need to specify player if running this command from the console");

    public MessageFile() throws IOException {
        super(MessageFile.class.getClassLoader().getResourceAsStream("config/messages.yml"), new File(HavocTab.getInstance().getDataFolder(), "messages.yml"));
    }

    @NotNull
    public String getBossBarNotFound(@NonNull String name) {
        return getString("bossbar-not-found", "&cNo bossbar found with the name \"%name%\"").replace("%name%", name);
    }

    @NotNull
    public String getGroupDataRemoved(@NonNull String group) {
        return getString("group-data-removed", "&3[HavocTab] All data has been successfully removed from group &e%group%").replace("%group%", group);
    }

    @NotNull
    public String getGroupValueAssigned(@NonNull String property, @NonNull String value, @NonNull String group) {
        return getString("group-value-assigned", "&3[HavocTab] %property% '&r%value%&r&3' has been successfully assigned to group &e%group%")
                .replace("%property%", property).replace("%value%", value).replace("%group%", group);
    }

    @NotNull
    public String getGroupValueRemoved(@NonNull String property, @NonNull String group) {
        return getString("group-value-removed", "&3[HavocTab] %property% has been successfully removed from group &e%group%")
                .replace("%property%", property).replace("%group%", group);
    }

    @NotNull
    public String getPlayerDataRemoved(@NonNull String player) {
        return getString("user-data-removed", "&3[HavocTab] All data has been successfully removed from player &e%player%").replace("%player%", player);
    }

    @NotNull
    public String getPlayerValueAssigned(@NonNull String property, @NonNull String value, @NonNull String player) {
        return getString("user-value-assigned", "&3[HavocTab] %property% '&r%value%&r&3' has been successfully assigned to player &e%player%")
                .replace("%property%", property).replace("%value%", value).replace("%player%", player);
    }

    @NotNull
    public String getPlayerValueRemoved(@NonNull String property, @NonNull String player) {
        return getString("user-value-removed", "&3[HavocTab] %property% has been successfully removed from player &e%player%")
                .replace("%property%", property).replace("%player%", player);
    }

    @NotNull
    public String getPlayerNotFound(@NonNull String name) {
        return getString("player-not-online", "&cNo online player found with the name \"%player%\"").replace("%player%", name);
    }

    @NotNull
    public String getInvalidNumber(@NonNull String input) {
        return getString("invalid-number", "\"%input%\" is not a number!").replace("%input%", input);
    }

    @NotNull
    public String getScoreboardNotFound(@NonNull String name) {
        return getString("scoreboard-not-found", "&cNo scoreboard found with the name \"%name%\"").replace("%name%", name);
    }

    @NotNull
    public String getBossBarAnnouncementSuccess(@NonNull String bar, int length) {
        return getString("bossbar-announcement-success", "&aAnnouncing bossbar &6%bossbar% &afor %length% seconds.")
                .replace("%bossbar%", bar).replace("%length%", String.valueOf(length));
    }

    @NotNull
    public String getBossBarSendSuccess(@NonNull String player, @NonNull String bar, int length) {
        return getString("bossbar-send-success", "&aSending bossbar &6%bossbar% &ato player &6%player% &afor %length% seconds.")
                .replace("%player%", player).replace("%bossbar%", bar).replace("%length%", String.valueOf(length));
    }

    @NotNull
    public String getChatBlocked(@NonNull String player) {
        return getString("chat-blocked", "&7You have blocked &f%player%&7. You will no longer see their messages.")
                .replace("%player%", player);
    }

    @NotNull
    public String getChatUnblocked(@NonNull String player) {
        return getString("chat-unblocked", "&7You have unblocked &f%player%&7.")
                .replace("%player%", player);
    }

    @NotNull
    public String getChatAlreadyBlocked(@NonNull String player) {
        return getString("chat-already-blocked", "&cYou have already blocked %player%.")
                .replace("%player%", player);
    }

    @NotNull
    public String getChatNotBlocked(@NonNull String player) {
        return getString("chat-not-blocked", "&cYou have not blocked %player%.")
                .replace("%player%", player);
    }

    @NotNull
    public String getChatBlockLimit(int limit) {
        return getString("chat-block-limit", "&cYou can only block %limit% players at once.")
                .replace("%limit%", String.valueOf(limit));
    }

    @NotNull
    public String getChatBlockListHeader(int count) {
        return getString("chat-blocklist-header", "&7Blocked players &8(&f%count%&8)")
                .replace("%count%", String.valueOf(count));
    }

    @NotNull
    public String getChatBlockListEntry(@NonNull String player, @NonNull String unblockCommand) {
        return getString("chat-blocklist-entry", "&8 - &f%player% &7(%command% %player%)")
                .replace("%player%", player).replace("%command%", unblockCommand);
    }

    @NotNull
    public String getChatMessageBlocked(@NonNull String player) {
        return getString("chat-message-blocked", "&c%player% is not accepting messages from you.")
                .replace("%player%", player);
    }

    @NotNull
    public String getBlockListUnblockTooltip(@NonNull String player) {
        return getString("chat-blocklist-unblock-tooltip", "&cClick to unblock &f%player%")
                .replace("%player%", player);
    }

    @NotNull
    public String getBlockListMore(int count) {
        return getString("chat-blocklist-more", "&8... and %count% more")
                .replace("%count%", String.valueOf(count));
    }

    @NotNull
    public String getFilterCooldown(int seconds) {
        return getString("filter-cooldown", "&cSlow down - wait %seconds%s before sending another message.")
                .replace("%seconds%", String.valueOf(seconds));
    }

    @NotNull
    public String getTagsMenuTitle(int page, int pages) {
        return getString("tags-menu-title", "Your Tags [%page%/%pages%]")
                .replace("%page%", String.valueOf(page)).replace("%pages%", String.valueOf(pages));
    }

    @NotNull
    public List<String> getTagsStatsLore(int owned, int limited, int favourites, @NonNull String equipped) {
        List<String> out = new ArrayList<>();
        for (String line : getStringList("tags-stats-lore", Arrays.asList(
                "&7Here you can view your tag statistics.",
                "",
                "&e| &fTags Owned &7> &e%owned%",
                "&e| &fLimited Tags Owned &7> &e%limited%",
                "&e| &fTags Favorited &7> &e%favourites%",
                "&e| &fEquipped &7> %equipped%"))) {
            out.add(line.replace("%owned%", String.valueOf(owned))
                    .replace("%limited%", String.valueOf(limited))
                    .replace("%favourites%", String.valueOf(favourites))
                    .replace("%equipped%", equipped));
        }
        return out;
    }

    @NotNull
    public List<String> getTagsLimitedLore(int owned) {
        List<String> out = new ArrayList<>();
        for (String line : getStringList("tags-limited-lore", Arrays.asList(
                "&7Click here to view the special limited tags.",
                "",
                "&d| &fTags owned &7> &d%owned%",
                "",
                "&e> CLICK to Open"))) {
            out.add(line.replace("%owned%", String.valueOf(owned)));
        }
        return out;
    }

    @NotNull
    public List<String> getTagsBackLore() {
        return getStringList("tags-back-lore", Arrays.asList("&7Return to all of your tags.", "", "&e> CLICK to Go Back"));
    }

    @NotNull
    public List<String> getTagsPreviousLore() {
        return getStringList("tags-previous-lore", Arrays.asList("&7Click here to go back to the previous page.", "", "&e> CLICK to Navigate"));
    }

    @NotNull
    public List<String> getTagsNextLore() {
        return getStringList("tags-next-lore", Arrays.asList("&7Click here to go to the next page.", "", "&e> CLICK to Navigate"));
    }

    @NotNull
    public List<String> getTagsClearLore(@NonNull String equipped) {
        List<String> out = new ArrayList<>();
        for (String line : getStringList("tags-clear-lore", Arrays.asList(
                "&7Click here to clear your current player tag.",
                "",
                "&c| &fEquipped &7> %equipped%",
                "",
                "&e> CLICK to Clear"))) {
            out.add(line.replace("%equipped%", equipped));
        }
        return out;
    }

    @NotNull
    public List<String> getTagsSearchLore() {
        return getStringList("tags-search-lore", Arrays.asList("&7Click here to search for any specific tag.", "", "&e> CLICK to Search"));
    }

    @NotNull
    public List<String> getTagsFavouritesLore(int count) {
        List<String> out = new ArrayList<>();
        for (String line : getStringList("tags-favourites-lore", Arrays.asList(
                "&7Click here to view only the tags you favorited.",
                "",
                "&e| &fFavorited &7> &e%count%",
                "",
                "&e> CLICK to Open"))) {
            out.add(line.replace("%count%", String.valueOf(count)));
        }
        return out;
    }

    @NotNull
    public List<String> getTagsItemLore(@NonNull String display, @NonNull String id, boolean equipped, boolean favourite) {
        List<String> out = new ArrayList<>();
        for (String line : getStringList("tags-item-lore", Arrays.asList(
                "&7Here you can view what tags you currently own!",
                "",
                "&e| &fPreview &7> %display%",
                "&e| &fStatus &7> %status%",
                "&e| &fFavorited &7> %favourite%",
                "&e| &fIdentifier &7> &b%id%",
                "",
                "&e> L-CLICK to %equipaction%",
                "&e> R-CLICK to %favaction%"))) {
            out.add(line.replace("%display%", display)
                    .replace("%id%", id)
                    .replace("%status%", equipped ? "&aEQUIPPED" : "&cUNEQUIPPED")
                    .replace("%favourite%", favourite ? "&aFAVORITED" : "&7NOT FAVORITED")
                    .replace("%equipaction%", equipped ? "Unequip" : "Equip")
                    .replace("%favaction%", favourite ? "Unfavorite" : "Favorite"));
        }
        return out;
    }

    @NotNull
    public String getTagEquipped(@NonNull String tag) {
        return getString("tag-equipped", "&aEquipped %tag%").replace("%tag%", tag);
    }

    @NotNull
    public String getTagAlreadyEquipped(@NonNull String tag) {
        return getString("tag-already-equipped",
                "&cUnequip %tag% &cfirst").replace("%tag%", tag);
    }

    @NotNull
    public String getTagLocked(@NonNull String tag) {
        return getString("tag-locked", "&cYou don't own %tag%").replace("%tag%", tag);
    }

    @NotNull
    public String getTagNotFound(@NonNull String tag) {
        return getString("tag-not-found", "&cNo tag \"%tag%\"").replace("%tag%", tag);
    }

    @NotNull
    public String getTagFavourited(@NonNull String tag) {
        return getString("tag-favourited", "&e\u2605 %tag%").replace("%tag%", tag);
    }

    @NotNull
    public String getTagUnfavourited(@NonNull String tag) {
        return getString("tag-unfavourited", "&8\u2606 %tag%").replace("%tag%", tag);
    }

    @NotNull
    public String getTagsNone() {
        return getString("tags-none", "&7You own no tags");
    }

    @NotNull
    public String getTagsHeader(int count) {
        return getString("tags-header", "&7Your tags &8(&f%count%&8)").replace("%count%", String.valueOf(count));
    }

    @NotNull
    public String getTagsEntry(@NonNull String name, @NonNull String id, @NonNull String command) {
        return getString("tags-entry", "&8 - %name% &7(%command% %id%)")
                .replace("%name%", name).replace("%id%", id).replace("%command%", command);
    }
}
