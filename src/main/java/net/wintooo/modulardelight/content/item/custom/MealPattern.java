package net.wintooo.modulardelight.content.item.custom;

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

    public static MealPattern resolve(MealProperty ambient, MealProperty condition, MealProperty activated) {
        boolean ambientEqCondition = ambient == condition;
        boolean ambientEqActivated = ambient == activated;
        boolean conditionEqActivated = condition == activated;

        if (ambientEqCondition && ambientEqActivated) return UNIFORM;
        if (ambientEqCondition) return AMBIENT_CONDITION;
        if (ambientEqActivated) return AMBIENT_ACTIVATED;
        if (conditionEqActivated) return CONDITION_ACTIVATED;
        return UNIQUE;
    }
}