package net.wintooo.modulardelight.content.item.custom;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public record MealEffect(
        String id,
        MealProperty property,
        EntityAttribute ambientAttribute,
        double ambientBaseAmount,
        EntityAttributeModifier.Operation ambientOperation,
        Function<Double, Text> ambientDescription,
        Function<Double, StatusEffectInstance> ambientStatusEffect,
        Predicate<PlayerEntity> tickTrigger,
        DamageTrigger damageTrigger,
        AttackTrigger attackTrigger,
        ConditionDifficulty difficulty,
        Text conditionDescription,
        BiConsumer<ServerPlayerEntity, Double> activatedAction,
        Function<Double, Text> activatedDescription,
        int triggerCooldownTicks
) {
    @FunctionalInterface
    public interface DamageTrigger {
        boolean test(PlayerEntity player, DamageSource source, float amount);
    }

    @FunctionalInterface
    public interface AttackTrigger {
        boolean test(PlayerEntity attacker, LivingEntity target, float amountDealt);
    }

    private static final Map<String, MealEffect> REGISTRY = new LinkedHashMap<>();
    private static final Random RANDOM = new Random();

    private static final EntityAttribute SWIM_SPEED = optionalAttribute("forge:swim_speed");
    private static final EntityAttribute ENTITY_GRAVITY = optionalAttribute("forge:entity_gravity");
    private static final EntityAttribute STEP_HEIGHT = optionalAttribute("forge:step_height_addition");
    private static final EntityAttribute ATTACK_RANGE = optionalAttribute("reach-entity-attributes:attack_range");

    private static EntityAttribute optionalAttribute(String id) {
        return Registries.ATTRIBUTE.getOrEmpty(new Identifier(id)).orElse(null);
    }

    public UUID modifierId() {
        return UUID.nameUUIDFromBytes(("modulardelight:ambient:" + id).getBytes());
    }

    public EntityAttributeModifier scaledAmbientModifier(double multiplier) {
        return new EntityAttributeModifier(modifierId(), "modulardelight:" + id,
                ambientBaseAmount * multiplier, ambientOperation);
    }

    private static int scaledDuration(int baseTicks, double multiplier) {
        return Math.max(20, Math.round(baseTicks * (float) multiplier));
    }

    private static int scaledAmplifier(int baseAmplifier, double multiplier) {
        return Math.max(0, baseAmplifier + (int) Math.floor(multiplier - 1.0));
    }

    private static String level(int amplifier) {
        String[] numerals = {"I", "II", "III", "IV", "V", "VI"};
        return numerals[Math.min(amplifier, numerals.length - 1)];
    }

    public static final MealEffect HEARTY = register(new MealEffect(
            "hearty",
            MealProperty.HEARTY,
            EntityAttributes.GENERIC_MAX_HEALTH,
            2.0,
            EntityAttributeModifier.Operation.ADDITION,
            mult -> Text.translatable("tooltip.modulardelight.effect.hearty.ambient",
                    Math.round(2.0 * mult / 2.0)),
            null,
            player -> player.getHealth() < 10.0f,
            null,
            null,
            ConditionDifficulty.EASY,
            Text.translatable("tooltip.modulardelight.effect.hearty.condition"),
            (player, mult) -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION,
                    scaledDuration(5 * 20, mult), 0, false, true)),
            mult -> Text.translatable("tooltip.modulardelight.effect.hearty.activated",
                    scaledDuration(5 * 20, mult) / 20),
            100
    ));

    public static final MealEffect TOUGH = register(new MealEffect(
            "tough",
            MealProperty.TOUGH,
            EntityAttributes.GENERIC_ARMOR,
            2.0,
            EntityAttributeModifier.Operation.ADDITION,
            mult -> Text.translatable("tooltip.modulardelight.effect.tough.ambient",
                    Math.round(2.0 * mult)),
            null,
            null,
            (player, source, amount) -> amount > 8.0f,
            null,
            ConditionDifficulty.MODERATE,
            Text.translatable("tooltip.modulardelight.effect.tough.condition"),
            (player, mult) -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                    scaledDuration(6 * 20, mult), scaledAmplifier(0, mult), false, true)),
            mult -> Text.translatable("tooltip.modulardelight.effect.tough.activated",
                    level(scaledAmplifier(0, mult)), scaledDuration(6 * 20, mult) / 20),
            200
    ));

    public static final MealEffect FIERCE = register(new MealEffect(
            "fierce",
            MealProperty.FIERCE,
            EntityAttributes.GENERIC_ATTACK_DAMAGE,
            1.0,
            EntityAttributeModifier.Operation.ADDITION,
            mult -> Text.translatable("tooltip.modulardelight.effect.fierce.ambient",
                    Math.round(1.0 * mult)),
            null,
            null,
            null,
            (attacker, target, amountDealt) -> amountDealt > 4.0f,
            ConditionDifficulty.MODERATE,
            Text.translatable("tooltip.modulardelight.effect.fierce.condition"),
            (player, mult) -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,
                    scaledDuration(8 * 20, mult), scaledAmplifier(0, mult), false, true)),
            mult -> Text.translatable("tooltip.modulardelight.effect.fierce.activated",
                    level(scaledAmplifier(0, mult)), scaledDuration(8 * 20, mult) / 20),
            160
    ));

    public static final MealEffect EXPLOSIVE = register(new MealEffect(
            "explosive",
            MealProperty.EXPLOSIVE,
            EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,
            0.2,
            EntityAttributeModifier.Operation.ADDITION,
            mult -> Text.translatable("tooltip.modulardelight.effect.explosive.ambient",
                    Math.round(0.2 * mult * 100)),
            null,
            null,
            (player, source, amount) -> source.isIn(DamageTypeTags.IS_EXPLOSION),
            null,
            ConditionDifficulty.HARD,
            Text.translatable("tooltip.modulardelight.effect.explosive.condition"),
            (player, mult) -> {
                World world = player.getWorld();
                if (!world.isClient) {
                    float power = (float) Math.min(2.0 * mult, 4.0);
                    world.createExplosion(player, player.getX(), player.getY(), player.getZ(),
                            power, false, World.ExplosionSourceType.NONE);
                }
            },
            mult -> Text.translatable("tooltip.modulardelight.effect.explosive.activated"),
            100
    ));

    public static final MealEffect SPEEDY = register(new MealEffect(
            "speedy",
            MealProperty.SPEEDY,
            EntityAttributes.GENERIC_MOVEMENT_SPEED,
            0.1,
            EntityAttributeModifier.Operation.MULTIPLY_TOTAL,
            mult -> Text.translatable("tooltip.modulardelight.effect.speedy.ambient",
                    Math.round(0.1 * mult * 100)),
            null,
            PlayerEntity::isSprinting,
            null,
            null,
            ConditionDifficulty.TRIVIAL,
            Text.translatable("tooltip.modulardelight.effect.speedy.condition"),
            (player, mult) -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,
                    scaledDuration(5 * 20, mult), scaledAmplifier(1, mult), false, true)),
            mult -> Text.translatable("tooltip.modulardelight.effect.speedy.activated",
                    level(scaledAmplifier(1, mult)), scaledDuration(5 * 20, mult) / 20),
            200
    ));

    public static final MealEffect NIMBLE = register(new MealEffect(
            "nimble",
            MealProperty.NIMBLE,
            null,
            0.0,
            EntityAttributeModifier.Operation.ADDITION,
            mult -> Text.translatable("tooltip.modulardelight.effect.nimble.ambient"),
            null,
            player -> !player.isOnGround() && player.getVelocity().y > 0.1,
            null,
            null,
            ConditionDifficulty.EASY,
            Text.translatable("tooltip.modulardelight.effect.nimble.condition"),
            (player, mult) -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST,
                    scaledDuration(8 * 20, mult), scaledAmplifier(1, mult), false, true)),
            mult -> Text.translatable("tooltip.modulardelight.effect.nimble.activated",
                    level(scaledAmplifier(1, mult)), scaledDuration(8 * 20, mult) / 20),
            100
    ));

    public static final MealEffect AQUATIC = register(new MealEffect(
            "aquatic",
            MealProperty.AQUATIC,
            SWIM_SPEED,
            0.3,
            EntityAttributeModifier.Operation.MULTIPLY_TOTAL,
            mult -> Text.translatable("tooltip.modulardelight.effect.aquatic.ambient"),
            null,
            PlayerEntity::isSubmergedInWater,
            null,
            null,
            ConditionDifficulty.TRIVIAL,
            Text.translatable("tooltip.modulardelight.effect.aquatic.condition"),
            (player, mult) -> {
                int duration = scaledDuration(60 * 20, mult);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, duration, 0, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, duration, 0, false, true));
            },
            mult -> Text.translatable("tooltip.modulardelight.effect.aquatic.activated",
                    scaledDuration(60 * 20, mult) / 60 / 20),
            1200
    ));

    public static final MealEffect STEALTHY = register(new MealEffect(
            "stealthy",
            MealProperty.STEALTHY,
            null,
            0.0,
            EntityAttributeModifier.Operation.ADDITION,
            mult -> Text.translatable("tooltip.modulardelight.effect.stealthy.ambient"),
            null,
            PlayerEntity::isSneaking,
            null,
            null,
            ConditionDifficulty.TRIVIAL,
            Text.translatable("tooltip.modulardelight.effect.stealthy.condition"),
            (player, mult) -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY,
                    scaledDuration(5 * 20, mult), 0, false, true)),
            mult -> Text.translatable("tooltip.modulardelight.effect.stealthy.activated",
                    scaledDuration(5 * 20, mult) / 20),
            400
    ));

    public static final MealEffect NOCTURNAL = register(new MealEffect(
            "nocturnal",
            MealProperty.NOCTURNAL,
            null,
            0.0,
            EntityAttributeModifier.Operation.ADDITION,
            mult -> Text.translatable("tooltip.modulardelight.effect.nocturnal.ambient"),
            mult -> new StatusEffectInstance(StatusEffects.NIGHT_VISION, 260, 0, true, false),
            MealEffect::isNight,
            null,
            null,
            ConditionDifficulty.TRIVIAL,
            Text.translatable("tooltip.modulardelight.effect.nocturnal.condition"),
            (player, mult) -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION,
                    scaledDuration(5 * 20, mult), 0, false, true)),
            mult -> Text.translatable("tooltip.modulardelight.effect.nocturnal.activated",
                    scaledDuration(5 * 20, mult) / 20),
            6000
    ));

    public static final MealEffect SKYBORNE = register(new MealEffect(
            "skyborne",
            MealProperty.SKYBORNE,
            ENTITY_GRAVITY,
            -0.15,
            EntityAttributeModifier.Operation.MULTIPLY_TOTAL,
            mult -> Text.translatable("tooltip.modulardelight.effect.skyborne.ambient"),
            null,
            player -> !player.isOnGround() && player.getVelocity().y < -0.5,
            null,
            null,
            ConditionDifficulty.MODERATE,
            Text.translatable("tooltip.modulardelight.effect.skyborne.condition"),
            (player, mult) -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING,
                    scaledDuration(20 * 20, mult), 0, false, true)),
            mult -> Text.translatable("tooltip.modulardelight.effect.skyborne.activated",
                    scaledDuration(20 * 20, mult) / 20),
            600
    ));

    public static final MealEffect UNSTABLE = register(new MealEffect(
            "unstable",
            MealProperty.UNSTABLE,
            null,
            0.0,
            EntityAttributeModifier.Operation.ADDITION,
            mult -> Text.translatable("tooltip.modulardelight.effect.unstable.ambient"),
            null,
            player -> RANDOM.nextFloat() < 0.002f,
            null,
            null,
            ConditionDifficulty.EXTREME,
            Text.translatable("tooltip.modulardelight.effect.unstable.condition"),
            (player, mult) -> teleportRandomly(player),
            mult -> Text.translatable("tooltip.modulardelight.effect.unstable.activated"),
            40
    ));

    public static final MealEffect FIERY = register(new MealEffect(
            "fiery",
            MealProperty.FIERY,
            null,
            0.0,
            EntityAttributeModifier.Operation.ADDITION,
            mult -> Text.translatable("tooltip.modulardelight.effect.fiery.ambient"),
            mult -> new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 260, 0, true, false),
            null,
            (player, source, amount) -> source.isIn(DamageTypeTags.IS_FIRE),
            null,
            ConditionDifficulty.MODERATE,
            Text.translatable("tooltip.modulardelight.effect.fiery.condition"),
            MealEffect::igniteNearbyEnemies,
            mult -> Text.translatable("tooltip.modulardelight.effect.fiery.activated",
                    scaledDuration(5 * 20, mult) / 20),
            600
    ));

    public static final MealEffect GANGLY = register(new MealEffect(
            "gangly",
            MealProperty.GANGLY,
            ATTACK_RANGE,
            1.0,
            EntityAttributeModifier.Operation.ADDITION,
            mult -> Text.translatable("tooltip.modulardelight.effect.gangly.ambient",
                    Math.round(1.0 * mult * 10.0) / 10.0),
            null,
            null,
            null,
            (attacker, target, amountDealt) -> attacker.distanceTo(target) > 4.5,
            ConditionDifficulty.MODERATE,
            Text.translatable("tooltip.modulardelight.effect.gangly.condition"),
            (player, mult) -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.INSTANT_HEALTH,
                    1, scaledAmplifier(0, mult), false, true)),
            mult -> Text.translatable("tooltip.modulardelight.effect.gangly.activated"),
            200
    ));

    public static final MealEffect LUCKY = register(new MealEffect(
            "lucky",
            MealProperty.LUCKY,
            EntityAttributes.GENERIC_LUCK,
            1.0,
            EntityAttributeModifier.Operation.ADDITION,
            mult -> Text.translatable("tooltip.modulardelight.effect.lucky.ambient",
                    Math.round(1.0 * mult)),
            null,
            null,
            (player, source, amount) -> {
                float healthAfter = player.getHealth() - amount;
                return healthAfter > 0.0f && healthAfter <= 2.0f;
            },
            null,
            ConditionDifficulty.HARD,
            Text.translatable("tooltip.modulardelight.effect.lucky.condition"),
            (player, mult) -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION,
                    scaledDuration(5 * 20, mult), scaledAmplifier(0, mult), false, true)),
            mult -> Text.translatable("tooltip.modulardelight.effect.lucky.activated",
                    level(scaledAmplifier(0, mult)), scaledDuration(5 * 20, mult) / 20),
            600
    ));

    public static final MealEffect SUREFOOTED = register(new MealEffect(
            "surefooted",
            MealProperty.SUREFOOTED,
            STEP_HEIGHT,
            0.5,
            EntityAttributeModifier.Operation.ADDITION,
            mult -> Text.translatable("tooltip.modulardelight.effect.surefooted.ambient"),
            null,
            null,
            (player, source, amount) -> source.isIn(DamageTypeTags.IS_FALL),
            null,
            ConditionDifficulty.EASY,
            Text.translatable("tooltip.modulardelight.effect.surefooted.condition"),
            (player, mult) -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.HEALTH_BOOST,
                    scaledDuration(30 * 20, mult), scaledAmplifier(0, mult), false, true)),
            mult -> Text.translatable("tooltip.modulardelight.effect.surefooted.activated",
                    level(scaledAmplifier(0, mult)), scaledDuration(30 * 20, mult) / 20),
            300
    ));

    private static void igniteNearbyEnemies(ServerPlayerEntity player, double mult) {
        World world = player.getWorld();
        if (world.isClient) return;
        int fireTicks = scaledDuration(5 * 20, mult);
        Box area = player.getBoundingBox().expand(4.0);
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, area,
                e -> e != player && e.isAlive())) {
            entity.setFireTicks(Math.max(entity.getFireTicks(), fireTicks));
        }
        world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    private static void teleportRandomly(ServerPlayerEntity player) {
        if (player.getWorld().isClient) return;
        ServerWorld world = (ServerWorld) player.getWorld();
        double range = 8.0;

        for (int attempt = 0; attempt < 16; attempt++) {
            double x = player.getX() + (RANDOM.nextDouble() - 0.5) * range;
            double y = player.getY() + (RANDOM.nextInt((int) range) - range / 2);
            double z = player.getZ() + (RANDOM.nextDouble() - 0.5) * range;

            BlockPos dest = BlockPos.ofFloored(x, y, z);
            if (world.isAir(dest) && world.isAir(dest.up())) {
                player.networkHandler.requestTeleport(x, y, z, player.getYaw(), player.getPitch());
                world.playSound(null, x, y, z, SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT,
                        SoundCategory.PLAYERS, 1.0F, 1.0F);
                break;
            }
        }
    }

    private static boolean isNight(PlayerEntity player) {
        long time = player.getWorld().getTimeOfDay() % 24000L;
        return time >= 13000L && time <= 23000L;
    }

    private static MealEffect register(MealEffect effect) {
        REGISTRY.put(effect.id(), effect);
        return effect;
    }

    public static MealEffect byId(String id) {
        return REGISTRY.get(id);
    }

    public static Iterable<MealEffect> all() {
        return REGISTRY.values();
    }

    public static MealEffect byProperty(MealProperty property) {
        for (MealEffect effect : REGISTRY.values()) {
            if (effect.property() == property) return effect;
        }
        return null;
    }

    public List<Text> getMealTooltip() {
        double mult = difficulty().multiplier();

        return List.of(
                Text.translatable(
                        "tooltip.modulardelight.meal.description.line1",
                        ambientDescription().apply(mult)
                ),
                Text.translatable(
                        "tooltip.modulardelight.meal.description.line2",
                        conditionDescription(),
                        activatedDescription().apply(mult)
                )
        );
    }

    public List<Text> getActiveTooltip() {
        double mult = difficulty().multiplier();

        return List.of(
                Text.translatable(
                        "tooltip.modulardelight.active.line1",
                        ambientDescription().apply(mult)
                ),
                Text.translatable(
                        "tooltip.modulardelight.active.line2",
                        conditionDescription(),
                        activatedDescription().apply(mult)
                )
        );
    }

    public static MealEffect combine(MealEffect ambientSource, MealEffect conditionSource, MealEffect activatedSource) {
        String id = ambientSource.id() + "+" + conditionSource.id() + "+" + activatedSource.id();
        return new MealEffect(
                id,
                ambientSource.property(),
                ambientSource.ambientAttribute(),
                ambientSource.ambientBaseAmount(),
                ambientSource.ambientOperation(),
                ambientSource.ambientDescription(),
                ambientSource.ambientStatusEffect(),
                conditionSource.tickTrigger(),
                conditionSource.damageTrigger(),
                conditionSource.attackTrigger(),
                conditionSource.difficulty(),
                conditionSource.conditionDescription(),
                activatedSource.activatedAction(),
                activatedSource.activatedDescription(),
                conditionSource.triggerCooldownTicks()
        );
    }
}