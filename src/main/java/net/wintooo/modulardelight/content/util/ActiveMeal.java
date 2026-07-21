package net.wintooo.modulardelight.content.util;

import net.wintooo.modulardelight.content.item.custom.MealEffect;

public record ActiveMeal(
        MealEffect ambient,
        MealEffect condition,
        MealEffect activated

) {}