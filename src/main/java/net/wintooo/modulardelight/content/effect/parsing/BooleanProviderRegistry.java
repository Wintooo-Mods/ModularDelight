package net.wintooo.modulardelight.content.effect.parsing;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.wintooo.modulardelight.ModularDelight;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public final class BooleanProviderRegistry {
    private static final Map<Identifier, Predicate<PlayerEntity>> STATES = new HashMap<>();

    private BooleanProviderRegistry() {}

    public static void register(Identifier id, Predicate<PlayerEntity> provider) {
        STATES.put(id, provider);
    }

    public static Predicate<PlayerEntity> get(Identifier id) {
        Predicate<PlayerEntity> provider = STATES.get(id);
        if (provider == null) throw new IllegalArgumentException("Unknown modular delight state: " + id);
        return provider;
    }

    public static void bootstrap() {
        register(ModularDelight.id("sneaking"), PlayerEntity::isSneaking);
        register(ModularDelight.id("sprinting"), PlayerEntity::isSprinting);
        register(ModularDelight.id("submerged"), PlayerEntity::isSubmergedInWater);
        register(ModularDelight.id("on_fire"), PlayerEntity::isOnFire);
        register(ModularDelight.id("on_ground"), PlayerEntity::isOnGround);
        register(ModularDelight.id("touching_water"), PlayerEntity::isTouchingWater);
        register(ModularDelight.id("wet"), PlayerEntity::isWet);
        register(ModularDelight.id("gliding"), PlayerEntity::isFallFlying);
        register(ModularDelight.id("sleeping"), PlayerEntity::isSleeping);
        register(ModularDelight.id("blocking"), PlayerEntity::isBlocking);
        register(ModularDelight.id("invisible"), PlayerEntity::isInvisible);
        register(ModularDelight.id("crawling"), PlayerEntity::isCrawling);
    }
}