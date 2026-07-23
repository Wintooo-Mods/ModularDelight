package net.wintooo.modulardelight.content.data;

import java.util.ArrayList;
import java.util.List;

public final class MealNameFilterRegistry {
    private static final List<MealNameFilter> FILTERS = new ArrayList<>();

    private MealNameFilterRegistry() {}

    public static void clear() {
        FILTERS.clear();
    }

    public static void addAll(List<MealNameFilter> filters) {
        FILTERS.addAll(filters);
    }

    public static String strip(String input) {
        String result = input;
        for (MealNameFilter filter : FILTERS) {
            result = filter.apply(result);
        }
        return result.replaceAll("\\s{2,}", " ").trim();
    }
}