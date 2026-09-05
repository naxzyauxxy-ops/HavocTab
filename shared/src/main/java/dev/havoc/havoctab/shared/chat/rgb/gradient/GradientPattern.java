package dev.havoc.havoctab.shared.chat.rgb.gradient;

import dev.havoc.havoctab.shared.chat.TabTextColor;
import dev.havoc.havoctab.shared.util.function.TriFunction;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract class for applying different gradient patterns
 */
public interface GradientPattern {

    /**
     * Applies gradients in provided text and returns text using only #RRGGBB
     *
     * @param   text
     *          text to be reformatted
     * @param   gradientFunction
     *          Function for reformatting gradient to new text
     * @return  reformatted text
     */
    @NotNull
    String applyPattern(@NotNull String text, @NotNull TriFunction<TabTextColor, String, TabTextColor, String> gradientFunction);
}