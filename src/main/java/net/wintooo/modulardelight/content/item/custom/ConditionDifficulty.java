package net.wintooo.modulardelight.content.item.custom;

import net.minecraft.text.Text;

public enum ConditionDifficulty {
    TRIVIAL(0.5, "trivial"),
    EASY(1.0, "easy"),
    MODERATE(1.5, "moderate"),
    HARD(2.0, "hard"),
    EXTREME(3.0, "extreme");

    private final double multiplier;
    private final String labelKey;

    ConditionDifficulty(double multiplier, String labelSuffix) {
        this.multiplier = multiplier;
        this.labelKey = "tooltip.modulardelight.difficulty." + labelSuffix;
    }

    public double multiplier() {
        return multiplier;
    }

    public Text label() {
        return Text.translatable(labelKey);
    }
}