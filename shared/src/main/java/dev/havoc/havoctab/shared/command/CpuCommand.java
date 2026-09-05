package dev.havoc.havoctab.shared.command;

import dev.havoc.havoctab.api.placeholder.Placeholder;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.chat.component.TabComponent;
import dev.havoc.havoctab.shared.cpu.CpuReport;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Handler for "/tab cpu" subcommand
 */
public class CpuCommand extends SubCommand {

    private final DecimalFormat decimal3 = new DecimalFormat("#.###");
    private final char LINE_CHAR = 9553;

    /**
     * Constructs new instance
     */
    public CpuCommand() {
        super("cpu", TabConstants.Permission.COMMAND_CPU);
    }

    @Override
    public void execute(@Nullable TabPlayer sender, @NotNull String[] args) {
        CpuReport report = HavocTab.getInstance().getCPUManager().getLastReport();
        if (report == null) {
            if (HavocTab.getInstance().getCPUManager().enableTracking()) {
                sendMessage(sender, "&aCPU usage tracking has been enabled. Run the command again in 10 seconds to see the first results.");
            } else {
                sendMessage(sender, "&cPlease wait at least 10 seconds since running the command for the first time.");
            }
            return;
        }
        Map<String, Map<String, Float>> features = report.getFeatureUsage();
        sendMessage(sender, " ");
        sendMessage(sender, "&8&l" + LINE_CHAR + "&8&m             &r&8&l[ &bHavocTab CPU Stats &8&l]&r&8&l&m             ");
        sendMessage(sender, "&8&l" + LINE_CHAR + " &6CPU stats from the last 10 seconds");
        sendMessage(sender, "&8&l" + LINE_CHAR + "&8&m                                                    ");
        sendMessage(sender, "&8&l" + LINE_CHAR + " &6Top 5 placeholders:");
        printPlaceholders(sender, report.getPlaceholderUsage());
        sendMessage(sender, "&8&l" + LINE_CHAR + "&8&m                                                    ");
        if (sender != null) {
            sendToPlayer(sender, features);
        } else {
            sendToConsole(features);
        }
        sendMessage(sender, "&8&l" + LINE_CHAR + "&8&m                                                    ");
        sendMessage(sender, String.format("&8&l%s &6&lPlaceholders Total: &a&l%s%%", LINE_CHAR, colorize(decimal3.format(report.getPlaceholderUsageTotal()), 10, 5)));
        sendMessage(sender, String.format("&8&l%s &6&lPlugin internals: &a&l%s%%", LINE_CHAR, colorize(decimal3.format(report.getFeatureUsageTotal()-report.getPlaceholderUsageTotal()), 10, 5)));
        sendMessage(sender, String.format("&8&l%s &6&lTotal: &e&l%s%%", LINE_CHAR, colorize(decimal3.format(report.getFeatureUsageTotal()), 10, 5)));
        sendMessage(sender, "&8&l" + LINE_CHAR + "&8&m             &r&8&l[ &bHavocTab CPU Stats &8&l]&r&8&l&m             ");
        sendMessage(sender, " ");
    }

    private void printPlaceholders(@Nullable TabPlayer sender, @NotNull Map<String, Float> map) {
        int printCounter = 0;
        for (Entry<String, Float> entry : map.entrySet()) {
            if (printCounter++ == 5) break;
            Placeholder p = HavocTab.getInstance().getPlaceholderManager().getPlaceholder(entry.getKey());
            Integer configuredRefresh = HavocTab.getInstance().getPlaceholderManager().getConfiguration().getRefreshIntervals().get(entry.getKey());
            String refresh = String.format(" %s (%d)&7", configuredRefresh == null ? "&8" : "&3", p.getRefresh());
            String colorized = entry.getKey().startsWith("%sync:") ? "&c" + decimal3.format(entry.getValue()) : colorize(decimal3.format(entry.getValue()), 1, 0.3f);
            sendMessage(sender, String.format("&8&l%s &7%s - %s%%", LINE_CHAR, entry.getKey() + refresh, colorized));
        }
    }

    private void sendToConsole(@NotNull Map<String, Map<String, Float>> features) {
        HavocTab.getInstance().getPlatform().logInfo(TabComponent.fromColoredText("&8&l" + LINE_CHAR + " &6Features:"));
        for (Entry<String, Map<String, Float>> entry : features.entrySet()) {
            HavocTab.getInstance().getPlatform().logInfo(TabComponent.fromColoredText(
                    String.format("&8&l%s &7%s &7(%s%%&7):", LINE_CHAR, entry.getKey(),
                            colorize(decimal3.format(entry.getValue().values().stream().mapToDouble(Float::floatValue).sum()), 5, 1))));
            for (Entry<String, Float> type : entry.getValue().entrySet()) {
                HavocTab.getInstance().getPlatform().logInfo(TabComponent.fromColoredText(
                        String.format("&8&l%s     &7%s - %s%%", LINE_CHAR, type.getKey(), colorize(decimal3.format(type.getValue()), 5, 1))));
            }
        }
    }

    private void sendToPlayer(@NotNull TabPlayer sender, @NotNull Map<String, Map<String, Float>> features) {
        sendMessage(sender, "&8&l" + LINE_CHAR + " &6Features (execute from console for more info):");
        for (Entry<String, Map<String, Float>> entry : features.entrySet()) {
            double featureTotal = entry.getValue().values().stream().mapToDouble(Float::floatValue).sum();
            sender.sendMessage(String.format("&8&l%s &7%s &7(%s%%&7):", LINE_CHAR, entry.getKey(), colorize(decimal3.format(featureTotal), 5, 1)));
        }
    }

    /**
     * Returns colored usage from provided usage
     *
     * @param   usage
     *          usage
     * @return  colored usage
     */
    private String colorize(@NotNull String usage, float threshold1, float threshold2) {
        float percent = Float.parseFloat(usage.replace(",", "."));
        if (percent > threshold1) return "&c" + usage;
        if (percent > threshold2) return "&e" + usage;
        return "&a" + usage;
    }
}