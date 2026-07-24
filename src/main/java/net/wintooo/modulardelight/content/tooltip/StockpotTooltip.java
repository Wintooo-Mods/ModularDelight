package net.wintooo.modulardelight.content.tooltip;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.item.TooltipData;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class StockpotTooltip {
    private StockpotTooltip() {}

    public record Data(ItemStack stock) implements TooltipData {}

    public record Component(Data data) implements TooltipComponent {
        private static final int ITEM_SIZE = 16;
        private static final int ITEM_NAME_GAP = 4;
        private static final int TEXT_SPACING = 10;
        private static final int TEXT_COLOR = 0xAAAAAA;

        @Override
        public int getHeight() {
            return data.stock().isEmpty()
                    ? TEXT_SPACING
                    : TEXT_SPACING + ITEM_SIZE;
        }

        @Override
        public int getWidth(TextRenderer textRenderer) {
            ItemStack stock = data.stock();
            if (stock.isEmpty()) {
                return textRenderer.getWidth(
                        Text.translatable("tooltip.modulardelight.stockpot.empty"));
            }
            return Math.max(
                    textRenderer.getWidth(countText(stock)),
                    textRenderer.getWidth(stock.getName()) + ITEM_SIZE + ITEM_NAME_GAP
            );
        }

        @Override
        public void drawItems(TextRenderer textRenderer, int x, int y, DrawContext context) {
            ItemStack stock = data.stock();
            if (stock.isEmpty()) {
                context.drawText(
                        textRenderer,
                        Text.translatable("tooltip.modulardelight.stockpot.empty"),
                        x, y,
                        TEXT_COLOR,
                        true
                );
                return;
            }
            context.drawText(
                    textRenderer,
                    countText(stock),
                    x, y,
                    TEXT_COLOR,
                    true
            );
            int itemY = y + TEXT_SPACING;
            context.drawItem(stock, x, itemY);
            context.drawText(
                    textRenderer,
                    stock.getName(),
                    x + ITEM_SIZE + ITEM_NAME_GAP,
                    itemY + 4,
                    0xFFFFFF,
                    true
            );
        }

        private static Text countText(ItemStack stock) {
            return stock.getCount() == 1
                    ? Text.translatable("tooltip.modulardelight.stockpot.single_serving")
                    : Text.translatable("tooltip.modulardelight.stockpot.many_servings", stock.getCount());
        }
    }
}