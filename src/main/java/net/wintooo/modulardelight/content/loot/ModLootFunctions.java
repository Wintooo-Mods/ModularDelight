package net.wintooo.modulardelight.content.loot;

import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.wintooo.modulardelight.ModularDelight;

public class ModLootFunctions {
    public static final LootFunctionType COPY_MEAL = Registry.register(
            Registries.LOOT_FUNCTION_TYPE,
            ModularDelight.id("copy_meal"),
            new LootFunctionType(new CopyStockFunction.Serializer())
    );

    public static void load() {}
}