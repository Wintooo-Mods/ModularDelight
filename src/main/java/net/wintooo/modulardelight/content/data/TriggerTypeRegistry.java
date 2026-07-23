package net.wintooo.modulardelight.content.data;

import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.wintooo.modulardelight.ModularDelight;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

public final class TriggerTypeRegistry {
    private static final Map<Identifier, TriggerType> TYPES = new HashMap<>();
    private static final Random RANDOM = new Random();

    private TriggerTypeRegistry() {}

    public static void register(Identifier id, TriggerType type) {
        TYPES.put(id, type);
    }

    public static TriggerType get(Identifier id) {
        return TYPES.get(id);
    }

    public static ParsedTrigger parseWithModifiers(JsonObject json) {
        Identifier type = EffectJson.id(json, "type");
        TriggerType factory = TYPES.get(type);
        if (factory == null) throw new IllegalArgumentException("Unknown trigger type: " + type);

        ParsedTrigger trigger = factory.parse(json);
        return EffectJson.bool(json, "inverted", false) ? invert(trigger) : trigger;
    }

    private static ParsedTrigger invert(ParsedTrigger trigger) {
        Text description = Text.literal("not (").append(trigger.defaultDescription()).append(")");
        return switch (trigger.category()) {
            case TICK -> ParsedTrigger.tick(player -> !trigger.tick().test(player), description);
            case DAMAGE -> ParsedTrigger.damage(
                    (player, source, amount) -> !trigger.damage().test(player, source, amount), description);
            case ATTACK -> ParsedTrigger.attack(
                    (attacker, target, dealt) -> !trigger.attack().test(attacker, target, dealt), description);
            case EAT -> ParsedTrigger.eat((player, food) -> !trigger.eat().test(player, food), description);
        };
    }

    public static void bootstrap() {
        register(ModularDelight.id("sprinting"), json ->
                ParsedTrigger.tick(PlayerEntity::isSprinting, Text.literal("you start sprinting")));

        register(ModularDelight.id("sneaking"), json ->
                ParsedTrigger.tick(PlayerEntity::isSneaking, Text.literal("you sneak")));

        register(ModularDelight.id("submerged"), json ->
                ParsedTrigger.tick(PlayerEntity::isSubmergedInWater, Text.literal("you are submerged in water")));

        register(ModularDelight.id("touching_ground"), json ->
                ParsedTrigger.tick(PlayerEntity::isOnGround, Text.literal("you touch the ground")));

        register(ModularDelight.id("in_block"), json -> {
            List<Identifier> blocks = EffectJson.idList(json, "blocks");
            List<Identifier> blockTags = EffectJson.idList(json, "block_tags");
            boolean checkFeet = EffectJson.bool(json, "check_feet", true);
            boolean checkHead = EffectJson.bool(json, "check_head", false);

            return ParsedTrigger.tick(player -> {
                World world = player.getWorld();
                if (checkFeet && matchesBlock(world, player.getBlockPos(), blocks, blockTags)) return true;
                return checkHead && matchesBlock(world, player.getBlockPos().up(), blocks, blockTags);
            }, Text.literal("you stand in the right spot"));
        });

        register(ModularDelight.id("rising"), json -> {
            double minVelocity = EffectJson.dbl(json, "min_velocity", 0.1);
            return ParsedTrigger.tick(
                    player -> !player.isOnGround() && player.getVelocity().y > minVelocity,
                    Text.literal("you rise"));
        });

        register(ModularDelight.id("falling_fast"), json -> {
            double minSpeed = EffectJson.dbl(json, "min_speed", 0.5);
            return ParsedTrigger.tick(
                    player -> !player.isOnGround() && player.getVelocity().y < -minSpeed,
                    Text.literal("you fall"));
        });

        register(ModularDelight.id("night"), json -> {
            long start = EffectJson.intg(json, "start_time", 13000);
            long end = EffectJson.intg(json, "end_time", 23000);
            return ParsedTrigger.tick(player -> {
                long time = player.getWorld().getTimeOfDay() % 24000L;
                return time >= start && time <= end;
            }, Text.literal("it's night"));
        });

        register(ModularDelight.id("low_health"), json -> {
            double threshold = EffectJson.dbl(json, "threshold", 10.0);
            return ParsedTrigger.tick(
                    player -> player.getHealth() < threshold,
                    Text.literal("you drop below " + (threshold / 2.0) + " hearts"));
        });

        register(ModularDelight.id("random_chance"), json -> {
            float chance = EffectJson.flt(json, "chance", 0.002f);
            return ParsedTrigger.tick(
                    player -> RANDOM.nextFloat() < chance,
                    Text.literal("the dice are rolled"));
        });

        register(ModularDelight.id("damage_source"), json -> {
            double minAmount = EffectJson.dbl(json, "min_amount", 0.0);
            List<Identifier> types = EffectJson.idList(json, "damage_types");
            List<Identifier> tags = EffectJson.idList(json, "damage_type_tags");
            List<Identifier> attackerTypes = EffectJson.idList(json, "attacker_types");

            return ParsedTrigger.damage((player, source, amount) -> {
                if (amount < minAmount) return false;
                boolean hasConstraints = !types.isEmpty() || !tags.isEmpty() || !attackerTypes.isEmpty();
                if (!hasConstraints) return true;

                for (Identifier typeId : types) {
                    if (source.isOf(RegistryKey.of(RegistryKeys.DAMAGE_TYPE, typeId))) return true;
                }
                for (Identifier tagId : tags) {
                    TagKey<DamageType> tag = TagKey.of(RegistryKeys.DAMAGE_TYPE, tagId);
                    if (source.isIn(tag)) return true;
                }
                if (!attackerTypes.isEmpty() && source.getAttacker() != null) {
                    Identifier attackerId = Registries.ENTITY_TYPE.getId(source.getAttacker().getType());
                    return attackerTypes.contains(attackerId);
                }
                return false;
            }, Text.literal("you take damage"));
        });

        register(ModularDelight.id("near_death"), json -> {
            double threshold = EffectJson.dbl(json, "threshold", 2.0);
            return ParsedTrigger.damage((player, source, amount) -> {
                float after = player.getHealth() - amount;
                return after > 0.0f && after <= threshold;
            }, Text.literal("you survive a near-fatal hit"));
        });

        register(ModularDelight.id("attack"), json -> {
            double minAmount = EffectJson.dbl(json, "min_amount", 0.0);
            double minDistance = EffectJson.dbl(json, "min_distance", 0.0);
            return ParsedTrigger.attack((attacker, target, dealt) ->
                            dealt >= minAmount && attacker.distanceTo(target) >= minDistance,
                    Text.literal("you land a hit"));
        });

        register(ModularDelight.id("eat_any"), json ->
                ParsedTrigger.eat((player, food) -> true, Text.literal("you eat something")));

        register(ModularDelight.id("all_of"), json -> parseCombinator(json, true));
        register(ModularDelight.id("any_of"), json -> parseCombinator(json, false));
    }

    private static ParsedTrigger parseCombinator(JsonObject json, boolean requireAll) {
        List<ParsedTrigger> subs = new ArrayList<>();
        json.getAsJsonArray("conditions").forEach(el -> subs.add(parseWithModifiers(el.getAsJsonObject())));
        if (subs.isEmpty()) {
            throw new IllegalArgumentException((requireAll ? "all_of" : "any_of") + " requires at least one condition");
        }

        TriggerCategory category = subs.get(0).category();
        for (ParsedTrigger sub : subs) {
            if (sub.category() != category) {
                throw new IllegalArgumentException(
                        "All sub-conditions of a combined condition must share the same trigger category (tick/damage/attack/eat)");
            }
        }

        Text description = EffectJson.text(json, "description",
                Text.literal(requireAll ? "all conditions are met" : "any condition is met"));

        return switch (category) {
            case TICK -> ParsedTrigger.tick(
                    player -> matches(subs, requireAll, s -> s.tick().test(player)), description);
            case DAMAGE -> ParsedTrigger.damage(
                    (player, source, amount) -> matches(subs, requireAll, s -> s.damage().test(player, source, amount)), description);
            case ATTACK -> ParsedTrigger.attack(
                    (attacker, target, dealt) -> matches(subs, requireAll, s -> s.attack().test(attacker, target, dealt)), description);
            case EAT -> ParsedTrigger.eat(
                    (player, food) -> matches(subs, requireAll, s -> s.eat().test(player, food)), description);
        };
    }

    private static boolean matches(List<ParsedTrigger> subs, boolean requireAll, Predicate<ParsedTrigger> test) {
        for (ParsedTrigger sub : subs) {
            boolean result = test.test(sub);
            if (requireAll && !result) return false;
            if (!requireAll && result) return true;
        }
        return requireAll;
    }

    private static boolean matchesBlock(World world, BlockPos pos, List<Identifier> ids, List<Identifier> tags) {
        BlockState state = world.getBlockState(pos);
        if (!ids.isEmpty()) {
            Identifier blockId = Registries.BLOCK.getId(state.getBlock());
            if (ids.contains(blockId)) return true;
        }
        for (Identifier tagId : tags) {
            TagKey<Block> tag = TagKey.of(RegistryKeys.BLOCK, tagId);
            if (state.isIn(tag)) return true;
        }
        return false;
    }
}