package net.wintooo.modulardelight.content.options;

import net.minecraft.client.option.SimpleOption;

public final class ModAccessibilityOptions {
    private ModAccessibilityOptions() {}

    public static final SimpleOption<Boolean> DISABLE_PROPERTY_COLORS =
            SimpleOption.ofBoolean(
                    "options.modulardelight.disable_property_colors",
                    false
            );

}