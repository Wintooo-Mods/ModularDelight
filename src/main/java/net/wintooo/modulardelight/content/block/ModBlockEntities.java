package net.wintooo.modulardelight.content.block;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.wintooo.modulardelight.ModularDelight;
import net.wintooo.modulardelight.content.block.custom.entity.StockpotBlockEntity;

public class ModBlockEntities {
    public static final BlockEntityType<StockpotBlockEntity> STOCKPOT = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            ModularDelight.id("stockpot"),
            FabricBlockEntityTypeBuilder.create(StockpotBlockEntity::new, ModBlocks.STOCKPOT).build()
    );

    public static void load() {}
}