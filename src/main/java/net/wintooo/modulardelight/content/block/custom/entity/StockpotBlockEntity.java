package net.wintooo.modulardelight.content.block.custom.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ViewerCountManager;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.wintooo.modulardelight.content.block.custom.StockpotBlock;
import net.wintooo.modulardelight.content.item.custom.MealEffect;
import net.wintooo.modulardelight.content.item.custom.MealProperty;
import net.wintooo.modulardelight.content.item.custom.ModularMealItem;
import net.wintooo.modulardelight.content.screen.StockpotScreenHandler;
import org.jetbrains.annotations.Nullable;
import vectorwing.farmersdelight.common.block.entity.HeatableBlockEntity;

import java.util.ArrayList;
import java.util.List;

public class StockpotBlockEntity extends BlockEntity implements SidedInventory, NamedScreenHandlerFactory, HeatableBlockEntity {

    public static final int SLOT_AMBIENT = 0;
    public static final int SLOT_CONDITION = 1;
    public static final int SLOT_ACTIVATED = 2;
    public static final int SLOT_STOCK = 3;
    public static final int SLOT_BOWL = 4;
    public static final int SLOT_OUTPUT = 5;
    public static final int INVENTORY_SIZE = 6;
    public static final int MAX_STOCK_SERVINGS = 64;
    private static final int COOK_TIME_TOTAL = 400;
    private float storedExperience = 0.0F;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);

    private int cookTime;
    private int cookTimeTotal = COOK_TIME_TOTAL;
    @Nullable
    private Text customName;


    private final ViewerCountManager viewerCountManager = new ViewerCountManager() {
        @Override
        protected void onContainerOpen(World world, BlockPos pos, BlockState state) {
            setOpen(state, true);
        }

        @Override
        protected void onContainerClose(World world, BlockPos pos, BlockState state) {
            setOpen(state, false);
        }

        @Override
        protected void onViewerCountUpdate(World world, BlockPos pos, BlockState state, int oldViewerCount, int newViewerCount) {
            world.emitGameEvent(null, newViewerCount > oldViewerCount ? GameEvent.CONTAINER_OPEN : GameEvent.CONTAINER_CLOSE, pos);
        }

        @Override
        protected boolean isPlayerViewing(PlayerEntity player) {
            if (player.currentScreenHandler instanceof StockpotScreenHandler handler) {
                Inventory inv = handler.inventory;
                return inv == StockpotBlockEntity.this;
            }
            return false;
        }
    };

    private boolean heated;

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> cookTime;
                case 1 -> cookTimeTotal;
                case 2 -> heated ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> cookTime = value;
                case 1 -> cookTimeTotal = value;
                case 2 -> heated = value != 0;
            }
        }

        @Override
        public int size() {
            return 3;
        }
    };

    public StockpotBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STOCKPOT, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, StockpotBlockEntity entity) {
        boolean dirty = false;
        boolean heated = entity.isHeated(world, pos);
        entity.heated = heated;

        if (heated && entity.hasAllIngredients() && entity.canAccumulateStock()) {
            entity.cookTime++;
            if (entity.cookTime >= entity.cookTimeTotal) {
                entity.craft();
                entity.cookTime = 0;
                dirty = true;
            }
        } else if (entity.cookTime > 0) {
            entity.cookTime = Math.max(0, entity.cookTime - 2);
        }

        if (entity.serveStockIntoOutput()) {
            dirty = true;
        }

        entity.viewerCountManager.updateViewerCount(world, pos, state);

        if (dirty) {
            entity.markDirty();
        }
    }

    private void setOpen(BlockState state, boolean open) {
        if (this.world != null && state.get(StockpotBlock.OPEN) != open) {
            this.world.setBlockState(this.pos, state.with(StockpotBlock.OPEN, open), Block_NOTIFY_ALL());
        }
    }

    private static int Block_NOTIFY_ALL() {
        return net.minecraft.block.Block.NOTIFY_ALL;
    }

    @Override
    public void onOpen(PlayerEntity player) {
        if (!player.isSpectator() && this.world != null) {
            this.viewerCountManager.openContainer(player, this.world, this.pos, this.getCachedState());
        }
    }

    @Override
    public void onClose(PlayerEntity player) {
        if (!player.isSpectator() && this.world != null) {
            this.viewerCountManager.closeContainer(player, this.world, this.pos, this.getCachedState());
        }
    }

    public static void animationTick(World world, BlockPos pos, StockpotBlockEntity entity) {
        if (!entity.isHeated(world, pos)) return;
        var random = world.random;
        if (random.nextFloat() < 0.2F) {
            double x = pos.getX() + 0.5D + (random.nextDouble() * 0.6D - 0.3D);
            double y = pos.getY() + 0.8D;
            double z = pos.getZ() + 0.5D + (random.nextDouble() * 0.6D - 0.3D);
            world.addParticle(net.minecraft.particle.ParticleTypes.BUBBLE_POP, x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }

    private boolean hasAllIngredients() {
        return isValidIngredientForSlot(SLOT_AMBIENT, inventory.get(SLOT_AMBIENT))
                && isValidIngredientForSlot(SLOT_CONDITION, inventory.get(SLOT_CONDITION))
                && isValidIngredientForSlot(SLOT_ACTIVATED, inventory.get(SLOT_ACTIVATED));
    }

    private boolean canAccumulateStock() {
        ItemStack prospective = computeResult();
        if (prospective.isEmpty()) return false;

        ItemStack stock = inventory.get(SLOT_STOCK);
        if (stock.isEmpty()) return true;

        if (!ItemStack.canCombine(stock, prospective)) {
            return false;
        }

        return stock.getCount() < MAX_STOCK_SERVINGS;
    }

    private static final float EXPERIENCE_PER_CRAFT = 0.35F;

    private ItemStack computeResult() {
        List<ItemStack> ingredients = new ArrayList<>(3);
        for (int i = SLOT_AMBIENT; i <= SLOT_ACTIVATED; i++) {
            ItemStack stack = inventory.get(i);
            if (!isValidIngredientForSlot(i, stack)) return ItemStack.EMPTY;
            ingredients.add(stack.copyWithCount(1));
        }
        return ModularMealItem.create(ingredients);
    }

    private void craft() {
        ItemStack result = computeResult();
        if (result.isEmpty()) {
            return;
        }

        storedExperience += EXPERIENCE_PER_CRAFT;

        ItemStack stock = inventory.get(SLOT_STOCK);
        if (stock.isEmpty()) {
            inventory.set(SLOT_STOCK, result);
        } else {
            stock.increment(result.getCount());
        }

        for (int slot = SLOT_AMBIENT; slot <= SLOT_ACTIVATED; slot++) {
            ItemStack ingredient = inventory.get(slot);
            if (ingredient.isEmpty()) continue;

            ItemStack consumed = ingredient.copyWithCount(1);
            ItemStack remainder = consumed.getRecipeRemainder();
            ingredient.decrement(1);

            if (!remainder.isEmpty() && world != null) {
                ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, remainder);
            }
        }

        markDirty();
    }

    private boolean serveStockIntoOutput() {
        ItemStack stock = inventory.get(SLOT_STOCK);
        ItemStack bowls = inventory.get(SLOT_BOWL);
        if (stock.isEmpty() || bowls.isEmpty()) return false;

        ItemStack output = inventory.get(SLOT_OUTPUT);
        if (!output.isEmpty() && !ItemStack.canCombine(output, stock)) {
            return false;
        }

        int space = output.isEmpty() ? MAX_STOCK_SERVINGS : output.getMaxCount() - output.getCount();
        int amount = Math.min(Math.min(stock.getCount(), bowls.getCount()), space);
        if (amount <= 0) return false;

        bowls.decrement(amount);
        if (output.isEmpty()) {
            inventory.set(SLOT_OUTPUT, stock.split(amount));
        } else {
            stock.decrement(amount);
            output.increment(amount);
        }
        return true;
    }

    public ItemStack useStockOnHeldItem(ItemStack heldStack) {
        ItemStack stock = inventory.get(SLOT_STOCK);
        if (!heldStack.isEmpty() && heldStack.isOf(Items.BOWL) && !stock.isEmpty()) {
            heldStack.decrement(1);
            markDirty();
            return stock.split(1);
        }
        return ItemStack.EMPTY;
    }

    public boolean isHeated() {
        return this.world != null && this.isHeated(this.world, this.pos);
    }

    public ItemStack getStock() {
        return inventory.get(SLOT_STOCK);
    }

    public static ItemStack getStockFromItem(ItemStack stack) {
        NbtCompound tag = stack.getSubNbt("BlockEntityTag");
        if (tag == null || !tag.contains("Stock", NbtElement.COMPOUND_TYPE)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.fromNbt(tag.getCompound("Stock"));
    }

    public NbtCompound writeStock(NbtCompound tag) {
        ItemStack stock = inventory.get(SLOT_STOCK);
        if (!stock.isEmpty()) {
            tag.put("Stock", stock.writeNbt(new NbtCompound()));
        }
        return tag;
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        inventory.clear();
        Inventories.readNbt(nbt, inventory);
        storedExperience = nbt.getFloat("Experience");
        cookTime = nbt.getInt("CookTime");
        cookTimeTotal = nbt.getInt("CookTimeTotal");
        if (nbt.contains("CustomName", net.minecraft.nbt.NbtElement.STRING_TYPE)) {
            customName = Text.Serializer.fromJson(nbt.getString("CustomName"));
        }
        if (nbt.contains("Stock", net.minecraft.nbt.NbtElement.COMPOUND_TYPE)) {
            inventory.set(SLOT_STOCK, ItemStack.fromNbt(nbt.getCompound("Stock")));
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, inventory);
        nbt.putFloat("Experience", storedExperience);
        nbt.putInt("CookTime", cookTime);
        nbt.putInt("CookTimeTotal", cookTimeTotal);
        if (customName != null) {
            nbt.putString("CustomName", Text.Serializer.toJson(customName));
        }
    }

    public void setCustomName(@Nullable Text name) {
        this.customName = name;
    }

    @Override
    public int size() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(inventory, slot, amount);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(inventory, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        if (stack.getCount() > stack.getMaxCount()) {
            stack.setCount(stack.getMaxCount());
        }
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (this.world == null || this.world.getBlockEntity(this.pos) != this) return false;
        return player.squaredDistanceTo(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clear() {
        inventory.clear();
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_AMBIENT, SLOT_CONDITION, SLOT_ACTIVATED -> isValidIngredientForSlot(slot, stack);
            case SLOT_BOWL -> stack.isOf(Items.BOWL);
            default -> false;
        };
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.DOWN) {
            return new int[]{SLOT_OUTPUT};
        }
        return new int[]{SLOT_AMBIENT, SLOT_CONDITION, SLOT_ACTIVATED, SLOT_BOWL};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return isValid(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == SLOT_OUTPUT;
    }

    private static MealEffect resolveEffect(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Identifier id = Registries.ITEM.getId(stack.getItem());
        MealProperty property = ModularMealItem.resolveProperty(id);
        return property == null ? null : MealEffect.byProperty(property);
    }

    public static boolean isValidAmbientIngredient(ItemStack stack) {
        MealEffect effect = resolveEffect(stack);
        if (effect == null) return false;
        return !effect.ambientAttributes().isEmpty()
                || !effect.ambientStatusEffects().isEmpty()
                || !effect.ambientDamageReactions().isEmpty()
                || effect.ambientAlwaysEdible();
    }

    public static boolean isValidConditionIngredient(ItemStack stack) {
        MealEffect effect = resolveEffect(stack);
        return effect != null
                && (effect.tickTrigger() != null || effect.damageTrigger() != null
                || effect.attackTrigger() != null || effect.eatTrigger() != null);
    }

    public static boolean isValidActivatedIngredient(ItemStack stack) {
        MealEffect effect = resolveEffect(stack);
        return effect != null && effect.activatedAction() != null;
    }

    public static boolean isValidIngredientForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_AMBIENT -> isValidAmbientIngredient(stack);
            case SLOT_CONDITION -> isValidConditionIngredient(stack);
            case SLOT_ACTIVATED -> isValidActivatedIngredient(stack);
            default -> false;
        };
    }

    @Override
    public Text getDisplayName() {
        return customName != null ? customName : Text.translatable("container.modulardelight.stockpot");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new StockpotScreenHandler(syncId, playerInventory, this, propertyDelegate);
    }

    public ItemStack takeStockForDrop() {
        ItemStack stock = inventory.get(SLOT_STOCK);
        inventory.set(SLOT_STOCK, ItemStack.EMPTY);
        return stock;
    }

    public void restoreStockAfterDrop(ItemStack stock) {
        inventory.set(SLOT_STOCK, stock);
    }

    public void awardUsedRecipesAndPopExperience(ServerWorld world, Vec3d pos) {
        int xp = MathHelper.floor(storedExperience);
        float fractional = storedExperience - xp;

        if (world.random.nextFloat() < fractional) {
            xp++;
        }

        if (xp > 0) {
            ExperienceOrbEntity.spawn(world, pos, xp);
        }

        storedExperience = 0.0F;
        markDirty();
    }
}