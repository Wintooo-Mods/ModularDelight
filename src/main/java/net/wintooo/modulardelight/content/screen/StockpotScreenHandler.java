package net.wintooo.modulardelight.content.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.wintooo.modulardelight.content.block.custom.entity.StockpotBlockEntity;

public class StockpotScreenHandler extends ScreenHandler {

    private static final int INV_START = StockpotBlockEntity.INVENTORY_SIZE;

    public final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public StockpotScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(StockpotBlockEntity.INVENTORY_SIZE), new ArrayPropertyDelegate(3));
    }

    public StockpotScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.STOCKPOT, syncId);
        checkSize(inventory, StockpotBlockEntity.INVENTORY_SIZE);
        checkDataCount(propertyDelegate, 3);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        inventory.onOpen(playerInventory.player);

        int inputStartX = 30;
        int inputY = 26;
        int spacing = 18;
        for (int i = 0; i < 3; i++) {
            this.addSlot(new Slot(inventory, StockpotBlockEntity.SLOT_AMBIENT + i,
                    inputStartX + (i * spacing), inputY) {
                @Override
                public boolean canInsert(ItemStack stack) {
                    return inventory.isValid(this.getIndex(), stack);
                }
            });
        }

        this.addSlot(new Slot(inventory, StockpotBlockEntity.SLOT_STOCK, 124, 26) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }

            @Override
            public boolean canTakeItems(PlayerEntity player) {
                return false;
            }
        });

        this.addSlot(new Slot(inventory, StockpotBlockEntity.SLOT_BOWL, 92, 55) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.BOWL);
            }
        });

        this.addSlot(new StockpotResultSlot(inventory, StockpotBlockEntity.SLOT_OUTPUT, 124, 55));

        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, 84 + row * 18));
            }
        }

        for (int column = 0; column < 9; ++column) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }

        this.addProperties(propertyDelegate);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack original;
        Slot slot = this.slots.get(index);

        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getStack();
        original = stack.copy();

        if (index < INV_START) {
            if (!this.insertItem(stack, INV_START, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (stack.isOf(Items.BOWL)) {
                if (!this.insertItem(stack,
                        StockpotBlockEntity.SLOT_BOWL,
                        StockpotBlockEntity.SLOT_BOWL + 1,
                        false)) {
                    return ItemStack.EMPTY;
                }
            }
            else if (!this.insertItem(stack,
                    StockpotBlockEntity.SLOT_AMBIENT,
                    StockpotBlockEntity.SLOT_STOCK,
                    false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        slot.onTakeItem(player, stack);
        return original;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.inventory.onClose(player);
    }

    public int getCookProgress() {
        int cookTime = this.propertyDelegate.get(0);
        int cookTimeTotal = this.propertyDelegate.get(1);
        return cookTimeTotal != 0 && cookTime != 0 ? cookTime * 24 / cookTimeTotal : 0;
    }

    public boolean isHeated() {
        return this.propertyDelegate.get(2) != 0;
    }
}