package net.wintooo.modulardelight.content.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.wintooo.modulardelight.ModularDelight;
import net.wintooo.modulardelight.content.block.ModBlocks;
import net.wintooo.modulardelight.content.meal.MealProperty;

import java.util.Comparator;

public final class ModCreativeMenu {

    @SuppressWarnings("unused")
    public static final ItemGroup MODULAR_DELIGHT = Registry.register(
            Registries.ITEM_GROUP,
            ModularDelight.id("modular_delight"),
            FabricItemGroup.builder()
                    .displayName(Text.literal("Modular Delight"))
                    .icon(() -> new ItemStack(ModBlocks.STOCKPOT))
                    .entries((context, entries) -> {
                        entries.add(ModBlocks.STOCKPOT);
                        entries.add(ModItems.MODULAR_MEAL);
                        MealProperty.all().stream()
                                .sorted(Comparator.comparing(p -> p.id().toString()))
                                .forEach(property -> Registries.ITEM.stream()
                                        .filter(item -> new ItemStack(item).isIn(property.tag()))
                                        .sorted((a, b) -> {
                                            Identifier idA = Registries.ITEM.getId(a);
                                            Identifier idB = Registries.ITEM.getId(b);

                                            int cmp = idA.getPath().compareTo(idB.getPath());
                                            return cmp != 0 ? cmp : idA.getNamespace().compareTo(idB.getNamespace());
                                        })
                                        .forEach(entries::add));
                    }).build());

    public static void load() {
    }
}