package net.wintooo.modulardelight.content.effect.parsing;

import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.wintooo.modulardelight.ModularDelight;

import java.util.HashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

public final class ValueProviderRegistry {
    private static final Map<Identifier, ToDoubleFunction<PlayerEntity>> VALUES = new HashMap<>();

    private ValueProviderRegistry() {}

    public static void register(Identifier id, ToDoubleFunction<PlayerEntity> provider) {
        VALUES.put(id, provider);
    }

    public static ToDoubleFunction<PlayerEntity> get(Identifier id) {
        ToDoubleFunction<PlayerEntity> provider = VALUES.get(id);
        if (provider == null) throw new IllegalArgumentException("Unknown modular delight value: " + id);
        return provider;
    }

    public static void bootstrap() {
        register(ModularDelight.id("health"), PlayerEntity::getHealth);
        register(ModularDelight.id("max_health"), PlayerEntity::getMaxHealth);
        register(ModularDelight.id("hunger"), p -> p.getHungerManager().getFoodLevel());
        register(ModularDelight.id("saturation"), p -> p.getHungerManager().getSaturationLevel());
        register(ModularDelight.id("velocity_y"), p -> p.getVelocity().y);
        register(ModularDelight.id("velocity_horizontal"), p -> {
            double dx = p.getVelocity().x, dz = p.getVelocity().z;
            return Math.sqrt(dx * dx + dz * dz);
        });
        register(ModularDelight.id("y_position"), PlayerEntity::getY);
        register(ModularDelight.id("light_level"), p -> p.getWorld().getLightLevel(p.getBlockPos()));
        register(ModularDelight.id("moon_phase"), p -> p.getWorld().getMoonPhase());
        register(ModularDelight.id("time_of_day"), p -> p.getWorld().getTimeOfDay() % 24000L);
        register(ModularDelight.id("armor"), PlayerEntity::getArmor);
        register(ModularDelight.id("luck"), p -> p.getAttributeValue(EntityAttributes.GENERIC_LUCK));
        register(ModularDelight.id("air"), PlayerEntity::getAir);
        register(ModularDelight.id("experience_level"), p -> p.experienceLevel);
        register(ModularDelight.id("fall_distance"), p -> p.fallDistance);
    }
}