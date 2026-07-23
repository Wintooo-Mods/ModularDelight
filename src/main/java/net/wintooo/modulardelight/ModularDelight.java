package net.wintooo.modulardelight;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;

import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import net.wintooo.modulardelight.content.block.ModBlocks;
import net.wintooo.modulardelight.content.block.custom.entity.ModBlockEntities;
import net.wintooo.modulardelight.content.command.ModDebugCommands;
import net.wintooo.modulardelight.content.data.*;
import net.wintooo.modulardelight.content.screen.ModScreenHandlers;
import net.wintooo.modulardelight.content.loot.ModLootFunctions;
import net.wintooo.modulardelight.content.util.DigestionManager;
import net.wintooo.modulardelight.content.effect.ModStatusEffects;
import net.wintooo.modulardelight.content.item.ModItems;

public class ModularDelight implements ModInitializer {
    public static final String MOD_ID = "modulardelight";
    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModItems.load();
        ModBlocks.load();
        ModBlockEntities.load();
        ModScreenHandlers.load();
        ModStatusEffects.load();
        ModLootFunctions.load();
        DigestionManager.register();
        TriggerTypeRegistry.bootstrap();
        ActionTypeRegistry.bootstrap();
        AmbientReactionTypeRegistry.bootstrap();
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(MealColorLoader.INSTANCE);
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(MealPropertyLoader.INSTANCE);
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(MealNameFilterLoader.INSTANCE);
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(MealOverrideLoader.INSTANCE);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ModDebugCommands.register(dispatcher));

        ItemStorage.SIDED.registerForBlockEntity(
                InventoryStorage::of,
                ModBlockEntities.STOCKPOT
        );
    }
}