package net.wintooo.modulardelight.content.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.wintooo.modulardelight.ModularDelight;

public class ModStatusEffects {
    public static final StatusEffect DIGESTION = Registry.register(
            Registries.STATUS_EFFECT,
            ModularDelight.id("digestion"),
            new StatusEffect(StatusEffectCategory.BENEFICIAL, 0x9C6B30) {});

    public static void load() {}
}