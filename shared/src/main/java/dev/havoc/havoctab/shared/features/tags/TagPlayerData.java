package dev.havoc.havoctab.shared.features.tags;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Per-player tag state.
 */
public class TagPlayerData {

    /** Id of the equipped tag, lowercase, or {@code null} for none */
    @Nullable
    public volatile String equipped;

    /** Ids of favourited tags, lowercase */
    @NotNull
    private final Set<String> favourites = Collections.synchronizedSet(new LinkedHashSet<>());

    /**
     * Returns a snapshot of this player's favourites.
     *
     * @return  Copy of the favourite ids
     */
    @NotNull
    public Set<String> getFavourites() {
        synchronized (favourites) {
            return new LinkedHashSet<>(favourites);
        }
    }

    /**
     * Returns whether a tag is favourited.
     *
     * @param   id
     *          Tag id
     * @return  {@code true} if favourited
     */
    public boolean isFavourite(@NotNull String id) {
        return favourites.contains(id.toLowerCase(Locale.US));
    }

    /**
     * Flips the favourite state of a tag.
     *
     * @param   id
     *          Tag id
     * @return  {@code true} if it is now favourited
     */
    public boolean toggleFavourite(@NotNull String id) {
        String key = id.toLowerCase(Locale.US);
        synchronized (favourites) {
            if (favourites.remove(key)) return false;
            favourites.add(key);
            return true;
        }
    }

    /**
     * Replaces the favourites, used when loading from disk.
     *
     * @param   values
     *          New favourite ids
     */
    public void setFavourites(@NotNull Iterable<String> values) {
        synchronized (favourites) {
            favourites.clear();
            for (String value : values) favourites.add(value.toLowerCase(Locale.US));
        }
    }
}
