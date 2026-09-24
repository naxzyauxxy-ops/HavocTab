package dev.havoc.havoctab.shared.chat.rgb.format;

import dev.havoc.havoctab.shared.chat.TabTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Abstract class for different RGB patterns
 */
public interface RGBFormatter {

    /**
     * Re-formats RGB codes in the provided text using given formatter.
     *
     * @param   text
     *          text to format
     * @param   rgbFunction
     *          Function for converting RGB codes to desired format
     * @return  reformatted text
     */
    @NotNull
    String reformat(@NotNull String text, @NotNull Function<TabTextColor, String> rgbFunction);
}