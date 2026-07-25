package net.wintooo.modulardelight.mixin;

import com.google.common.collect.Ordering;
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
import net.wintooo.modulardelight.mixin.client.HandledScreenAccessor;
import net.wintooo.modulardelight.mixin.client.ScreenAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Mixin(AbstractInventoryScreen.class)
public abstract class AbstractInventoryScreenMixin {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
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

        context.drawTooltip(textRenderer, modulardelight$buildTooltip(hovered, mealTooltip), data, mouseX, mouseY);
    }

    @SuppressWarnings("resource")
    @Inject(method = "drawStatusEffects", at = @At("TAIL"))
    private void modulardelight$drawWideDigestionTooltip(
            DrawContext context, int mouseX, int mouseY, CallbackInfo ci
    ) {
        AbstractInventoryScreen<?> self = (AbstractInventoryScreen<?>) (Object) this;
        MinecraftClient client = ((ScreenAccessor) self).modulardelight$getClient();
        if (client == null || client.player == null) return;

        Collection<StatusEffectInstance> collection = client.player.getStatusEffects();
        if (collection.isEmpty()) return;

        HandledScreenAccessor handled = (HandledScreenAccessor) self;
        ScreenAccessor screenAccessor = (ScreenAccessor) self;

        int screenX = handled.modulardelight$getX();
        int screenY = handled.modulardelight$getY();
        int backgroundWidth = handled.modulardelight$getBackgroundWidth();
        int screenWidth = ((ScreenAccessor) self).modulardelight$getWidth();
        TextRenderer textRenderer = screenAccessor.modulardelight$getTextRenderer();

        int barX = screenX + backgroundWidth + 2;
        int available = screenWidth - barX;
        if (available < 120) return;

        if (mouseX < barX || mouseX > barX + 120) return;

        int entryHeight = collection.size() > 5 ? 132 / (collection.size() - 1) : 33;
        Iterable<StatusEffectInstance> sorted = Ordering.natural().sortedCopy(collection);

        int lineY = screenY;
        StatusEffectInstance hovered = null;
        for (StatusEffectInstance instance : sorted) {
            if (mouseY >= lineY && mouseY <= lineY + entryHeight) {
                hovered = instance;
            }
            lineY += entryHeight;
        }

        if (hovered == null || hovered.getEffectType() != ModStatusEffects.DIGESTION) return;

        List<Text> mealTooltip = ClientDigestionManager.getActiveTooltip();
        if (mealTooltip.isEmpty()) return;

        context.drawTooltip(textRenderer, modulardelight$buildTooltip(hovered, mealTooltip), mouseX, mouseY);
    }

    @Unique
    private List<Text> modulardelight$buildTooltip(StatusEffectInstance hovered, List<Text> mealTooltip) {
        List<Text> finalTooltip = new ArrayList<>();
        finalTooltip.add(hovered.getEffectType().getName().copy().formatted(Formatting.WHITE));
        finalTooltip.add(Text.literal(""));
        finalTooltip.addAll(mealTooltip);
        finalTooltip.add(Text.literal(""));
        finalTooltip.add(StatusEffectUtil.getDurationText(hovered, 1.0F).copy().formatted(Formatting.WHITE));
        return finalTooltip;
    }
}