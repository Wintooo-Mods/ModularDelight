package net.wintooo.modulardelight.mixin.client;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Accessor("x")
    int modulardelight$getX();

    @Accessor("y")
    int modulardelight$getY();

    @Accessor("backgroundWidth")
    int modulardelight$getBackgroundWidth();
}