package net.wintooo.modulardelight.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.wintooo.modulardelight.content.util.DigestionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityAttackMixin {

    @Inject(method = "applyDamage", at = @At("TAIL"))
    private void modulardelight$onApplyDamage(DamageSource source, float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (source.getAttacker() instanceof ServerPlayerEntity attacker) {
            DigestionManager.onAttack(attacker, self, amount);
        }
    }
}