package net.wintooo.modulardelight.content.block;

import net.minecraft.block.*;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.wintooo.modulardelight.ModularDelight;
import net.wintooo.modulardelight.content.block.custom.StockpotBlock;
import net.wintooo.modulardelight.content.item.ModItems;
import net.wintooo.modulardelight.content.item.custom.StockpotItem;

public class ModBlocks {

    public static final StockpotBlock STOCKPOT = register("stockpot",
            new StockpotBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.IRON_GRAY)
                    .strength(0.5F, 6.0F)
                    .sounds(BlockSoundGroup.LANTERN)
                    .nonOpaque()));

    static {
        ModItems.register("stockpot", new StockpotItem(STOCKPOT, new Item.Settings()));
    }

    public static <T extends Block> T register(String name, T block) {
        return Registry.register(Registries.BLOCK, ModularDelight.id(name), block);
    }

    public static void load() {}
}