package net.wintooo.modulardelight.content.item;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.wintooo.modulardelight.ModularDelight;
import net.wintooo.modulardelight.content.item.custom.ModularMealItem;

public class ModItems {
    public static final ModularMealItem MODULAR_MEAL = register("modular_meal",
            new ModularMealItem(new Item.Settings()
                    .maxCount(16)
                    .food(new net.minecraft.item.FoodComponent.Builder()
                            .hunger(0)
                            .saturationModifier(0f)
                            .alwaysEdible()
                            .build())));


    public static <T extends Item> T register(String name, T item) {
        return Registry.register(Registries.ITEM, ModularDelight.id(name), item);
    }

    public static void load() {}
}