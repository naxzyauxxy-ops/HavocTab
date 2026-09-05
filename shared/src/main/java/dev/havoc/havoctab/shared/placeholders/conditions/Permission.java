package dev.havoc.havoctab.shared.placeholders.conditions;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import dev.havoc.havoctab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * An expression that checks if a player has a specific permission.
 */
@RequiredArgsConstructor
public class Permission extends ConditionalExpression {

    /** Permission to check */
    private final String permission;

    @Override
    public boolean isMet(@NonNull TabPlayer viewer, @NonNull TabPlayer target) {
        return target.hasPermission(permission);
    }

    @Override
    @NotNull
    public ConditionalExpression invert() {
        return new NotPermission(permission);
    }

    @Override
    @NotNull
    public String toShortFormat() {
        return "permission:" + permission;
    }

    @Override
    public boolean hasRelationalContent() {
        return false;
    }
}
