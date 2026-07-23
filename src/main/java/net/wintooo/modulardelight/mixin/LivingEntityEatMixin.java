package net.wintooo.modulardelight.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.wintooo.modulardelight.content.util.DigestionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityEatMixin {

    @Inject(method = "eatFood", at = @At("RETURN"))
    private void modulardelight$onEatFood(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (world.isClient) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayerEntity player) {
            DigestionManager.onEatFood(player, stack);
        }
    }
}