package net.wintooo.modulardelight.content.item.custom;

import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipData;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.wintooo.modulardelight.content.block.custom.entity.StockpotBlockEntity;
import net.wintooo.modulardelight.content.tooltip.StockpotTooltip;

import java.util.Optional;

public class StockpotItem extends BlockItem {
    private static final int BAR_COLOR = MathHelper.packRgb(0.4F, 0.4F, 1.0F);

    public StockpotItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return getStockCount(stack) > 0;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return Math.min(1 + 12 * getStockCount(stack) / StockpotBlockEntity.MAX_STOCK_SERVINGS, 13);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return BAR_COLOR;
    }

    @Override
    public Optional<TooltipData> getTooltipData(ItemStack stack) {
        return Optional.of(new StockpotTooltip.Data(StockpotBlockEntity.getStockFromItem(stack)));
    }

    private static int getStockCount(ItemStack stack) {
        return StockpotBlockEntity.getStockFromItem(stack).getCount();
    }
}