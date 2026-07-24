package net.wintooo.modulardelight.content.util;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.wintooo.modulardelight.content.meal.DigestionEffect;
import net.wintooo.modulardelight.content.meal.ActiveMeal;
import net.wintooo.modulardelight.content.meal.MealProperty;

import java.util.ArrayList;
import java.util.List;

public final class ClientDigestionManager {

    private static final List<ActiveMeal> ACTIVE = new ArrayList<>();

    private ClientDigestionManager() {}

    public static void set(List<ActiveMeal> meals) {
        ACTIVE.clear();
        ACTIVE.addAll(meals);
    }

    public static List<Text> getActiveTooltip() {
        if (ACTIVE.isEmpty()) {
            return List.of();
        }

        List<Text> tooltip = new ArrayList<>();

        for (int i = 0; i < ACTIVE.size(); i++) {
            if (i > 0) {
                tooltip.add(Text.literal(""));
            }

            ActiveMeal meal = ACTIVE.get(i);
            tooltip.add(headerText(meal));

            DigestionEffect composite = DigestionEffect.combine(
                    meal.ambient(),
                    meal.condition(),
                    meal.activated()
            );

            tooltip.addAll(composite.getActiveTooltip());
        }

        return tooltip;
    }

    private static Text headerText(ActiveMeal meal) {
        MealProperty ambient = meal.ambient().property();
        MealProperty condition = meal.condition().property();
        MealProperty activated = meal.activated().property();

        Text ambientName = ambient.name();
        Text conditionName = condition.name();
        Text activatedName = activated.name();

        boolean ambientEqCondition = ambient == condition;
        boolean ambientEqActivated = ambient == activated;
        boolean conditionEqActivated = condition == activated;

        Text header;
        if (ambientEqCondition && ambientEqActivated) {
            header = Text.translatable("tooltip.modulardelight.active.header.one", ambientName);
        } else if (ambientEqCondition) {
            header = Text.translatable("tooltip.modulardelight.active.header.two", ambientName, activatedName);
        } else if (ambientEqActivated) {
            header = Text.translatable("tooltip.modulardelight.active.header.two", ambientName, conditionName);
        } else if (conditionEqActivated) {
            header = Text.translatable("tooltip.modulardelight.active.header.two", conditionName, ambientName);
        } else {
            header = Text.translatable("tooltip.modulardelight.active.header.three", ambientName, conditionName, activatedName);
        }

        return header.copy().formatted(Formatting.BOLD, Formatting.WHITE);
    }

    public static void clear() {
        ACTIVE.clear();
    }
}