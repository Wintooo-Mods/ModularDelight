package net.wintooo.modulardelight.mixin;

import net.minecraft.client.item.TooltipData;
import net.minecraft.item.ItemStack;
import net.wintooo.modulardelight.content.item.custom.MealProperty;
import net.wintooo.modulardelight.content.item.custom.tooltip.MealPropertyTooltip;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(ItemStack.class)
public class ItemStackTooltipDataMixin {
    @Inject(method = "getTooltipData", at = @At("RETURN"), cancellable = true)
    private void modulardelight$appendPropertyTooltip(CallbackInfoReturnable<Optional<TooltipData>> cir) {
        if (cir.getReturnValue().isPresent()) return;
        ItemStack self = (ItemStack) (Object) this;
        List<MealProperty> matched = MealProperty.all().stream()
                .filter(p -> self.isIn(p.tag()))
                .toList();
        if (!matched.isEmpty()) {
            cir.setReturnValue(Optional.of(new MealPropertyTooltip.Data(matched)));
        }
    }
}