package dev.havoc.havoctab.shared.features.tablistpages;

import dev.havoc.havoctab.shared.config.file.ConfigurationSection;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Configuration of tablist pagination ({@code tablist-pages} section of config.yml).
 */
@Getter
@RequiredArgsConstructor
public class TablistPagesConfiguration {

    @NotNull private final ConfigurationSection section;

    /**
     * How many players are shown at once.
     * <p>
     * The Java client renders at most 80 tablist entries, so anything above that is
     * simply not drawn. This is why pagination is needed at all.
     */
    private final int pageSize;

    /** Milliseconds each page is shown before the next one */
    private final int cycleInterval;

    /** Whether the viewer's own entry is pinned to every page */
    private final boolean alwaysShowSelf;

    /**
     * Loads the configuration from the given section.
     *
     * @param   section
     *          Section to load from
     * @return  Loaded configuration
     */
    @NotNull
    public static TablistPagesConfiguration fromSection(@NotNull ConfigurationSection section) {
        section.checkForUnknownKey(Arrays.asList("enabled", "page-size", "cycle-interval-milliseconds",
                "always-show-self"));

        int pageSize = section.getInt("page-size", 80);
        if (pageSize > 80) {
            section.startupWarn("page-size is " + pageSize + ", but the Minecraft client only renders 80 " +
                    "tablist entries. Values above 80 mean the extra players are never drawn. Using 80 instead.");
            pageSize = 80;
        }
        if (pageSize < 1) {
            section.startupWarn("page-size must be at least 1, using 80 instead.");
            pageSize = 80;
        }

        int interval = section.getInt("cycle-interval-milliseconds", 5000);
        if (interval < 1000) {
            section.startupWarn("cycle-interval-milliseconds is below 1000. Pages flipping faster than once " +
                    "per second are unreadable and waste bandwidth. Using 1000 instead.");
            interval = 1000;
        }

        return new TablistPagesConfiguration(
                section,
                pageSize,
                interval,
                section.getBoolean("always-show-self", true)
        );
    }
}
