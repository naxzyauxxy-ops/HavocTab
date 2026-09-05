package dev.havoc.havoctab.shared.features.scoreboard.lines;

import lombok.NonNull;
import dev.havoc.havoctab.shared.Limitations;
import dev.havoc.havoctab.shared.Property;
import dev.havoc.havoctab.shared.ProtocolVersion;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import dev.havoc.havoctab.shared.features.scoreboard.ScoreboardImpl;
import org.jetbrains.annotations.NotNull;

/**
 * Line using all 3 values - prefix, name and suffix. Line may flicker when placeholder changes value.
 * Limitations:
 *   1.5.x - 1.7.x: up to 42 characters
 *   1.8.x - 1.12.x: up to 66 characters
 *   1.13+: unlimited
 */
public class LongLine extends ScoreboardLine {

    /**
     * Constructs new instance with given parameters
     *
     * @param   parent
     *          scoreboard this line belongs to
     * @param   lineNumber
     *          ID of this line
     * @param   text
     *          line text
     */
    public LongLine(@NonNull ScoreboardImpl parent, int lineNumber, @NonNull String text) {
        super(parent, lineNumber, text);
    }

    @Override
    public void refresh(@NotNull TabPlayer refreshed, boolean force) {
        if (refreshed.scoreboardData.activeScoreboard != parent) return; //player has different scoreboard displayed
        Property lineProperty = refreshed.scoreboardData.lineProperties.get(this);
        if (lineProperty.update()) {
            if (refreshed.getVersion().getMinorVersion() >= 13 && !HavocTab.getInstance().getConfiguration().getConfig().isPacketEventsCompensation()) {
                updateTeam(refreshed, lineProperty.get(), "");
            } else {
                removeLine(refreshed, refreshed.scoreboardData.lineNameProperties.get(this).get());
                String[] values = splitText(
                        forcedPlayerNameStart,
                        parent.getManager().getCache().get(lineProperty.get()).toLegacyText(),
                        refreshed.getVersion().getNetworkId() >= ProtocolVersion.V1_8.getNetworkId() ? Limitations.SCOREBOARD_SCORE_LENGTH_1_8 : Limitations.SCOREBOARD_SCORE_LENGTH_1_7
                );
                addLine(refreshed, values[1], values[0], values[2]);
                refreshed.scoreboardData.lineNameProperties.get(this).changeRawValue(values[1]);
            }
        }
    }

    @Override
    public void register(@NonNull TabPlayer p) {
        p.scoreboardData.lineProperties.put(this, new Property(this, p, text));
        getScoreRefresher().registerProperties(p);
        String value = p.scoreboardData.lineProperties.get(this).get();
        if (p.getVersion().getMinorVersion() >= 13 && !HavocTab.getInstance().getConfiguration().getConfig().isPacketEventsCompensation()) {
            addLine(p, forcedPlayerNameStart, value, "");
            p.scoreboardData.lineNameProperties.put(this, new Property(this, p, forcedPlayerNameStart));
        } else {
            String[] values = splitText(
                    forcedPlayerNameStart,
                    parent.getManager().getCache().get(value).toLegacyText(),
                    p.getVersion().getNetworkId() >= ProtocolVersion.V1_8.getNetworkId() ? Limitations.SCOREBOARD_SCORE_LENGTH_1_8 : Limitations.SCOREBOARD_SCORE_LENGTH_1_7
            );
            addLine(p, values[1], values[0], values[2]);
            p.scoreboardData.lineNameProperties.put(this, new Property(this, p, values[1]));
        }
    }

    @Override
    public void unregister(@NonNull TabPlayer p) {
        if (p.scoreboardData.activeScoreboard == parent) {
            removeLine(p, p.scoreboardData.lineNameProperties.get(this).get());
        }
    }

    @Override
    public void setText(@NonNull String text) {
        ensureActive();
        initializeText(text);
        for (TabPlayer p : parent.getPlayers()) {
            p.scoreboardData.lineProperties.get(this).changeRawValue(text);
            refresh(p, false);
        }
    }

    @Override
    @NotNull
    public String getPlayerName(@NonNull TabPlayer viewer) {
        return viewer.scoreboardData.lineNameProperties.get(this).get();
    }
}