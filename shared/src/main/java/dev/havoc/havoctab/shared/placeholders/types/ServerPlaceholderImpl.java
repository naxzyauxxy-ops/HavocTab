package dev.havoc.havoctab.shared.placeholders.types;

import lombok.Getter;
import lombok.NonNull;
import dev.havoc.havoctab.api.placeholder.ServerPlaceholder;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.TabConstants;
import dev.havoc.havoctab.shared.cpu.TimedCaughtTask;
import dev.havoc.havoctab.shared.features.types.CustomThreaded;
import dev.havoc.havoctab.shared.features.types.RefreshableFeature;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Implementation of ServerPlaceholder interface
 */
public class ServerPlaceholderImpl extends TabPlaceholder implements ServerPlaceholder {

    /** Placeholder function returning fresh output on request */
    private final Supplier<String> supplier;

    /** Last known output of the placeholder */
    @Getter
    @NotNull
    private String lastValue = identifier;

    /**
     * Constructs new instance with given parameters
     *
     * @param   identifier
     *          placeholder's identifier, must start and end with %
     * @param   refresh
     *          refresh interval in milliseconds, must be divisible by {@link TabConstants.Placeholder#MINIMUM_REFRESH_INTERVAL}
     *          or equal to -1 to disable automatic refreshing
     * @param   supplier
     *          supplier returning fresh output on request
     */
    public ServerPlaceholderImpl(@NonNull String identifier, int refresh, @NonNull Supplier<String> supplier) {
        super(identifier, refresh);
        if (identifier.startsWith("%rel_")) throw new IllegalArgumentException("\"rel_\" is reserved for relational placeholder identifiers");
        this.supplier = supplier;
        hasValueChanged(request());
    }

    @Override
    public void update() {
        updateValue(request());
    }

    @Override
    public void updateValue(@Nullable String value) {
        if (hasValueChanged(value)) {
            for (RefreshableFeature r : getUsedByFeatures()) {
                for (TabPlayer all : HavocTab.getInstance().getOnlinePlayers()) {
                    if (!all.isLoaded()) return; // Updated on join
                    TimedCaughtTask task = new TimedCaughtTask(HavocTab.getInstance().getCpu(), () -> r.refresh(all, false), r.getFeatureName(), r.getRefreshDisplayName());
                    if (r instanceof CustomThreaded) {
                        ((CustomThreaded) r).getCustomThread().execute(task);
                    } else {
                        task.run();
                    }
                }
            }
        }
    }

    public boolean hasValueChanged(@Nullable String value) {
        if (value == null) return false;
        String newValue = setPlaceholders(replacements.findReplacement(value), null);

        if (!ERROR_VALUE.equals(newValue) && !lastValue.equals(newValue)) {
            lastValue = newValue;
            for (TabPlayer player : HavocTab.getInstance().getOnlinePlayers()) {
                updateParents(player);
                player.expansionData.setPlaceholderValue(identifier, newValue);
            }
            return true;
        }
        return false;
    }

    @Override
    public void updateFromNested(@NonNull TabPlayer unused) {
        hasValueChanged(request());
    }

    @Override
    @NotNull
    public String getLastValue(@Nullable TabPlayer p) {
        return lastValue;
    }

    @Override
    @NotNull
    public String getLastValueSafe(@NotNull TabPlayer player) {
        return lastValue;
    }

    /**
     * Calls the placeholder request function and returns the output.
     * If the placeholder threw an exception, it is logged in {@code placeholder-errors.log}
     * file and {@link #ERROR_VALUE} is returned.
     *
     * @return  value placeholder returned or {@link #ERROR_VALUE} if it threw an error
     */
    @Nullable
    public String request() {
        long time = System.currentTimeMillis();
        try {
            return supplier.get();
        } catch (Throwable t) {
            HavocTab.getInstance().getErrorManager().placeholderError("Server placeholder " + identifier + " generated an error", t);
            return ERROR_VALUE;
        } finally {
            long timeDiff = System.currentTimeMillis() - time;
            if (timeDiff > TabConstants.Placeholder.RETURN_TIME_WARN_THRESHOLD) {
                HavocTab.getInstance().debug("Placeholder " + identifier + " took " + timeDiff + "ms to return value");
            }
        }
    }
}