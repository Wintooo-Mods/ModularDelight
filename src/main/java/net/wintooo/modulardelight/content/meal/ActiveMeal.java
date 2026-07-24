package net.wintooo.modulardelight.content.meal;

public record ActiveMeal(
        DigestionEffect ambient,
        DigestionEffect condition,
        DigestionEffect activated

) {}