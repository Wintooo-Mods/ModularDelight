package net.wintooo.modulardelight;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.wintooo.modulardelight.content.block.ModBlocks;
import net.wintooo.modulardelight.content.input.ModKeyBindings;
import net.wintooo.modulardelight.content.item.ModItems;
import net.wintooo.modulardelight.content.item.custom.ModularMealItem;
import net.wintooo.modulardelight.content.network.ModClientNetworking;
import net.wintooo.modulardelight.content.screen.ModScreenHandlers;
import net.wintooo.modulardelight.content.screen.StockpotScreen;
import net.wintooo.modulardelight.content.tooltip.IngredientPropertyTooltip;
import net.wintooo.modulardelight.content.tooltip.MealSummaryTooltip;
import net.wintooo.modulardelight.content.tooltip.StockpotTooltip;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ModularDelightClient implements ClientModInitializer {
    private static final int NO_TINT = -1;

    @Override
    public void onInitializeClient() {
        ModClientNetworking.register();
        ModKeyBindings.register();

        ModelPredicateProviderRegistry.register(ModItems.MODULAR_MEAL, ModularDelight.id("pattern"),
                (stack, world, entity, seed) -> ModularMealItem.getPatternModelIndex(stack));

        ModelLoadingPlugin.register(context -> {
            ResourceManager resources = MinecraftClient.getInstance().getResourceManager();

            Set<Identifier> models = resources.findResources(
                    "models/item/modular_meal",
                    id -> id.getPath().endsWith(".json")
            ).keySet().stream().map(fileId -> {
                String path = fileId.getPath()
                        .substring("models/".length())
                        .replaceFirst("\\.json$", "");

                return new Identifier(fileId.getNamespace(), path);
            }).collect(Collectors.toSet());

            context.addModels(models);
        });

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.STOCKPOT, RenderLayer.getCutout());

        TooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof IngredientPropertyTooltip.Data d) return new IngredientPropertyTooltip.Component(d);
            if (data instanceof MealSummaryTooltip.Data d) return new MealSummaryTooltip.Component(d);
            if (data instanceof StockpotTooltip.Data d) return new StockpotTooltip.Component(d);
            return null;
        });

        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            if (tintIndex == 0) return NO_TINT;

            List<Integer> tintColors = ModularMealItem.resolveTintColors(stack);
            int slot = tintIndex - 1;
            return slot >= 0 && slot < tintColors.size() ? tintColors.get(slot) : NO_TINT;
        }, ModItems.MODULAR_MEAL);

        HandledScreens.register(ModScreenHandlers.STOCKPOT, StockpotScreen::new);
    }
}