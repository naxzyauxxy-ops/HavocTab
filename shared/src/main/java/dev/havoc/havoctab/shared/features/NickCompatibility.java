package dev.havoc.havoctab.shared.features;

import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.TabConstants.CpuUsageCategory;
import dev.havoc.havoctab.shared.cpu.CpuManager;
import dev.havoc.havoctab.shared.cpu.TimedCaughtTask;
import dev.havoc.havoctab.shared.features.belowname.BelowName;
import dev.havoc.havoctab.shared.features.nametags.NameTag;
import dev.havoc.havoctab.shared.features.playerlistobjective.YellowNumber;
import dev.havoc.havoctab.shared.features.proxy.ProxyPlayer;
import dev.havoc.havoctab.shared.features.proxy.ProxySupport;
import dev.havoc.havoctab.shared.features.types.EntryAddListener;
import dev.havoc.havoctab.shared.features.types.TabFeature;
import dev.havoc.havoctab.shared.platform.Scoreboard;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.UUID;

/**
 * This feature attempts to provide compatibility with nick/disguise plugins by
 * listening to player add packet and see if nickname is different. If it is, player
 * is considered nicked and all name-bound features will use this new nickname.
 */
public class NickCompatibility extends TabFeature implements EntryAddListener {

    @Nullable private final NameTag nameTags = HavocTab.getInstance().getNameTagManager();
    @Nullable private final BelowName belowname = HavocTab.getInstance().getFeatureManager().getFeature(TabConstants.Feature.BELOW_NAME);
    @Nullable private final YellowNumber yellownumber = HavocTab.getInstance().getFeatureManager().getFeature(TabConstants.Feature.YELLOW_NUMBER);
    @Nullable private final ProxySupport proxy = HavocTab.getInstance().getFeatureManager().getFeature(TabConstants.Feature.PROXY_SUPPORT);

    public synchronized void onEntryAdd(TabPlayer packetReceiver, UUID id, String name) {
        TabPlayer packetPlayer = HavocTab.getInstance().getPlayerByTabListUUID(id);
        // Using "packetPlayer == packetReceiver" for now, as this should technically not matter, but it does
        // A nick plugin author said the nickname will be different for other players but same for nicking player,
        //      but the plugin does not even do that and changes it for everyone.
        // Another plugin changes name only for nicked player, for others it changes it in pipeline, which is
        //      injected after HavocTab, so this cannot be read properly and would false trigger un-nick.
        // For some very specific complicated plugins this may need to be different, either changing == to !=
        //      or completely removing the check. However, only this option worked for both nick plugins I tested.
        if (packetPlayer != null && packetPlayer == packetReceiver && !packetPlayer.getNickname().equals(name)) {
            packetPlayer.setNickname(name);
            HavocTab.getInstance().debug("Processing name change of player " + packetPlayer.getName() + " to " + name);
            processNameChange(packetPlayer);
        }
        if (proxy != null) {
            ProxyPlayer proxyPlayer = proxy.getProxyPlayers().get(id);
            if (proxyPlayer == null) return;
            if (!proxyPlayer.getNickname().equals(name)) {
                proxyPlayer.setNickname(name);
                HavocTab.getInstance().debug("[Proxy Support] Processing name change of proxy player " + proxyPlayer.getName() + " to " + name);
                processNameChange(proxyPlayer);
            }
        }
    }

    /**
     * Processes name change in all features.
     *
     * @param   player
     *          Player to update in all features
     */
    public void processNameChange(@NotNull TabPlayer player) {
        CpuManager cpu = HavocTab.getInstance().getCpu();
        cpu.getProcessingThread().execute(new TimedCaughtTask(cpu, () -> {
            if (nameTags != null && !player.teamData.isDisabled())
                for (TabPlayer viewer : nameTags.getOnlinePlayers().getPlayers()) {
                    viewer.teamData.unregisterTeam(player);
                    viewer.teamData.registerTeam(
                            player,
                            player.sortingData.getShortTeamName(),
                            nameTags.getPrefixCache().get(player.teamData.prefix.getFormat(viewer)),
                            nameTags.getSuffixCache().get(player.teamData.suffix.getFormat(viewer)),
                            player.teamData.getTeamVisibility(viewer) ? Scoreboard.NameVisibility.ALWAYS : Scoreboard.NameVisibility.NEVER,
                            player.teamData.getCollisionRule() ? Scoreboard.CollisionRule.ALWAYS : Scoreboard.CollisionRule.NEVER,
                            Collections.singletonList(player.getNickname()),
                            nameTags.getTeamOptions(),
                            nameTags.getLastColorCache().get(player.teamData.prefix.getFormat(viewer)).getLastStyle().toEnumChatFormat()
                    );
                }
            if (belowname != null) belowname.processNicknameChange(player);
            if (yellownumber != null) yellownumber.processNicknameChange(player);
        }, getFeatureName(), CpuUsageCategory.NICK_PLUGIN_COMPATIBILITY));
    }

    private void processNameChange(ProxyPlayer player) {
        CpuManager cpu = HavocTab.getInstance().getCpu();
        cpu.getProcessingThread().execute(new TimedCaughtTask(cpu, () -> {
            if (nameTags != null && player.getNametag() != null) {
                String teamName = player.getNametag().getResolvedTeamName();
                for (TabPlayer viewer : nameTags.getOnlinePlayers().getPlayers()) {
                    viewer.teamData.unregisterTeam(player);
                    viewer.teamData.registerTeam(
                            player,
                            teamName,
                            player.getNametag().getFeature().getPrefixCache().get(player.getNametag().getPrefix()),
                            player.getNametag().getFeature().getSuffixCache().get(player.getNametag().getSuffix()),
                            player.getNametag().getNameVisibility(),
                            Scoreboard.CollisionRule.ALWAYS,
                            Collections.singletonList(player.getNickname()),
                            nameTags.getTeamOptions(),
                            player.getNametag().getFeature().getLastColorCache().get(player.getNametag().getPrefix()).getLastStyle().toEnumChatFormat()
                    );
                }
            }
            if (belowname != null && player.getBelowname() != null) {
                for (TabPlayer all : belowname.getOnlinePlayers().getPlayers()) {
                    all.getScoreboard().setScore(
                            BelowName.OBJECTIVE_NAME,
                            player.getNickname(),
                            player.getBelowname().getValue(),
                            null, // Unused by this objective slot
                            player.getBelowname().getFeature().getCache().get(player.getBelowname().getFancyValue())
                    );
                }
            }
            if (yellownumber != null && player.getPlayerlist() != null) {
                for (TabPlayer all : yellownumber.getOnlinePlayers().getPlayers()) {
                    all.getScoreboard().setScore(
                            YellowNumber.OBJECTIVE_NAME,
                            player.getNickname(),
                            player.getPlayerlist().getValue(),
                            null, // Unused by this objective slot
                            player.getPlayerlist().getFeature().getCache().get(player.getPlayerlist().getFancyValue())
                    );
                }
            }
        }, getFeatureName(), CpuUsageCategory.NICK_PLUGIN_COMPATIBILITY));
    }

    @NotNull
    @Override
    public String getFeatureName() {
        return "Nick compatibility";
    }
}
