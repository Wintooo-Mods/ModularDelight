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
import net.wintooo.modulardelight.content.item.custom.ModularMealItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class MealOverrideLoader extends JsonDataLoader implements IdentifiableResourceReloadListener {
    public static final MealOverrideLoader INSTANCE = new MealOverrideLoader();
    private static final Logger LOGGER = LoggerFactory.getLogger("ModularDelight/MealOverrides");

    private MealOverrideLoader() {
        super(new Gson(), "modular_delight/meal_overrides");
    }

    @Override
    public Identifier getFabricId() {
        return ModularDelight.id("meal_overrides");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
        MealOverrideRegistry.clear();

        for (Map.Entry<Identifier, JsonElement> entry : prepared.entrySet()) {
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                if (EffectJson.bool(json, "remove", false)) continue;

                List<Identifier> ingredients = EffectJson.idList(json, "ingredients");
                if (ingredients.size() != ModularMealItem.REQUIRED_SLOTS) {
                    throw new IllegalArgumentException("Meal override " + entry.getKey()
                            + " must list exactly " + ModularMealItem.REQUIRED_SLOTS + " ingredients");
                }

                boolean ordered = EffectJson.bool(json, "ordered", false);
                Text name = EffectJson.text(json, "name", null);
                Float modelIndex = json.has("model_index") ? (float) EffectJson.dbl(json, "model_index", 0.0) : null;

                MealOverrideRegistry.register(entry.getKey(), new MealOverride(ingredients, ordered, name, modelIndex));
            } catch (Exception e) {
                LOGGER.error("Failed to load modular delight meal override {}", entry.getKey(), e);
            }
        }

        LOGGER.info("Loaded {} modular delight meal overrides", MealOverrideRegistry.count());
    }
}