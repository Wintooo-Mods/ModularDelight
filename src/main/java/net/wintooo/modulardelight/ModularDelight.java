package net.wintooo.modulardelight;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;

import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import net.wintooo.modulardelight.content.block.ModBlockEntities;
import net.wintooo.modulardelight.content.block.ModBlocks;
import net.wintooo.modulardelight.content.command.ModDebugCommands;
import net.wintooo.modulardelight.content.data.MealColorLoader;
import net.wintooo.modulardelight.content.data.MealNameFilterLoader;
import net.wintooo.modulardelight.content.data.MealOverrideLoader;
import net.wintooo.modulardelight.content.data.PropertyLoader;
import net.wintooo.modulardelight.content.effect.ModStatusEffects;
import net.wintooo.modulardelight.content.effect.parsing.ActionTypeRegistry;
import net.wintooo.modulardelight.content.effect.parsing.AmbientReactionTypeRegistry;
import net.wintooo.modulardelight.content.effect.parsing.TriggerTypeRegistry;
import net.wintooo.modulardelight.content.item.ModCreativeMenu;
import net.wintooo.modulardelight.content.item.ModItems;
import net.wintooo.modulardelight.content.loot.ModLootFunctions;
import net.wintooo.modulardelight.content.screen.ModScreenHandlers;
import net.wintooo.modulardelight.content.util.DigestionManager;

public class ModularDelight implements ModInitializer {
    public static final String MOD_ID = "modulardelight";
    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @SuppressWarnings("UnstableApiUsage") // Fabric's Transfer API (item storage) is experimental type shift
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
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(PropertyLoader.INSTANCE);
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(MealNameFilterLoader.INSTANCE);
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(MealOverrideLoader.INSTANCE);
        ModCreativeMenu.load();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ModDebugCommands.register(dispatcher));
        ItemStorage.SIDED.registerForBlockEntity(
                InventoryStorage::of,
                ModBlockEntities.STOCKPOT
        );
    }
}