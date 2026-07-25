package net.wintooo.modulardelight.content.options;

import net.minecraft.client.MinecraftClient;

public final class AccessibilityHelper {
    private AccessibilityHelper() {}

    public static boolean usePropertyColors() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client == null
                || !ModAccessibilityOptions.DISABLE_PROPERTY_COLORS.getValue();
    }
}