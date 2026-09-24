package dev.havoc.havoctab.shared.features.types;

import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.placeholders.PlaceholderReference;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Interface for features periodically refreshing visuals
 */
public abstract class RefreshableFeature extends TabFeature {

    /**
     * Returns display name of {@link #refresh(TabPlayer, boolean)} called for this feature in /tab cpu.
     *
     * @return  Display name of {@link #refresh(TabPlayer, boolean)} called for this feature in /tab cpu
     */
    @NotNull
    public abstract String getRefreshDisplayName();

    /**
     * Called when a placeholder used by this feature changes value
     *
     * @param   refreshed
     *          Player which a placeholder changed value for
     * @param   force
     *          Whether refresh should be forced
     */
    public abstract void refresh(@NotNull TabPlayer refreshed, boolean force);

    /**
     * Registers this feature as one using specified placeholders
     *
     * @param   placeholders
     *          placeholders to add as used in this feature
     */
    public void addUsedPlaceholderReferences(@NotNull Collection<PlaceholderReference> placeholders) {
        if (placeholders.isEmpty()) return;
        for (PlaceholderReference p : placeholders) {
            HavocTab.getInstance().getPlaceholderManager().addUsedPlaceholder(p.getHandle(), this);
        }
    }

    /**
     * Registers this feature as one using specified placeholder.
     *
     * @param   placeholder
     *          placeholder to add as used in this feature
     */
    public void addUsedPlaceholder(@NotNull String placeholder) {
        HavocTab.getInstance().getPlaceholderManager().addUsedPlaceholder(placeholder, this);
    }
}
