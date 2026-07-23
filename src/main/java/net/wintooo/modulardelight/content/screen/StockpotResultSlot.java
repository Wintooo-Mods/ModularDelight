package net.wintooo.modulardelight.content.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;
import net.wintooo.modulardelight.content.block.custom.entity.StockpotBlockEntity;

public class StockpotResultSlot extends Slot {
    public StockpotResultSlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return false;
    }

    @Override
    public void onTakeItem(PlayerEntity player, ItemStack stack) {
        super.onTakeItem(player, stack);

        if (!player.getWorld().isClient &&
                inventory instanceof StockpotBlockEntity stockpot) {

            stockpot.awardUsedRecipesAndPopExperience(
                    (ServerWorld) player.getWorld(),
                    player.getPos());
        }
    }
}