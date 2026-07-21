package net.wintooo.modulardelight;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.render.RenderLayer;
import net.wintooo.modulardelight.content.block.ModBlocks;
import net.wintooo.modulardelight.content.item.custom.MealColor;
import net.wintooo.modulardelight.content.screen.ModScreenHandlers;
import net.wintooo.modulardelight.content.item.custom.tooltip.StockpotTooltip;
import net.wintooo.modulardelight.content.screen.StockpotScreen;
import net.wintooo.modulardelight.content.item.ModItems;
import net.wintooo.modulardelight.content.item.custom.ModularMealItem;
import net.wintooo.modulardelight.content.item.custom.tooltip.MealPropertyTooltip;
import net.wintooo.modulardelight.content.item.custom.tooltip.MealSummaryTooltip;

import java.util.List;

public class ModularDelightClient implements ClientModInitializer {
    private static final int NO_TINT = -1;
    private static final int FALLBACK_COLOR = 0xE8E4D8;

    @Override
    public void onInitializeClient() {
        ModelPredicateProviderRegistry.register(ModItems.MODULAR_MEAL, ModularDelight.id("pattern"),
                (stack, world, entity, seed) -> ModularMealItem.getPatternModelIndex(stack));

        BlockRenderLayerMap.INSTANCE.putBlock(
                ModBlocks.STOCKPOT,
                RenderLayer.getCutout()
        );

        TooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof MealPropertyTooltip.Data d) return new MealPropertyTooltip.Component(d);
            if (data instanceof MealSummaryTooltip.Data d) return new MealSummaryTooltip.Component(d);
            if (data instanceof StockpotTooltip.Data d)
                return new StockpotTooltip.Component(d);
            return null;
        });

        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            if (tintIndex == 0) return NO_TINT;

            List<MealColor> tintColors = ModularMealItem.resolveTintColors(stack);
            int slot = tintIndex - 1;
            if (slot < 0 || slot >= tintColors.size()) return NO_TINT;

            MealColor color = tintColors.get(slot);
            return color != null ? color.rgb() : FALLBACK_COLOR;
        }, ModItems.MODULAR_MEAL);

        HandledScreens.register(ModScreenHandlers.STOCKPOT, StockpotScreen::new);
    }
}
