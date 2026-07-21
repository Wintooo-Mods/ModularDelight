package net.wintooo.modulardelight.content.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.wintooo.modulardelight.ModularDelight;

public class StockpotScreen extends HandledScreen<StockpotScreenHandler> {

    private static final Identifier TEXTURE = ModularDelight.id("textures/gui/stockpot.png");

    private static final int HEAT_ICON_X = 48;
    private static final int HEAT_ICON_Y = 47;
    private static final int HEAT_ICON_W = 17;
    private static final int HEAT_ICON_H = 15;

    private static final int ARROW_X = 89;
    private static final int ARROW_Y = 25;
    private static final int ARROW_H = 17;

    public StockpotScreen(StockpotScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();

        this.titleX = 57 - this.textRenderer.getWidth(this.title) / 2;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        this.renderBackground(context);

        int x = this.x;
        int y = this.y;

        context.drawTexture(TEXTURE, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight);

        if (this.handler.isHeated()) {
            context.drawTexture(TEXTURE, x + HEAT_ICON_X, y + HEAT_ICON_Y, 176, 0, HEAT_ICON_W, HEAT_ICON_H);
        }

        int progress = this.handler.getCookProgress();
        if (progress > 0) {
            context.drawTexture(TEXTURE, x + ARROW_X, y + ARROW_Y, 176, 15, progress + 1, ARROW_H);
        }
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
        super.drawMouseoverTooltip(context, mouseX, mouseY);

        if (this.isPointWithinBounds(HEAT_ICON_X, HEAT_ICON_Y, HEAT_ICON_W, HEAT_ICON_H, mouseX, mouseY)) {
            Text tooltip = this.handler.isHeated()
                    ? Text.translatable("tooltip.modulardelight.stockpot.heated")
                    : Text.translatable("tooltip.modulardelight.stockpot.needs_heat");
            context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}