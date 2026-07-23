package net.wintooo.modulardelight.content.block.custom;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.wintooo.modulardelight.content.block.custom.entity.ModBlockEntities;
import net.wintooo.modulardelight.content.block.custom.entity.StockpotBlockEntity;

import org.jetbrains.annotations.Nullable;
import vectorwing.farmersdelight.common.block.state.CookingPotSupport;
import vectorwing.farmersdelight.common.registry.ModSounds;

import static vectorwing.farmersdelight.common.tag.ModTags.HEAT_SOURCES;

@SuppressWarnings({"deprecation", "unchecked"})
public class StockpotBlock extends Block implements Waterloggable, BlockEntityProvider {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final EnumProperty<CookingPotSupport> SUPPORT = EnumProperty.of("support", CookingPotSupport.class);
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    public static final BooleanProperty OPEN = BooleanProperty.of("open");

    private static final VoxelShape BASE_SHAPE = Block.createCuboidShape(1.0D, 0.0D, 1.0D, 15.0D, 12.0D, 15.0D);
    private static final VoxelShape LID_SHAPE = Block.createCuboidShape(1.0D, 12.0D, 1.0D, 15.0D, 14.0D, 15.0D);
    private static final VoxelShape TRAY_SHAPE = Block.createCuboidShape(0.0D, -1.0D, 0.0D, 16.0D, 0.0D, 16.0D);

    private static final VoxelShape OPEN_SHAPE =
            VoxelShapes.union(BASE_SHAPE);
    private static final VoxelShape CLOSED_SHAPE =
            VoxelShapes.union(BASE_SHAPE, LID_SHAPE);
    private static final VoxelShape OPEN_WITH_TRAY_SHAPE =
            VoxelShapes.union(OPEN_SHAPE, TRAY_SHAPE);
    private static final VoxelShape CLOSED_WITH_TRAY_SHAPE =
            VoxelShapes.union(CLOSED_SHAPE, TRAY_SHAPE);

    public StockpotBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(SUPPORT, CookingPotSupport.NONE)
                .with(WATERLOGGED, false)
                .with(OPEN, false));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack heldStack = player.getStackInHand(hand);
        if (heldStack.isEmpty() && player.isSneaking()) {
            world.setBlockState(pos, state.with(SUPPORT, state.get(SUPPORT) == CookingPotSupport.HANDLE
                    ? getTraySupport(world, pos) : CookingPotSupport.HANDLE));
            world.playSound(null, pos, SoundEvents.BLOCK_LANTERN_PLACE, SoundCategory.BLOCKS, 0.7F, 1.0F);
            return ActionResult.SUCCESS;
        }

        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof StockpotBlockEntity stockpot) {
                ItemStack servingStack = stockpot.useStockOnHeldItem(heldStack);
                if (!servingStack.isEmpty()) {
                    if (!player.getInventory().insertStack(servingStack)) {
                        player.dropItem(servingStack, false);
                    }
                    world.playSound(null, pos, SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, SoundCategory.BLOCKS, 1.0F, 1.0F);
                } else {
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        serverPlayer.openHandledScreen(stockpot);
                    }
                }
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        boolean closed = !state.get(OPEN);

        return closed
                ? CLOSED_SHAPE
                : OPEN_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        boolean closed = !state.get(OPEN);
        boolean tray = state.get(SUPPORT) == CookingPotSupport.TRAY;

        if (tray) {
            return closed
                    ? CLOSED_WITH_TRAY_SHAPE
                    : OPEN_WITH_TRAY_SHAPE;
        }

        return closed
                ? CLOSED_SHAPE
                : OPEN_SHAPE;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        BlockPos pos = context.getBlockPos();
        World world = context.getWorld();
        FluidState fluid = world.getFluidState(pos);

        BlockState state = this.getDefaultState()
                .with(FACING, context.getHorizontalPlayerFacing().getOpposite())
                .with(WATERLOGGED, fluid.getFluid() == Fluids.WATER);

        if (context.getSide() == Direction.DOWN) {
            return state.with(SUPPORT, CookingPotSupport.HANDLE);
        }
        return state.with(SUPPORT, getTraySupport(world, pos));
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        if (direction.getAxis() == Direction.Axis.Y && state.get(SUPPORT) != CookingPotSupport.HANDLE) {
            return state.with(SUPPORT, getTraySupport(world, pos));
        }
        return state;
    }

    private CookingPotSupport getTraySupport(WorldView world, BlockPos pos) {
        BlockPos belowPos = pos.down();
        BlockState below = world.getBlockState(belowPos);
        if (!below.isIn(HEAT_SOURCES)) {
            return CookingPotSupport.NONE;
        }
        boolean fullBlock = Block.isShapeFullCube(below.getCollisionShape(world, belowPos));
        return fullBlock ? CookingPotSupport.NONE : CookingPotSupport.TRAY;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (stack.hasCustomName()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof StockpotBlockEntity stockpot) {
                stockpot.setCustomName(stack.getName());
            }
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);

            if (blockEntity instanceof StockpotBlockEntity stockpot) {

                if (!world.isClient) {
                    stockpot.awardUsedRecipesAndPopExperience(
                            (ServerWorld)world,
                            Vec3d.ofCenter(pos));
                }

                ItemStack stock = stockpot.takeStockForDrop();
                ItemScatterer.spawn(world, pos, stockpot);
                stockpot.restoreStockAfterDrop(stock);

                world.updateComparators(pos, this);
            }

            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING, SUPPORT, WATERLOGGED, OPEN);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof StockpotBlockEntity stockpot && stockpot.isHeated()) {
            SoundEvent boilSound = !stockpot.getStock().isEmpty() ? ModSounds.BLOCK_COOKING_POT_BOIL_SOUP.get() : ModSounds.BLOCK_COOKING_POT_BOIL.get();
            double x = (double)pos.getX() + (double)0.5F;
            double y = pos.getY();
            double z = (double)pos.getZ() + (double)0.5F;
            if (random.nextInt(10) == 0) {
                world.playSound(x, y, z, boilSound, SoundCategory.BLOCKS, 0.5F, random.nextFloat() * 0.2F + 0.9F, false);
            }
        }
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof StockpotBlockEntity stockpot) {
            return net.minecraft.screen.ScreenHandler.calculateComparatorOutput((BlockEntity) stockpot);
        }
        return 0;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new StockpotBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient) {
            return validateTicker(type, ModBlockEntities.STOCKPOT, (world1, pos, state1, entity) -> StockpotBlockEntity.animationTick(world1, pos, entity));
        }
        return validateTicker(type, ModBlockEntities.STOCKPOT, StockpotBlockEntity::tick);
    }

    @Nullable
    private static <A extends BlockEntity, E extends BlockEntity> BlockEntityTicker<A> validateTicker(
            BlockEntityType<A> givenType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == givenType ? (BlockEntityTicker<A>) ticker : null;
    }
}