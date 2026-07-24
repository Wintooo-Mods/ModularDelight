package net.wintooo.modulardelight.content.meal;

import net.minecraft.util.Identifier;

public enum MealPattern {
    UNIFORM(0.0f, "porridge"),
    AMBIENT_CONDITION(0.25f, "curry"),
    AMBIENT_ACTIVATED(0.5f, "casserole"),
    CONDITION_ACTIVATED(0.75f, "soup"),
    UNIQUE(1.0f, "stew");

    private final float modelIndex;
    private final String nameKey;

    MealPattern(float modelIndex, String nameSuffix) {
        this.modelIndex = modelIndex;
        this.nameKey = "item.modulardelight.modular_meal.name." + nameSuffix;
    }

    public float modelIndex() {
        return modelIndex;
    }

    public String nameTranslationKey() {
        return nameKey;
    }

    public static MealPattern resolve(Identifier ambientId, Identifier conditionId, Identifier activatedId) {
        boolean ambientEqCondition = ambientId.equals(conditionId);
        boolean ambientEqActivated = ambientId.equals(activatedId);
        boolean conditionEqActivated = conditionId.equals(activatedId);

        if (ambientEqCondition && ambientEqActivated) return UNIFORM;
        if (ambientEqCondition) return AMBIENT_CONDITION;
        if (ambientEqActivated) return AMBIENT_ACTIVATED;
        if (conditionEqActivated) return CONDITION_ACTIVATED;
        return UNIQUE;
    }
}