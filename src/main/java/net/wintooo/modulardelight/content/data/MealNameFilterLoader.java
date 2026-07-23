package net.wintooo.modulardelight.content.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.wintooo.modulardelight.ModularDelight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MealNameFilterLoader extends JsonDataLoader implements IdentifiableResourceReloadListener {
    public static final MealNameFilterLoader INSTANCE = new MealNameFilterLoader();
    private static final Logger LOGGER = LoggerFactory.getLogger("ModularDelight/NameFilters");

    private MealNameFilterLoader() {
        super(new Gson(), "modular_delight/name_filters");
    }

    @Override
    public Identifier getFabricId() {
        return ModularDelight.id("name_filters");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
        List<MealNameFilter> filters = new ArrayList<>();

        for (Map.Entry<Identifier, JsonElement> entry : prepared.entrySet()) {
            try {
                JsonArray array = entry.getValue().getAsJsonArray();
                for (JsonElement el : array) {
                    if (el.isJsonPrimitive()) {
                        filters.add(new MealNameFilter(el.getAsString(), false, true));
                        continue;
                    }
                    JsonObject obj = el.getAsJsonObject();
                    boolean isRegex = obj.has("regex");
                    String pattern = isRegex ? obj.get("regex").getAsString() : obj.get("match").getAsString();
                    boolean caseInsensitive = EffectJson.bool(obj, "case_insensitive", true);
                    filters.add(new MealNameFilter(pattern, isRegex, caseInsensitive));
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load modular delight name filter file {}", entry.getKey(), e);
            }
        }

        MealNameFilterRegistry.clear();
        MealNameFilterRegistry.addAll(filters);
        LOGGER.info("Loaded {} modular delight name filters", filters.size());
    }
}