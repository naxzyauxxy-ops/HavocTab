package dev.havoc.havoctab.shared.features.header;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import dev.havoc.havoctab.api.tablist.HeaderFooterManager;
import dev.havoc.havoctab.shared.Property;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.cpu.ThreadExecutor;
import dev.havoc.havoctab.shared.features.types.*;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import dev.havoc.havoctab.shared.util.cache.StringToComponentCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Feature handler for header and footer.
 */
@RequiredArgsConstructor
public class HeaderFooter extends RefreshableFeature implements HeaderFooterManager, JoinListener, Loadable, UnLoadable,
        CustomThreaded, Dumpable {

    private final StringToComponentCache headerCache = new StringToComponentCache("Header", 1000);
    private final StringToComponentCache footerCache = new StringToComponentCache("Footer", 1000);
    @Getter private final ThreadExecutor customThread = new ThreadExecutor("HavocTab Header/Footer Thread");
    private final Map<String, HeaderFooterDesign> registeredDesigns = new LinkedHashMap<>();
    private HeaderFooterDesign[] definedDesigns;

    @NonNull private final HeaderFooterConfiguration configuration;

    @Override
    public void load() {
        for (Map.Entry<String, HeaderFooterConfiguration.HeaderFooterDesignDefinition> entry : configuration.getDesigns().entrySet()) {
            String designName = entry.getKey();
            HeaderFooterDesign design = new HeaderFooterDesign(this, designName, entry.getValue());
            registeredDesigns.put(designName, design);
            HavocTab.getInstance().getFeatureManager().registerFeature(TabConstants.Feature.design(designName), design);
        }
        definedDesigns = registeredDesigns.values().toArray(new HeaderFooterDesign[0]);
        for (TabPlayer p : HavocTab.getInstance().getOnlinePlayers()) {
            onJoin(p);
        }
    }

    @Override
    public void unload() {
        for (TabPlayer p : HavocTab.getInstance().getOnlinePlayers()) {
            if (p.headerFooterData.activeDesign == null) continue;
            p.getTabList().setPlayerListHeaderFooter(null, null);
        }
    }

    @Override
    public void onJoin(@NotNull TabPlayer connectedPlayer) {
        sendHighestDesign(connectedPlayer);
    }

    private void sendHighestDesign(@NotNull TabPlayer player) {
        HeaderFooterDesign highest = detectHighestDesign(player);
        HeaderFooterDesign current = player.headerFooterData.activeDesign;
        if (highest != current) {
            player.headerFooterData.activeDesign = highest;
            if (highest != null) {
                sendHeaderFooter(player);
            } else {
                player.getTabList().setPlayerListHeaderFooter(null, null);
            }
        }
    }

    @Nullable
    private HeaderFooterDesign detectHighestDesign(@NonNull TabPlayer p) {
        for (HeaderFooterDesign design : definedDesigns) {
            if (design.isConditionMet(p)) return design;
        }
        return null;
    }

    @NotNull
    @Override
    public String getRefreshDisplayName() {
        return "Switching designs";
    }

    @Override
    public void refresh(@NotNull TabPlayer p, boolean force) {
        sendHighestDesign(p);
    }

    /**
     * Sends header and footer to player based on currently active design or forced
     * header/footer set by the API.
     *
     * @param   player
     *          Player to send header and footer to
     */
    public void sendHeaderFooter(@NotNull TabPlayer player) {
        String header;
        String footer;
        if (player.headerFooterData.forcedHeader != null) {
            header = player.headerFooterData.forcedHeader.updateAndGet();
        } else if (player.headerFooterData.activeDesign != null) {
            Property prop = player.headerFooterData.headerProperties.get(player.headerFooterData.activeDesign);
            if (prop == null) {
                prop = new Property(player.headerFooterData.activeDesign, player, String.join("\n", player.headerFooterData.activeDesign.getDefinition().getHeader()));
                player.headerFooterData.headerProperties.put(player.headerFooterData.activeDesign, prop);
            }
            header = prop.updateAndGet();
        } else {
            header = "";
        }
        if (player.headerFooterData.forcedFooter != null) {
            footer = player.headerFooterData.forcedFooter.updateAndGet();
        } else if (player.headerFooterData.activeDesign != null) {
            Property prop = player.headerFooterData.footerProperties.get(player.headerFooterData.activeDesign);
            if (prop == null) {
                prop = new Property(player.headerFooterData.activeDesign, player, String.join("\n", player.headerFooterData.activeDesign.getDefinition().getFooter()));
                player.headerFooterData.footerProperties.put(player.headerFooterData.activeDesign, prop);
            }
            footer = prop.updateAndGet();
        } else {
            footer = "";
        }
        player.getTabList().setPlayerListHeaderFooter(headerCache.get(header), footerCache.get(footer));
    }

    // ------------------
    // API Implementation
    // ------------------

    @Override
    public void setHeader(@NotNull dev.havoc.havoctab.api.TabPlayer p, @Nullable String header) {
        ensureActive();
        customThread.execute(() -> {
            TabPlayer player = (TabPlayer) p;
            if (header != null) {
                player.headerFooterData.forcedHeader = new Property(this, player, header);
            } else {
                player.headerFooterData.forcedHeader = null;
            }
            sendHeaderFooter(player);
        });
    }

    @Override
    public void setFooter(@NotNull dev.havoc.havoctab.api.TabPlayer p, @Nullable String footer) {
        ensureActive();
        customThread.execute(() -> {
            TabPlayer player = (TabPlayer) p;
            if (footer != null) {
                player.headerFooterData.forcedFooter = new Property(this, player, footer);
            } else {
                player.headerFooterData.forcedFooter = null;
            }
            sendHeaderFooter(player);
        });
    }

    @Override
    public void setHeaderAndFooter(@NotNull dev.havoc.havoctab.api.TabPlayer p, @Nullable String header, @Nullable String footer) {
        ensureActive();
        customThread.execute(() -> {
            TabPlayer player = (TabPlayer) p;
            if (header != null) {
                player.headerFooterData.forcedHeader = new Property(this, player, header);
            } else {
                player.headerFooterData.forcedHeader = null;
            }
            if (footer != null) {
                player.headerFooterData.forcedFooter = new Property(this, player, footer);
            } else {
                player.headerFooterData.forcedFooter = null;
            }
            sendHeaderFooter(player);
        });
    }

    @NotNull
    @Override
    public String getFeatureName() {
        return "Header/Footer";
    }

    @Override
    @NotNull
    public Object dump(@NotNull TabPlayer player) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("configuration", configuration.getSection().getMap());
        map.put("chain", new LinkedHashMap<String, Object>() {{
            for (HeaderFooterDesign design : definedDesigns) {
                 put(design.getName(), design.dump(player, player.headerFooterData.activeDesign));
            }
        }});
        if (player.headerFooterData.activeDesign != null) {
            map.put("currently displayed design", new LinkedHashMap<String, Object>() {{
                put("name", player.headerFooterData.activeDesign.getName());
                put("header", player.headerFooterData.headerProperties.get(player.headerFooterData.activeDesign).get().split("\n"));
                put("footer", player.headerFooterData.footerProperties.get(player.headerFooterData.activeDesign).get().split("\n"));
            }});
        } else {
            map.put("currently displayed design", null);
        }
        return map;
    }
}
