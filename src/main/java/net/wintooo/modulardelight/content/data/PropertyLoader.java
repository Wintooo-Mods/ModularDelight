package net.wintooo.modulardelight.content.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.wintooo.modulardelight.ModularDelight;
import net.wintooo.modulardelight.content.effect.parsing.*;
import net.wintooo.modulardelight.content.effect.parsing.EffectJson;
import net.wintooo.modulardelight.content.meal.DigestionEffect;
import net.wintooo.modulardelight.content.meal.MealProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class PropertyLoader extends JsonDataLoader implements IdentifiableResourceReloadListener {
    public static final PropertyLoader INSTANCE = new PropertyLoader();
    private static final Logger LOGGER = LoggerFactory.getLogger("ModularDelight/Properties");

    private PropertyLoader() {
        super(new Gson(), "modular_delight/properties");
    }

    @Override
    public Identifier getFabricId() {
        return ModularDelight.id("properties");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
        MealProperty.clear();
        DigestionEffect.clear();

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

                boolean hasAmbient = json.has("ambient");
                if (hasCondition || hasAmbient) {
                    DigestionEffect.register(entry.getKey(), parseEffect(entry.getKey(), property, json));
                    effectCount++;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load modular delight property {}", entry.getKey(), e);
            }
        }

        LOGGER.info("Loaded {} modular delight properties ({} with meal effects)",
                MealProperty.all().size(), effectCount);
    }

    /**
     * condition/activated are optional (but must appear together, enforced above). A property
     * with only an "ambient" block is legal — an ambient-only effect, valid solely in the
     * Ambient slot, with no trigger and no action of its own.
     */
    private DigestionEffect parseEffect(Identifier id, MealProperty property, JsonObject json) {
        ParsedAmbient ambient = ParsedAmbient.parse(json.has("ambient") ? json.getAsJsonObject("ambient") : null);

        Predicate<PlayerEntity> tickTrigger = null;
        DigestionEffect.DamageTrigger damageTrigger = null;
        DigestionEffect.AttackTrigger attackTrigger = null;
        DigestionEffect.EatTrigger eatTrigger = null;
        double multiplier = 1.0;
        Text conditionDescription = Text.literal("");
        int cooldownTicks = 0;
        BiConsumer<ServerPlayerEntity, Double> activatedAction = null;
        Function<Double, Text> activatedDescription = mult -> Text.literal("");

        if (json.has("condition")) {
            JsonObject conditionJson = json.getAsJsonObject("condition");
            ParsedTrigger trigger = TriggerTypeRegistry.parseWithModifiers(conditionJson);
            multiplier = EffectJson.dbl(conditionJson, "multiplier", 1.0);
            cooldownTicks = EffectJson.intg(conditionJson, "cooldown_ticks", 100);
            conditionDescription = EffectJson.text(conditionJson, "description", trigger.defaultDescription());
            tickTrigger = trigger.tick();
            damageTrigger = trigger.damage();
            attackTrigger = trigger.attack();
            eatTrigger = trigger.eat();

            JsonObject activatedJson = json.getAsJsonObject("activated");
            JsonObject actionJson = activatedJson.getAsJsonObject("action");
            ParsedAction action = ActionTypeRegistry.parse(actionJson);
            activatedAction = action.action();
            activatedDescription = activatedJson.has("description")
                    ? mult -> Text.literal(String.format(activatedJson.get("description").getAsString(), action.describeArgs().apply(mult)))
                    : action.defaultDescription();
        }

        return new DigestionEffect(
                id,
                property,
                ambient.attributes(),
                ambient.statusEffects(),
                ambient.damageReactions(),
                ambient.alwaysEdible(),
                ambient.description(),
                tickTrigger,
                damageTrigger,
                attackTrigger,
                eatTrigger,
                multiplier,
                conditionDescription,
                cooldownTicks,
                activatedAction,
                activatedDescription
        );
    }
}