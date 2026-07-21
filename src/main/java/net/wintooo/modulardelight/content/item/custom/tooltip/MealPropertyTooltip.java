package net.wintooo.modulardelight.content.item.custom.tooltip;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.item.TooltipData;
import net.minecraft.text.Text;
import net.wintooo.modulardelight.content.item.custom.MealProperty;

import java.util.List;

public final class MealPropertyTooltip {
    private MealPropertyTooltip() {}

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
                Text text = Text.translatable(property.translationKey());
                max = Math.max(max, ICON_SIZE + ICON_TEXT_GAP + textRenderer.getWidth(text));
            }
            return max;
        }

        @Override
        public void drawItems(TextRenderer textRenderer, int x, int y, DrawContext context) {
            int lineY = y;
            for (MealProperty property : data.properties()) {
                context.drawTexture(property.icon(), x, lineY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
                context.drawText(textRenderer, Text.translatable(property.translationKey()),
                        x + ICON_SIZE + ICON_TEXT_GAP, lineY + 1, 0xCF856E, false);
                lineY += LINE_HEIGHT;
            }
        }
    }
}