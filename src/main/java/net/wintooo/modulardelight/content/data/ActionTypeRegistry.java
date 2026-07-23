package net.wintooo.modulardelight.content.data;

import com.google.gson.JsonObject;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.wintooo.modulardelight.ModularDelight;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.math.Vec3d;
import com.google.gson.JsonElement;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public final class ActionTypeRegistry {
    private static final Map<Identifier, ActionType> TYPES = new HashMap<>();
    private static final Random RANDOM = new Random();

    private ActionTypeRegistry() {}

    public static void register(Identifier id, ActionType type) {
        TYPES.put(id, type);
    }

    public static ParsedAction parse(JsonObject json) {
        Identifier type = EffectJson.id(json, "type");
        ActionType factory = TYPES.get(type);
        if (factory == null) throw new IllegalArgumentException("Unknown modular delight action type: " + type);
        return factory.parse(json);
    }

    public static void bootstrap() {
        register(ModularDelight.id("status_effect"), ActionTypeRegistry::parseStatusEffect);
        register(ModularDelight.id("multi"), ActionTypeRegistry::parseMulti);
        register(ModularDelight.id("explosion"), ActionTypeRegistry::parseExplosion);
        register(ModularDelight.id("teleport_random"), ActionTypeRegistry::parseTeleportRandom);
        register(ModularDelight.id("ignite_nearby"), ActionTypeRegistry::parseIgniteNearby);
        register(ModularDelight.id("damage_nearby"), ActionTypeRegistry::parseDamageNearby);
        register(ModularDelight.id("status_effect_nearby"), ActionTypeRegistry::parseStatusEffectNearby);
        register(ModularDelight.id("knockback_nearby"), ActionTypeRegistry::parseKnockbackNearby);
        register(ModularDelight.id("add_velocity"), ActionTypeRegistry::parseAddVelocity);
        register(ModularDelight.id("set_velocity"), ActionTypeRegistry::parseSetVelocity);
        register(ModularDelight.id("run_command"), ActionTypeRegistry::parseRunCommand);
        register(ModularDelight.id("if"), ActionTypeRegistry::parseIf);
        register(ModularDelight.id("chance"), ActionTypeRegistry::parseChance);
        register(ModularDelight.id("choice"), ActionTypeRegistry::parseChoice);
        register(ModularDelight.id("delay"), ActionTypeRegistry::parseDelay);
        register(ModularDelight.id("nothing"), ActionTypeRegistry::parseNothing);
        registerDelayScheduler();
    }

    private static ParsedAction parseStatusEffect(JsonObject json) {
        Identifier effectId = EffectJson.id(json, "effect");
        StatusEffect effect = Registries.STATUS_EFFECT.get(effectId);
        if (effect == null) throw new IllegalArgumentException("Unknown status effect: " + effectId);

        int baseDuration = EffectJson.intg(json, "duration_ticks", 100);
        int minDuration = EffectJson.intg(json, "min_duration_ticks", 20);
        boolean scaleDuration = EffectJson.bool(json, "scale_duration", true);
        int baseAmplifier = EffectJson.intg(json, "amplifier", 0);
        boolean scaleAmplifier = EffectJson.bool(json, "scale_amplifier", false);
        boolean showParticles = EffectJson.bool(json, "show_particles", true);
        boolean showIcon = EffectJson.bool(json, "show_icon", true);

        Function<Double, Integer> duration = mult -> scaleDuration
                ? Math.max(minDuration, Math.round(baseDuration * mult.floatValue()))
                : baseDuration;
        Function<Double, Integer> amplifier = mult -> scaleAmplifier
                ? EffectMath.scaledAmplifier(baseAmplifier, mult)
                : baseAmplifier;
        Text effectName = effect.getName();

        return new ParsedAction(
                (player, mult) -> player.addStatusEffect(new StatusEffectInstance(
                        effect, duration.apply(mult), amplifier.apply(mult), false, showParticles, showIcon)),
                mult -> scaleAmplifier
                        ? new Object[]{ EffectMath.level(amplifier.apply(mult)), duration.apply(mult) / 20 }
                        : new Object[]{ duration.apply(mult) / 20 },
                mult -> scaleAmplifier
                        ? Text.literal("gain ").append(effectName)
                            .append(" " + EffectMath.level(amplifier.apply(mult)) + " for " + (duration.apply(mult) / 20) + "s")
                        : Text.literal("gain ").append(effectName).append(" for " + (duration.apply(mult) / 20) + "s")
        );
    }

    private static ParsedAction parseMulti(JsonObject json) {
        List<ParsedAction> sub = new ArrayList<>();
        json.getAsJsonArray("actions").forEach(el -> sub.add(parse(el.getAsJsonObject())));

        return new ParsedAction(
                (player, mult) -> sub.forEach(a -> a.action().accept(player, mult)),
                mult -> sub.stream().flatMap(a -> Arrays.stream(a.describeArgs().apply(mult))).toArray(),
                mult -> {
                    MutableText combined = null;
                    for (ParsedAction a : sub) {
                        Text part = a.defaultDescription().apply(mult);
                        combined = combined == null ? part.copy() : combined.append(" & ").append(part);
                    }
                    return combined == null ? Text.literal("") : combined;
                }
        );
    }

    private static ParsedAction parseExplosion(JsonObject json) {
        double basePower = EffectJson.dbl(json, "power", 2.0);
        double maxPower = EffectJson.dbl(json, "max_power", 4.0);

        return new ParsedAction(
                (player, mult) -> {
                    World world = player.getWorld();
                    if (world.isClient) return;
                    float power = (float) Math.min(basePower * mult, maxPower);
                    world.createExplosion(player, player.getX(), player.getY(), player.getZ(),
                            power, false, World.ExplosionSourceType.NONE);
                },
                mult -> new Object[0],
                mult -> Text.literal("erupt in an explosion")
        );
    }

    private static ParsedAction parseTeleportRandom(JsonObject json) {
        double range = EffectJson.dbl(json, "range", 8.0);
        int attempts = EffectJson.intg(json, "attempts", 16);

        return new ParsedAction(
                (player, mult) -> {
                    if (player.getWorld().isClient) return;
                    ServerWorld world = (ServerWorld) player.getWorld();

                    for (int i = 0; i < attempts; i++) {
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
                },
                mult -> new Object[0],
                mult -> Text.literal("teleport a short distance")
        );
    }

    private static ParsedAction parseIgniteNearby(JsonObject json) {
        double radius = EffectJson.dbl(json, "radius", 4.0);
        int baseFireTicks = EffectJson.intg(json, "fire_ticks", 100);
        boolean scale = EffectJson.bool(json, "scale_duration", true);

        Function<Double, Integer> fireTicks = mult ->
                scale ? EffectMath.scaledDuration(baseFireTicks, mult) : baseFireTicks;

        return new ParsedAction(
                (player, mult) -> {
                    World world = player.getWorld();
                    if (world.isClient) return;
                    int ticks = fireTicks.apply(mult);
                    Box area = player.getBoundingBox().expand(radius);
                    for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, area,
                            e -> e != player && e.isAlive())) {
                        entity.setFireTicks(Math.max(entity.getFireTicks(), ticks));
                    }
                    world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE,
                            SoundCategory.PLAYERS, 1.0F, 1.0F);
                },
                mult -> new Object[]{ fireTicks.apply(mult) / 20 },
                mult -> Text.literal("ignite nearby enemies for " + (fireTicks.apply(mult) / 20) + "s")
        );
    }

    private static ParsedAction parseDamageNearby(JsonObject json) {
        double radius = EffectJson.dbl(json, "radius", 3.0);
        double baseDamage = EffectJson.dbl(json, "damage", 2.0);
        boolean scale = EffectJson.bool(json, "scale_with_multiplier", true);

        return new ParsedAction(
                (player, mult) -> {
                    World world = player.getWorld();
                    if (world.isClient) return;
                    float damage = (float) (scale ? baseDamage * mult : baseDamage);
                    Box area = player.getBoundingBox().expand(radius);
                    DamageSource source = world.getDamageSources().thorns(player);
                    for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, area,
                            e -> e != player && e.isAlive())) {
                        entity.damage(source, damage);
                    }
                    world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH,
                            SoundCategory.PLAYERS, 1.0F, 0.8F);
                },
                mult -> new Object[0],
                mult -> Text.literal("spike everything around you")
        );
    }

    private static ParsedAction parseStatusEffectNearby(JsonObject json) {
        Identifier effectId = EffectJson.id(json, "effect");
        StatusEffect effect = Registries.STATUS_EFFECT.get(effectId);
        double radius = EffectJson.dbl(json, "radius", 8.0);
        int baseDuration = EffectJson.intg(json, "duration_ticks", 200);
        boolean scale = EffectJson.bool(json, "scale_duration", true);
        int amplifier = EffectJson.intg(json, "amplifier", 0);
        String filter = json.has("target") ? json.get("target").getAsString() : "hostile";

        Predicate<LivingEntity> targetFilter = "hostile".equals(filter) ? e -> e instanceof HostileEntity : e -> true;

        return new ParsedAction(
                (player, mult) -> {
                    World world = player.getWorld();
                    if (world.isClient || effect == null) return;
                    int duration = scale ? EffectMath.scaledDuration(baseDuration, mult) : baseDuration;
                    Box area = player.getBoundingBox().expand(radius);
                    for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, area,
                            e -> e != player && e.isAlive() && targetFilter.test(e))) {
                        entity.addStatusEffect(new StatusEffectInstance(effect, duration, amplifier, true, false));
                    }
                },
                mult -> new Object[0],
                mult -> Text.literal("reveal nearby enemies")
        );
    }

    private static ParsedAction parseKnockbackNearby(JsonObject json) {
        double radius = EffectJson.dbl(json, "radius", 4.0);
        double baseStrength = EffectJson.dbl(json, "strength", 0.6);
        double vertical = EffectJson.dbl(json, "vertical", 0.25);
        boolean scale = EffectJson.bool(json, "scale_with_multiplier", true);

        return new ParsedAction(
                (player, mult) -> {
                    World world = player.getWorld();
                    if (world.isClient) return;
                    double strength = scale ? baseStrength * mult : baseStrength;
                    Box area = player.getBoundingBox().expand(radius);
                    for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, area,
                            e -> e != player && e.isAlive())) {
                        double dx = entity.getX() - player.getX();
                        double dz = entity.getZ() - player.getZ();
                        double dist = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
                        entity.addVelocity((dx / dist) * strength, vertical, (dz / dist) * strength);
                        entity.velocityModified = true;
                    }
                    world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                            SoundCategory.PLAYERS, 1.0F, 1.0F);
                },
                mult -> new Object[0],
                mult -> Text.literal("knock back nearby enemies")
        );
    }

    private static ParsedAction parseAddVelocity(JsonObject json) {
        double x = EffectJson.dbl(json, "x", 0.0);
        double y = EffectJson.dbl(json, "y", 0.0);
        double z = EffectJson.dbl(json, "z", 0.0);
        boolean relativeToLook = EffectJson.bool(json, "relative_to_look", false);
        boolean scaleWithMultiplier = EffectJson.bool(json, "scale_with_multiplier", true);

        return new ParsedAction(
                (player, mult) -> {
                    if (player.getWorld().isClient) return;
                    double scale = scaleWithMultiplier ? mult : 1.0;
                    Vec3d delta;
                    if (relativeToLook) {
                        double rad = Math.toRadians(player.getYaw());
                        double forward = x * scale;
                        double side = z * scale;
                        double dx = -Math.sin(rad) * forward + Math.cos(rad) * side;
                        double dz = Math.cos(rad) * forward + Math.sin(rad) * side;
                        delta = new Vec3d(dx, y * scale, dz);
                    } else {
                        delta = new Vec3d(x * scale, y * scale, z * scale);
                    }
                    player.addVelocity(delta.x, delta.y, delta.z);
                    player.velocityModified = true;
                    player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
                },
                mult -> new Object[0],
                mult -> Text.literal("get a burst of momentum")
        );
    }

    private static ParsedAction parseSetVelocity(JsonObject json) {
        double x = EffectJson.dbl(json, "x", 0.0);
        double y = EffectJson.dbl(json, "y", 0.0);
        double z = EffectJson.dbl(json, "z", 0.0);
        boolean scaleWithMultiplier = EffectJson.bool(json, "scale_with_multiplier", true);

        return new ParsedAction(
                (player, mult) -> {
                    if (player.getWorld().isClient) return;
                    double scale = scaleWithMultiplier ? mult : 1.0;
                    player.setVelocity(x * scale, y * scale, z * scale);
                    player.velocityModified = true;
                    player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
                },
                mult -> new Object[0],
                mult -> Text.literal("launch through the air")
        );
    }

    private static ParsedAction parseRunCommand(JsonObject json) {
        String commandTemplate = JsonHelper.getString(json, "command");
        int permissionLevel = EffectJson.intg(json, "permission_level", 2);
        boolean silent = EffectJson.bool(json, "silent", true);
        int multiplierDecimals = EffectJson.intg(json, "multiplier_decimals", 2);

        return new ParsedAction(
                (player, mult) -> {
                    if (player.getWorld().isClient) return;
                    MinecraftServer server = player.getServer();
                    if (server == null) return;

                    BlockPos pos = player.getBlockPos();
                    String command = commandTemplate
                            .replace("{player}", player.getGameProfile().getName())
                            .replace("{x}", String.valueOf(pos.getX()))
                            .replace("{y}", String.valueOf(pos.getY()))
                            .replace("{z}", String.valueOf(pos.getZ()))
                            .replace("{multiplier}", EffectJson.formatDecimal(mult, multiplierDecimals));

                    ServerCommandSource source = player.getCommandSource().withLevel(permissionLevel);
                    if (silent) source = source.withSilent();

                    server.getCommandManager().executeWithPrefix(source, command);
                },
                mult -> new Object[0],
                mult -> Text.literal("trigger something")
        );
    }

    private static ParsedAction parseIf(JsonObject json) {
        JsonObject conditionJson = json.getAsJsonObject("condition");
        ParsedTrigger condition = TriggerTypeRegistry.parseWithModifiers(conditionJson);
        if (condition.category() != TriggerCategory.TICK) {
            throw new IllegalArgumentException(
                    "'if' actions can only gate on tick-category conditions (the player's state " +
                            "at the moment the effect fires) — not damage/attack/eat conditions");
        }

        ParsedAction thenAction = parse(json.getAsJsonObject("then"));
        ParsedAction elseAction = json.has("else") ? parse(json.getAsJsonObject("else")) : null;

        return new ParsedAction(
                (player, mult) -> {
                    if (condition.tick().test(player)) {
                        thenAction.action().accept(player, mult);
                    } else if (elseAction != null) {
                        elseAction.action().accept(player, mult);
                    }
                },
                mult -> thenAction.describeArgs().apply(mult),
                mult -> thenAction.defaultDescription().apply(mult)
        );
    }

    private static ParsedAction parseChance(JsonObject json) {
        float chance = EffectJson.flt(json, "chance", 1.0f);
        ParsedAction action = parse(json.getAsJsonObject("action"));
        ParsedAction failAction = json.has("fail_action") ? parse(json.getAsJsonObject("fail_action")) : null;

        return new ParsedAction(
                (player, mult) -> {
                    if (RANDOM.nextFloat() < chance) {
                        action.action().accept(player, mult);
                    } else if (failAction != null) {
                        failAction.action().accept(player, mult);
                    }
                },
                mult -> action.describeArgs().apply(mult),
                mult -> action.defaultDescription().apply(mult)
        );
    }

    private record WeightedAction(ParsedAction action, int weight) {}

    private static ParsedAction parseChoice(JsonObject json) {
        List<WeightedAction> options = new ArrayList<>();
        int totalWeight = 0;
        for (JsonElement el : json.getAsJsonArray("actions")) {
            JsonObject entry = el.getAsJsonObject();
            ParsedAction action = parse(entry.getAsJsonObject("element"));
            int weight = EffectJson.intg(entry, "weight", 1);
            options.add(new WeightedAction(action, weight));
            totalWeight += weight;
        }
        if (options.isEmpty()) throw new IllegalArgumentException("'choice' action requires at least one entry in 'actions'");
        int finalTotalWeight = Math.max(1, totalWeight);

        return new ParsedAction(
                (player, mult) -> {
                    int roll = RANDOM.nextInt(finalTotalWeight);
                    int cumulative = 0;
                    for (WeightedAction option : options) {
                        cumulative += option.weight();
                        if (roll < cumulative) {
                            option.action().action().accept(player, mult);
                            return;
                        }
                    }
                },
                mult -> new Object[0],
                mult -> Text.literal("do something unpredictable")
        );
    }

    private static final List<PendingDelay> PENDING_DELAYS = new CopyOnWriteArrayList<>();

    private record PendingDelay(UUID playerUuid, long fireAtTick, ParsedAction action, double multiplier) {}

    private static void registerDelayScheduler() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (PENDING_DELAYS.isEmpty()) return;
            long currentTick = server.getOverworld().getTime();

            for (PendingDelay pending : PENDING_DELAYS) {
                if (currentTick < pending.fireAtTick()) continue;
                PENDING_DELAYS.remove(pending);

                ServerPlayerEntity player = server.getPlayerManager().getPlayer(pending.playerUuid());
                if (player != null) {
                    pending.action().action().accept(player, pending.multiplier());
                }
            }
        });
    }

    private static ParsedAction parseDelay(JsonObject json) {
        int ticks = EffectJson.intg(json, "ticks", 20);
        ParsedAction action = parse(json.getAsJsonObject("action"));

        return new ParsedAction(
                (player, mult) -> {
                    if (player.getWorld().isClient) return;
                    long fireAtTick = player.getServerWorld().getTime() + ticks;
                    PENDING_DELAYS.add(new PendingDelay(player.getUuid(), fireAtTick, action, mult));
                },
                mult -> action.describeArgs().apply(mult),
                mult -> action.defaultDescription().apply(mult)
        );
    }

    private static ParsedAction parseNothing(JsonObject json) {
        return new ParsedAction(
                (player, mult) -> {},
                mult -> new Object[0],
                mult -> Text.literal("do nothing")
        );
    }
}