package net.wintooo.modulardelight.content.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.wintooo.modulardelight.content.item.custom.MealEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public record ParsedAmbient(
        List<AmbientAttribute> attributes,
        List<Function<Double, StatusEffectInstance>> statusEffects,
        List<MealEffect.AmbientDamageReaction> damageReactions,
        boolean alwaysEdible,
        Function<Double, Text> description
) {
    public record AmbientAttribute(EntityAttribute attribute, double baseAmount, EntityAttributeModifier.Operation operation) {}

    public static final ParsedAmbient EMPTY =
            new ParsedAmbient(List.of(), List.of(), List.of(), false, mult -> Text.literal(""));

    public static ParsedAmbient parse(JsonObject json) {
        if (json == null) return EMPTY;

        List<AmbientAttribute> attributes = new ArrayList<>();
        List<Function<Double, StatusEffectInstance>> statusEffects = new ArrayList<>();
        List<MealEffect.AmbientDamageReaction> damageReactions = new ArrayList<>();
        List<Function<Double, Text>> autoDescriptions = new ArrayList<>();
        boolean alwaysEdible = EffectJson.bool(json, "always_edible", false);

        for (JsonObject attr : collectEntries(json, "attribute", "attributes")) {
            Identifier attrId = EffectJson.id(attr, "id");
            EntityAttribute attribute = Registries.ATTRIBUTE.getOrEmpty(attrId).orElse(null);
            if (attribute == null) continue;

            double amount = EffectJson.dbl(attr, "amount", 0.0);
            EntityAttributeModifier.Operation operation = EffectJson.operation(attr, "operation");
            double divisor = EffectJson.dbl(attr, "description_divisor", 1.0);
            int decimals = EffectJson.intg(attr, "description_decimals", 0);
            String customFormat = attr.has("description") ? attr.get("description").getAsString() : null;

            attributes.add(new AmbientAttribute(attribute, amount, operation));

            Text attrName = Text.translatable(attribute.getTranslationKey());
            autoDescriptions.add(mult -> customFormat != null
                    ? Text.literal(String.format(customFormat, EffectJson.formatDecimal(amount * mult / divisor, decimals)))
                    : Text.literal("grants you +" + EffectJson.formatDecimal(amount * mult / divisor, decimals) + " ").append(attrName));
        }

        for (JsonObject se : collectEntries(json, "status_effect", "status_effects")) {
            Identifier effectId = EffectJson.id(se, "id");
            StatusEffect effect = Registries.STATUS_EFFECT.get(effectId);
            if (effect == null) continue;

            int refreshTicks = EffectJson.intg(se, "refresh_ticks", 260);
            int amplifier = EffectJson.intg(se, "amplifier", 0);
            statusEffects.add(mult -> new StatusEffectInstance(effect, refreshTicks, amplifier, true, false, false));

            Text effectName = effect.getName();
            autoDescriptions.add(mult -> Text.literal("gives you ").append(effectName));
        }

        for (JsonObject reactionJson : collectEntries(json, "damage_reaction", "damage_reactions")) {
            damageReactions.add(AmbientReactionTypeRegistry.parse(reactionJson));
            autoDescriptions.add(mult -> Text.literal("reacts to incoming damage"));
        }

        if (alwaysEdible) {
            autoDescriptions.add(mult -> Text.literal("lets you eat anytime"));
        }

        Function<Double, Text> autoDescription = mult -> {
            MutableText combined = null;
            for (Function<Double, Text> part : autoDescriptions) {
                Text piece = part.apply(mult);
                combined = combined == null ? piece.copy() : combined.append(" & ").append(piece);
            }
            return combined == null ? Text.literal("") : combined;
        };

        Function<Double, Text> description = json.has("description")
                ? mult -> Text.literal(json.get("description").getAsString())
                : autoDescription;

        return new ParsedAmbient(attributes, statusEffects, damageReactions, alwaysEdible, description);
    }

    private static List<JsonObject> collectEntries(JsonObject json, String singularKey, String pluralKey) {
        List<JsonObject> entries = new ArrayList<>();
        if (json.has(singularKey)) entries.add(json.getAsJsonObject(singularKey));
        if (json.has(pluralKey)) {
            JsonArray array = json.getAsJsonArray(pluralKey);
            for (int i = 0; i < array.size(); i++) entries.add(array.get(i).getAsJsonObject());
        }
        return entries;
    }
}