package net.wintooo.modulardelight.content.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.wintooo.modulardelight.ModularDelight;
import net.wintooo.modulardelight.content.item.custom.MealColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MealColorLoader extends JsonDataLoader implements IdentifiableResourceReloadListener {
    public static final MealColorLoader INSTANCE = new MealColorLoader();
    private static final Logger LOGGER = LoggerFactory.getLogger("ModularDelight/Colors");

    private MealColorLoader() {
        super(new Gson(), "modular_delight/colors");
    }

    @Override
    public Identifier getFabricId() {
        return ModularDelight.id("colors");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
        MealColor.clear();

        for (Map.Entry<Identifier, JsonElement> entry : prepared.entrySet()) {
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                int rgb = EffectJson.hexColor(json, "rgb", 0xFFFFFF);
                MealColor.register(entry.getKey(), rgb);
            } catch (Exception e) {
                LOGGER.error("Failed to load meal color {}", entry.getKey(), e);
            }
        }

        LOGGER.info("Loaded {} modular delight colors", MealColor.all().size());
    }
}