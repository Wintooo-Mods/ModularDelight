package net.wintooo.modulardelight.content.item.custom;

import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.wintooo.modulardelight.ModularDelight;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public record MealColor(String id, TagKey<Item> tag, int rgb) {
    private static final Map<String, MealColor> REGISTRY = new LinkedHashMap<>();

    public static final MealColor RED = register("red", 0xB33A3A);
    public static final MealColor ORANGE = register("orange", 0xFA9913);
    public static final MealColor YELLOW = register("yellow", 0xE8C93A);
    public static final MealColor LIGHT_GREEN = register("light_green", 0x6BAA4A);
    public static final MealColor GREEN = register("green", 0x2F5A2A);
    public static final MealColor BLUE = register("blue", 0x3E6B9E);
    public static final MealColor PURPLE = register("purple", 0x6E4A8E);
    public static final MealColor PINK = register("pink", 0xE194CB);
    public static final MealColor LIGHT_BROWN = register("light_brown", 0xD2A65C);
    public static final MealColor BROWN = register("brown", 0x4F371F);
    public static final MealColor WHITE = register("white", 0xE8E4D8);
    public static final MealColor GRAY = register("gray", 0x807D7A);
    public static final MealColor BLACK = register("black", 0x292726);

    private static MealColor register(String id, int rgb) {
        TagKey<Item> tag = TagKey.of(RegistryKeys.ITEM, ModularDelight.id("colors/" + id));
        MealColor color = new MealColor(id, tag, rgb);
        REGISTRY.put(id, color);
        return color;
    }

    public static Collection<MealColor> all() {
        return REGISTRY.values();
    }

    public static MealColor byId(String id) {
        return REGISTRY.get(id);
    }
}