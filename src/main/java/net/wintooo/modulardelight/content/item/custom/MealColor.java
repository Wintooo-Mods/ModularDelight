package net.wintooo.modulardelight.content.item.custom;

import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public record MealColor(Identifier id, TagKey<Item> tag, int rgb) {
    private static final Map<Identifier, MealColor> REGISTRY = new LinkedHashMap<>();

    public static void clear() {
        REGISTRY.clear();
    }

    public static MealColor register(Identifier id, int rgb) {
        TagKey<Item> tag = TagKey.of(RegistryKeys.ITEM, new Identifier(id.getNamespace(), "colors/" + id.getPath()));
        MealColor color = new MealColor(id, tag, rgb);
        REGISTRY.put(id, color);
        return color;
    }

    public static Collection<MealColor> all() {
        return REGISTRY.values();
    }

}