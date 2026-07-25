package net.wintooo.modulardelight.content.meal;

import net.minecraft.text.Text;
import net.wintooo.modulardelight.content.options.AccessibilityHelper;

import java.math.BigDecimal;


public final class MealTooltipStyle {
    private MealTooltipStyle() {}

    public static Text property(MealProperty property, Text text) {
        if (!AccessibilityHelper.usePropertyColors()) {
            return text.copy();
        }

        return text.copy().styled(style -> style.withColor(property.color()));
    }

    public static boolean hasNonDefaultMultiplier(double multiplier) {
        return Math.abs(multiplier - 1.0) > 0.0001;
    }

    public static Text multiplier(double multiplier) {
        Text text = Text.literal("(×" + formatMultiplier(multiplier) + ")");

        if (!AccessibilityHelper.usePropertyColors()) {
            return text;
        }

        return text.copy().styled(style -> style.withColor(multiplierColor(multiplier)));
    }

    private static String formatMultiplier(double multiplier) {
        return BigDecimal.valueOf(multiplier).stripTrailingZeros().toPlainString();
    }

    private static int multiplierColor(double multiplier) {
        double logarithmicMultiplier = Math.log(Math.max(multiplier, 0.01)) / Math.log(2.0);
        double progress = clamp((logarithmicMultiplier + 1.0) / 3.0);
        return hsvToRgb((1.0 - progress) * 0.33);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static int hsvToRgb(double hue) {
        double chroma = 0.96 * 0.78;
        double sector = hue * 6.0;
        double secondary = chroma * (1.0 - Math.abs(sector % 2.0 - 1.0));
        double match = 0.96 - chroma;

        double red;
        double green;
        double blue;
        if (sector < 1.0) {
            red = chroma; green = secondary; blue = 0.0;
        } else if (sector < 2.0) {
            red = secondary; green = chroma; blue = 0.0;
        } else if (sector < 3.0) {
            red = 0.0; green = chroma; blue = secondary;
        } else if (sector < 4.0) {
            red = 0.0; green = secondary; blue = chroma;
        } else if (sector < 5.0) {
            red = secondary; green = 0.0; blue = chroma;
        } else {
            red = chroma; green = 0.0; blue = secondary;
        }

        return ((int) Math.round((red + match) * 255.0) << 16)
                | ((int) Math.round((green + match) * 255.0) << 8)
                | (int) Math.round((blue + match) * 255.0);
    }
}
