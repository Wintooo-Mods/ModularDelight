package net.wintooo.modulardelight.content.effect.parsing;

import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.wintooo.modulardelight.ModularDelight;
import net.wintooo.modulardelight.content.meal.DigestionEffect;

import java.util.HashMap;
import java.util.Map;

public final class AmbientReactionTypeRegistry {
    @FunctionalInterface
    public interface AmbientReactionFactory {
        DigestionEffect.AmbientDamageReaction parse(JsonObject json);
    }

    private static final Map<Identifier, AmbientReactionFactory> TYPES = new HashMap<>();

    private AmbientReactionTypeRegistry() {}

    public static void register(Identifier id, AmbientReactionFactory factory) {
        TYPES.put(id, factory);
    }

    public static DigestionEffect.AmbientDamageReaction parse(JsonObject json) {
        Identifier type = EffectJson.id(json, "type");
        AmbientReactionFactory factory = TYPES.get(type);
        if (factory == null) throw new IllegalArgumentException("Unknown ambient reaction type: " + type);
        return factory.parse(json);
    }

    public static void bootstrap() {
        register(ModularDelight.id("reflect_damage"), json -> {
            double fraction = EffectJson.dbl(json, "fraction", 0.25);

            return (player, source, amount) -> {
                if (source.isOf(DamageTypes.THORNS)) return;
                Entity attackerEntity = source.getAttacker();
                if (!(attackerEntity instanceof LivingEntity attacker)) return;

                World world = player.getWorld();
                if (world.isClient) return;

                float reflected = (float) (amount * fraction);
                if (reflected <= 0f) return;
                attacker.damage(world.getDamageSources().thorns(player), reflected);
            };
        });
    }
}
