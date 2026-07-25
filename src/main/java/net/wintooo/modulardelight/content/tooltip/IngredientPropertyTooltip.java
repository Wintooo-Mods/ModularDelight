package net.wintooo.modulardelight.content.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.item.TooltipData;
import net.minecraft.text.Text;
import net.wintooo.modulardelight.content.meal.MealProperty;
import net.wintooo.modulardelight.content.options.AccessibilityHelper;

import java.util.List;

public final class IngredientPropertyTooltip {
    private IngredientPropertyTooltip() {}

    public record Data(List<MealProperty> properties) implements TooltipData {}

    public record Component(Data data) implements TooltipComponent {
        private static final int ICON_SIZE = 9;
        private static final int LINE_HEIGHT = 10;
        private static final int ICON_TEXT_GAP = 4;
        private static final int BOTTOM_PADDING = 3;

        @Override
        public int getHeight() {
            return data.properties().size() * LINE_HEIGHT + BOTTOM_PADDING;
        }

        @Override
        public int getWidth(TextRenderer textRenderer) {
            int max = 0;
            for (MealProperty property : data.properties()) {
                Text text = property.name();
                max = Math.max(max, ICON_SIZE + ICON_TEXT_GAP + textRenderer.getWidth(text));
            }
            return max;
        }

        @Override
        public void drawItems(TextRenderer textRenderer, int x, int y, DrawContext context) {
            int lineY = y;
            boolean useColors = AccessibilityHelper.usePropertyColors();

            for (MealProperty property : data.properties()) {
                if (useColors) {
                    float red = ((property.color() >> 16) & 0xFF) / 255.0f;
                    float green = ((property.color() >> 8) & 0xFF) / 255.0f;
                    float blue = (property.color() & 0xFF) / 255.0f;
                    RenderSystem.setShaderColor(red, green, blue, 1.0f);
                } else {
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                }

                context.drawTexture(
                        property.icon(),
                        x,
                        lineY,
                        0,
                        0,
                        ICON_SIZE,
                        ICON_SIZE,
                        ICON_SIZE,
                        ICON_SIZE
                );

                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

                context.drawText(
                        textRenderer,
                        property.name(),
                        x + ICON_SIZE + ICON_TEXT_GAP,
                        lineY + 1,
                        useColors ? property.color() : 0xFFFFFF,
                        false
                );

                lineY += LINE_HEIGHT;
            }
        }
    }
}
