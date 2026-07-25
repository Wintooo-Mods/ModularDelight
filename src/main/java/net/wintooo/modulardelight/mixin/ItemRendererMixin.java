package net.wintooo.modulardelight.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.wintooo.modulardelight.content.item.ModItems;
import net.wintooo.modulardelight.content.item.custom.ModularMealItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
    @Inject(method = "getModel", at = @At("RETURN"), cancellable = true)
    private void modulardelight$useMealOverrideModel(
            ItemStack stack,
            World world,
            LivingEntity entity,
            int seed,
            CallbackInfoReturnable<BakedModel> cir
    ) {
        if (!stack.isOf(ModItems.MODULAR_MEAL)) return;

        Identifier modelId = ModularMealItem.getOverrideModel(stack);
        if (modelId == null) return;

        var modelManager = MinecraftClient.getInstance().getBakedModelManager();
        BakedModel model = modelManager.getModel(modelId);

        if (model != null) {
            cir.setReturnValue(model);
        }
    }
}