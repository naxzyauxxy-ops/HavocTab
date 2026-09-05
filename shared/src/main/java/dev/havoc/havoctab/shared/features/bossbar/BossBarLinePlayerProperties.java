package dev.havoc.havoctab.shared.features.bossbar;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import dev.havoc.havoctab.shared.Property;

/**
 * Class storing properties of a bossbar for player.
 */
@RequiredArgsConstructor
public class BossBarLinePlayerProperties {

    /** Property holding BossBar title */
    @NonNull public final Property textProperty;

    /** Property holding BossBar progress */
    @NonNull public final Property progressProperty;

    /** Property holding BossBar color */
    @NonNull public final Property colorProperty;

    /** Property holding BossBar style */
    @NonNull public final Property styleProperty;
}
