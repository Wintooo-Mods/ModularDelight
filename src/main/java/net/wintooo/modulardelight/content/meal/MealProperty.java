package net.wintooo.modulardelight.content.meal;

import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public record MealProperty(Identifier id, TagKey<Item> tag, Identifier icon, Text name, int color) {
    private static final Map<Identifier, MealProperty> REGISTRY = new LinkedHashMap<>();
    private static final Identifier DEFAULT_ICON =
            new Identifier("modulardelight", "textures/gui/sprites/property/property.png");

    public static void clear() {
        REGISTRY.clear();
    }

    public static MealProperty register(Identifier id, Identifier icon, Text name, int color) {
        TagKey<Item> tag = TagKey.of(RegistryKeys.ITEM, new Identifier(id.getNamespace(), "properties/" + id.getPath()));
        MealProperty prop = new MealProperty(id, tag, icon == null ? DEFAULT_ICON : icon, name, color);
        REGISTRY.put(id, prop);
        return prop;
    }

    public static Collection<MealProperty> all() {
        return REGISTRY.values();
    }

}
