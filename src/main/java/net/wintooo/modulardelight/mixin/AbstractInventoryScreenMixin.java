package net.wintooo.modulardelight.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.item.TooltipData;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.wintooo.modulardelight.content.effect.ModStatusEffects;
import net.wintooo.modulardelight.content.util.ClientDigestionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(AbstractInventoryScreen.class)
public abstract class AbstractInventoryScreenMixin {

    @ModifyVariable(method = "drawStatusEffects", at = @At("STORE"), ordinal = 0)
    private boolean modulardelight$forceCompactForDigestion(boolean wide) {
        MinecraftClient client = ((ScreenAccessor) this).modulardelight$getClient();
        return wide && (client.player == null
                || !client.player.hasStatusEffect(ModStatusEffects.DIGESTION));
    }

    @Redirect(
            method = "drawStatusEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;II)V"
            )
    )
    private void modulardelight$replaceDigestionTooltip(
            DrawContext context,
            TextRenderer textRenderer,
            List<Text> list,
            Optional<TooltipData> data,
            int mouseX,
            int mouseY,
            @Local StatusEffectInstance hovered
    ) {
        List<Text> mealTooltip;
        if (hovered.getEffectType() != ModStatusEffects.DIGESTION
                || (mealTooltip = ClientDigestionManager.getActiveTooltip()).isEmpty()) {
            context.drawTooltip(textRenderer, list, data, mouseX, mouseY);
            return;
        }

        List<Text> finalTooltip = new ArrayList<>();
        finalTooltip.add(list.get(0).copy().formatted(Formatting.WHITE));
        finalTooltip.add(Text.literal(""));
        finalTooltip.addAll(mealTooltip);
        finalTooltip.add(Text.literal(""));
        finalTooltip.add(StatusEffectUtil.getDurationText(hovered, 1.0F).copy().formatted(Formatting.WHITE));

        context.drawTooltip(textRenderer, finalTooltip, data, mouseX, mouseY);
    }
}