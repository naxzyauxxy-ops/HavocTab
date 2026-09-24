package dev.havoc.havoctab.shared.features.sorting.types;

import dev.havoc.havoctab.shared.platform.TabPlayer;
import dev.havoc.havoctab.shared.features.sorting.Sorting;
import org.jetbrains.annotations.NotNull;

/**
 * Sorting by a numeric placeholder from lowest to highest
 */
public class PlaceholderLowToHigh extends SortingType {

    /**
     * Constructs new instance with given parameters.
     *
     * @param   sorting
     *          Sorting feature
     * @param   sortingPlaceholder
     *          Placeholder to sort by
     */
    public PlaceholderLowToHigh(Sorting sorting, String sortingPlaceholder) {
        super(sorting, "PLACEHOLDER_LOW_TO_HIGH:" + sortingPlaceholder, sortingPlaceholder);
    }

    @Override
    public String getChars(@NotNull TabPlayer p) {
        if (sortingPlaceholder == null) return "";
        return compressNumber(DEFAULT_NUMBER + parseDouble(setPlaceholders(p), 0, p));
    }

    @Override
    @NotNull
    public String getReturnedValue(@NotNull TabPlayer p) {
        return setPlaceholders(p);
    }
}