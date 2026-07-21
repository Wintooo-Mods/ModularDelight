package net.wintooo.modulardelight.content.screen;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.wintooo.modulardelight.ModularDelight;

public class ModScreenHandlers {
    public static final ScreenHandlerType<StockpotScreenHandler> STOCKPOT = Registry.register(
            Registries.SCREEN_HANDLER,
            ModularDelight.id("stockpot"),
            new ScreenHandlerType<>(StockpotScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
    );

    public static void load() {}
}