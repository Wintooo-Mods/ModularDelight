package net.wintooo.modulardelight.content.item.custom;

import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.wintooo.modulardelight.ModularDelight;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public record MealProperty(String id, TagKey<Item> tag, Identifier icon, String translationKey) {
    private static final Map<String, MealProperty> REGISTRY = new LinkedHashMap<>();

    public static final MealProperty HEARTY = register("hearty");
    public static final MealProperty TOUGH = register("tough");
    public static final MealProperty FIERCE = register("fierce");
    public static final MealProperty EXPLOSIVE = register("explosive");
    public static final MealProperty SPEEDY = register("speedy");
    public static final MealProperty NIMBLE = register("nimble");
    public static final MealProperty AQUATIC = register("aquatic");
    public static final MealProperty STEALTHY = register("stealthy");
    public static final MealProperty NOCTURNAL = register("nocturnal");
    public static final MealProperty SKYBORNE = register("skyborne");
    public static final MealProperty UNSTABLE = register("unstable");
    public static final MealProperty FIERY = register("fiery");
    public static final MealProperty GANGLY = register("gangly");
    public static final MealProperty SUREFOOTED = register("surefooted");

    private static MealProperty register(String id) {
        TagKey<Item> tag = TagKey.of(RegistryKeys.ITEM, ModularDelight.id("properties/" + id));
        Identifier icon = ModularDelight.id("textures/gui/sprites/property/property.png");
        String translationKey = "tooltip.modulardelight.property." + id;
        MealProperty prop = new MealProperty(id, tag, icon, translationKey);
        REGISTRY.put(id, prop);
        return prop;
    }

    public static Collection<MealProperty> all() {
        return REGISTRY.values();
    }
}