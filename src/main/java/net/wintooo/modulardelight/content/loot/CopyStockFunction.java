package net.wintooo.modulardelight.content.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.nbt.NbtCompound;
import net.wintooo.modulardelight.content.block.custom.entity.StockpotBlockEntity;

public class CopyStockFunction extends ConditionalLootFunction {

    protected CopyStockFunction(LootCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ItemStack process(ItemStack stack, LootContext context) {
        BlockEntity blockEntity = context.get(LootContextParameters.BLOCK_ENTITY);
        if (blockEntity instanceof StockpotBlockEntity stockpot) {
            NbtCompound tag = stockpot.writeStock(new NbtCompound());
            if (!tag.isEmpty()) {
                stack.setSubNbt("BlockEntityTag", tag);
            }
        }
        return stack;
    }

    @Override
    public LootFunctionType getType() {
        return ModLootFunctions.COPY_MEAL;
    }

    public static class Serializer extends ConditionalLootFunction.Serializer<CopyStockFunction> {
        @Override
        public CopyStockFunction fromJson(JsonObject json, JsonDeserializationContext context, LootCondition[] conditions) {
            return new CopyStockFunction(conditions);
        }
    }
}