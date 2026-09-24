package dev.havoc.havoctab.shared.placeholders.conditions;

import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

/**
 * Condition that always returns false.
 */
public class FalseCondition extends Condition {

    /** Instance of the class */
    public static final FalseCondition INSTANCE = new FalseCondition();

    private FalseCondition() {
        super("false", Collections.emptyList(), true, "true", "false");
    }

    @Override
    public boolean isMet(@NotNull TabPlayer viewer, @NotNull TabPlayer target) {
        return false;
    }

    @NotNull
    public Condition invert() {
        return TrueCondition.INSTANCE;
    }

    @NotNull
    @Override
    public String toShortFormat() {
        return "false";
    }
}
