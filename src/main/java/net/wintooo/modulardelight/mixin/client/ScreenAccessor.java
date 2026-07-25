package net.wintooo.modulardelight.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Screen.class)
public interface ScreenAccessor {
    @Accessor("client")
    MinecraftClient modulardelight$getClient();

    @Accessor("textRenderer")
    TextRenderer modulardelight$getTextRenderer();

    @Accessor("width")
    int modulardelight$getWidth();
}