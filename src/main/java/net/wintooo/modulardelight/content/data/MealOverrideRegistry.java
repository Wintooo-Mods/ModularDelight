package net.wintooo.modulardelight.content.data;

import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MealOverrideRegistry {
    private static final Map<Identifier, MealOverride> REGISTRY = new LinkedHashMap<>();

    private MealOverrideRegistry() {}

    public static void clear() {
        REGISTRY.clear();
    }

    public static void register(Identifier id, MealOverride override) {
        REGISTRY.put(id, override);
    }

    public static int count() {
        return REGISTRY.size();
    }

    public static MealOverride find(List<Identifier> ingredientIds) {
        for (MealOverride override : REGISTRY.values()) {
            if (override.matches(ingredientIds)) return override;
        }
        return null;
    }
}