package net.wintooo.modulardelight.content.data;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record MealOverride(
        List<Identifier> ingredients,
        boolean ordered,
        Text name,
        Identifier model
) {
    public boolean matches(List<Identifier> ingredientIds) {
        if (ingredientIds.size() != ingredients.size()) return false;
        if (ordered) return ingredients.equals(ingredientIds);

        List<Identifier> a = new ArrayList<>(ingredients);
        List<Identifier> b = new ArrayList<>(ingredientIds);
        Comparator<Identifier> byString = Comparator.comparing(Identifier::toString);
        a.sort(byString);
        b.sort(byString);
        return a.equals(b);
    }
}