package net.wintooo.modulardelight.content.effect.parsing;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class EffectJson {
    private EffectJson() {}

    public static Text text(JsonObject json, String key, Text fallback) {
        if (!json.has(key)) return fallback;
        return Text.Serializer.fromJson(json.get(key));
    }

    public static Identifier id(JsonObject json, String key) {
        return new Identifier(JsonHelper.getString(json, key));
    }

    public static double dbl(JsonObject json, String key, double fallback) {
        return JsonHelper.getDouble(json, key, fallback);
    }

    public static float flt(JsonObject json, String key, float fallback) {
        return JsonHelper.getFloat(json, key, fallback);
    }

    public static int intg(JsonObject json, String key, int fallback) {
        return JsonHelper.getInt(json, key, fallback);
    }

    public static boolean bool(JsonObject json, String key, boolean fallback) {
        return JsonHelper.getBoolean(json, key, fallback);
    }

    public static EntityAttributeModifier.Operation operation(JsonObject json, String key) {
        String value = JsonHelper.getString(json, key, "addition");
        return switch (value) {
            case "multiply_base" -> EntityAttributeModifier.Operation.MULTIPLY_BASE;
            case "multiply_total" -> EntityAttributeModifier.Operation.MULTIPLY_TOTAL;
            default -> EntityAttributeModifier.Operation.ADDITION;
        };
    }

    public static int hexColor(JsonObject json, String key, int fallback) {
        if (!json.has(key)) return fallback;
        JsonElement el = json.get(key);
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
            return Integer.parseInt(el.getAsString().replace("#", ""), 16);
        }
        return el.getAsInt();
    }

    public static List<Identifier> idList(JsonObject json, String key) {
        List<Identifier> out = new ArrayList<>();
        if (!json.has(key)) return out;
        JsonArray array = JsonHelper.getArray(json, key);
        for (JsonElement el : array) out.add(new Identifier(el.getAsString()));
        return out;
    }

    public static String formatDecimal(double value, int decimals) {
        if (decimals <= 0) return String.valueOf(Math.round(value));
        return BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).toPlainString();
    }
}