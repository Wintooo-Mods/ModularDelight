package net.wintooo.modulardelight.content.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.wintooo.modulardelight.ModularDelight;
import net.wintooo.modulardelight.content.item.custom.MealEffect;
import net.wintooo.modulardelight.content.item.custom.MealProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Function;

public class MealPropertyLoader extends JsonDataLoader implements IdentifiableResourceReloadListener {
    public static final MealPropertyLoader INSTANCE = new MealPropertyLoader();
    private static final Logger LOGGER = LoggerFactory.getLogger("ModularDelight/Properties");

    private MealPropertyLoader() {
        super(new Gson(), "modular_delight/properties");
    }

    @Override
    public Identifier getFabricId() {
        return ModularDelight.id("properties");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
        MealProperty.clear();
        MealEffect.clear();

        int effectCount = 0;
        for (Map.Entry<Identifier, JsonElement> entry : prepared.entrySet()) {
            try {
                JsonObject json = entry.getValue().getAsJsonObject();

                if (EffectJson.bool(json, "remove", false)) {
                    continue;
                }

                Identifier icon = json.has("icon") ? EffectJson.id(json, "icon") : null;
                Text name = EffectJson.text(json, "name", Text.literal(entry.getKey().getPath()));
                MealProperty property = MealProperty.register(entry.getKey(), icon, name);

                boolean hasCondition = json.has("condition");
                boolean hasActivated = json.has("activated");
                if (hasCondition != hasActivated) {
                    throw new IllegalArgumentException("Property " + entry.getKey()
                            + " must define both 'condition' and 'activated', or neither");
                }
                if (hasCondition) {
                    MealEffect.register(entry.getKey(), parseEffect(entry.getKey(), property, json));
                    effectCount++;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load modular delight property {}", entry.getKey(), e);
            }
        }

        LOGGER.info("Loaded {} modular delight properties ({} with meal effects)",
                MealProperty.all().size(), effectCount);
    }

    private MealEffect parseEffect(Identifier id, MealProperty property, JsonObject json) {
        ParsedAmbient ambient = ParsedAmbient.parse(json.has("ambient") ? json.getAsJsonObject("ambient") : null);

        JsonObject conditionJson = json.getAsJsonObject("condition");
        ParsedTrigger trigger = TriggerTypeRegistry.parseWithModifiers(conditionJson);
        double multiplier = EffectJson.dbl(conditionJson, "multiplier", 1.0);
        int cooldownTicks = EffectJson.intg(conditionJson, "cooldown_ticks", 100);
        Text conditionDescription = EffectJson.text(conditionJson, "description", trigger.defaultDescription());

        JsonObject activatedJson = json.getAsJsonObject("activated");
        JsonObject actionJson = activatedJson.getAsJsonObject("action");
        ParsedAction action = ActionTypeRegistry.parse(actionJson);
        Function<Double, Text> activatedDescription = activatedJson.has("description")
                ? mult -> Text.literal(String.format(activatedJson.get("description").getAsString(), action.describeArgs().apply(mult)))
                : action.defaultDescription();

        return new MealEffect(
                id,
                property,
                ambient.attributes(),
                ambient.statusEffects(),
                ambient.damageReactions(),
                ambient.alwaysEdible(),
                ambient.description(),
                trigger.tick(),
                trigger.damage(),
                trigger.attack(),
                trigger.eat(),
                multiplier,
                conditionDescription,
                cooldownTicks,
                action.action(),
                activatedDescription
        );
    }
}