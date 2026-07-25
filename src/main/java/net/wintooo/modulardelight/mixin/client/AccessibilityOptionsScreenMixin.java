package net.wintooo.modulardelight.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.lib.apache.commons.ArrayUtils;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.option.SimpleOption;
import net.wintooo.modulardelight.content.options.ModAccessibilityOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AccessibilityOptionsScreen.class)
public class AccessibilityOptionsScreenMixin {

    @ModifyReturnValue(
        method = "getOptions",
        at = @At("RETURN")
    )
    private static SimpleOption<?>[] addOption(SimpleOption<?>[] original) {
        return ArrayUtils.add(
                original,
                ModAccessibilityOptions.DISABLE_PROPERTY_COLORS
        );
    }
}