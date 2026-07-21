package net.wintooo.modulardelight.content.item.custom.tooltip;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.item.TooltipData;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

public final class MealSummaryTooltip {
    private MealSummaryTooltip() {}

    public record Data(List<Text> descriptionLines, List<ItemStack> ingredients) implements TooltipData {}

    public record Component(Data data) implements TooltipComponent {
        private static final int TEXT_LINE_HEIGHT = 10;
        private static final int TEXT_ICON_GAP = 1;
        private static final int SLOT_SIZE = 18;
        private static final int BOTTOM_PADDING = 3;
        private static final int TEXT_COLOR = 0xAAAAAA;

        @Override
        public int getHeight() {
            int textHeight = data.descriptionLines().isEmpty() ? 0
                    : data.descriptionLines().size() * TEXT_LINE_HEIGHT + TEXT_ICON_GAP;
            return textHeight + SLOT_SIZE + BOTTOM_PADDING;
        }

        @Override
        public int getWidth(TextRenderer textRenderer) {
            int textWidth = 0;
            for (Text line : data.descriptionLines()) {
                textWidth = Math.max(textWidth, textRenderer.getWidth(line));
            }
            return Math.max(textWidth, data.ingredients().size() * SLOT_SIZE);
        }

        @Override
        public void drawItems(TextRenderer textRenderer, int x, int y, DrawContext context) {
            int lineY = y;
            for (Text line : data.descriptionLines()) {
                context.drawText(textRenderer, line, x, lineY, TEXT_COLOR, false);
                lineY += TEXT_LINE_HEIGHT;
            }
            if (!data.descriptionLines().isEmpty()) lineY += TEXT_ICON_GAP;

            int slotX = x;
            for (ItemStack ingredient : data.ingredients()) {
                context.drawItem(ingredient, slotX, lineY);
                slotX += SLOT_SIZE;
            }
        }
    }
}