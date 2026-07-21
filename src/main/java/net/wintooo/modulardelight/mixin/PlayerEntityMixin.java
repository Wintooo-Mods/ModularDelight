package net.wintooo.modulardelight.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.wintooo.modulardelight.content.util.DigestionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void modulardelight$writeDigestion(NbtCompound nbt, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            DigestionManager.writeToNbt(player, nbt);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void modulardelight$readDigestion(NbtCompound nbt, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            DigestionManager.readFromNbt(player, nbt);
        }
    }
}