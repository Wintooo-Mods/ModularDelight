package net.wintooo.modulardelight.content.util;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.wintooo.modulardelight.content.effect.ModStatusEffects;
import net.wintooo.modulardelight.content.data.ParsedAmbient;
import net.wintooo.modulardelight.content.item.custom.MealEffect;

import java.util.*;
import java.util.function.Function;

import static net.wintooo.modulardelight.content.network.DigestionNetworking.sync;

public class DigestionManager {
    private record ActiveEffect(
            String key,
            MealEffect composite,
            MealEffect ambient,
            MealEffect condition,
            MealEffect activated,
            int remainingTicks
    ) {}

    private static final Map<UUID, Map<String, ActiveEffect>> ACTIVE = new HashMap<>();
    private static final Map<UUID, Map<String, Integer>> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Map<UUID, EntityAttribute>> APPLIED_MODIFIERS = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server ->
                server.getPlayerManager().getPlayerList().forEach(DigestionManager::tick));

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                onDamage(player, source, amount);
            }
            return true;
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return TypedActionResult.pass(stack);
            }

            FoodComponent food = stack.getItem().getFoodComponent();
            if (food == null || player.canConsume(food.isAlwaysEdible())) {
                return TypedActionResult.pass(stack);
            }

            if (!hasAlwaysEdibleAmbient(serverPlayer)) {
                return TypedActionResult.pass(stack);
            }

            player.setCurrentHand(hand);
            return TypedActionResult.consume(stack);
        });
    }

    public static void grant(
            ServerPlayerEntity player,
            MealEffect composite,
            MealEffect ambient,
            MealEffect condition,
            MealEffect activated,
            int durationTicks
    ) {
        String key = MealEffect.compositeKey(ambient, condition, activated);
        ACTIVE.computeIfAbsent(player.getUuid(), k -> new HashMap<>())
                .put(key, new ActiveEffect(key, composite, ambient, condition, activated, durationTicks));
        sync(player);
    }

    private static void tick(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        Map<String, ActiveEffect> active = ACTIVE.get(uuid);
        boolean hasDigestion = player.hasStatusEffect(ModStatusEffects.DIGESTION);

        if (active != null && !active.isEmpty()) {
            if (!hasDigestion) {
                active.clear();
                sync(player);
            } else {
                applyAmbientStatusEffects(player, active);
                tickActiveEffects(player, active);
            }
        }

        reconcileAmbientModifiers(player);
    }

    private static void applyAmbientStatusEffects(ServerPlayerEntity player, Map<String, ActiveEffect> active) {
        for (ActiveEffect entry : active.values()) {
            MealEffect effect = entry.composite();
            for (Function<Double, StatusEffectInstance> statusEffectFn : effect.ambientStatusEffects()) {
                StatusEffectInstance desired = statusEffectFn.apply(1.0);
                StatusEffectInstance current = player.getStatusEffect(desired.getEffectType());
                if (current == null || current.getDuration() < 100) {
                    player.addStatusEffect(new StatusEffectInstance(
                            desired.getEffectType(), desired.getDuration(), desired.getAmplifier(), true, false, false));
                }
            }
        }
    }

    private static void tickActiveEffects(ServerPlayerEntity player, Map<String, ActiveEffect> active) {
        Map<String, Integer> cooldowns = COOLDOWNS.computeIfAbsent(player.getUuid(), k -> new HashMap<>());
        Iterator<Map.Entry<String, ActiveEffect>> it = active.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, ActiveEffect> entry = it.next();
            ActiveEffect old = entry.getValue();
            MealEffect effect = old.composite();
            int remaining = old.remainingTicks() - 1;

            if (remaining <= 0) {
                it.remove();
                sync(player);
                continue;
            }
            entry.setValue(new ActiveEffect(old.key(), old.composite(), old.ambient(), old.condition(), old.activated(), remaining));

            if (effect.tickTrigger() == null) continue;

            int cooldown = cooldowns.getOrDefault(old.key(), 0);
            if (cooldown > 0) {
                cooldowns.put(old.key(), cooldown - 1);
            } else if (effect.tickTrigger().test(player)) {
                effect.activatedAction().accept(player, effect.multiplier());
                cooldowns.put(old.key(), effect.triggerCooldownTicks());
            }
        }
    }

    private static void onDamage(ServerPlayerEntity player, DamageSource source, float damageTaken) {
        Map<String, ActiveEffect> active = ACTIVE.get(player.getUuid());
        if (active == null || active.isEmpty()) return;

        for (ActiveEffect entry : active.values()) {
            MealEffect effect = entry.composite();
            for (MealEffect.AmbientDamageReaction reaction : effect.ambientDamageReactions()) {
                reaction.react(player, source, damageTaken, effect.multiplier());
            }
        }

        Map<String, Integer> cooldowns = COOLDOWNS.computeIfAbsent(player.getUuid(), k -> new HashMap<>());
        for (ActiveEffect entry : active.values()) {
            MealEffect effect = entry.composite();
            if (effect.damageTrigger() == null) continue;

            int cooldown = cooldowns.getOrDefault(entry.key(), 0);
            if (cooldown > 0) continue;

            if (effect.damageTrigger().test(player, source, damageTaken)) {
                effect.activatedAction().accept(player, effect.multiplier());
                cooldowns.put(entry.key(), effect.triggerCooldownTicks());
            }
        }
    }

    public static void onAttack(ServerPlayerEntity attacker, LivingEntity target, float damageDealt) {
        Map<String, ActiveEffect> active = ACTIVE.get(attacker.getUuid());
        if (active == null || active.isEmpty()) return;

        Map<String, Integer> cooldowns = COOLDOWNS.computeIfAbsent(attacker.getUuid(), k -> new HashMap<>());
        for (ActiveEffect entry : active.values()) {
            MealEffect effect = entry.composite();
            if (effect.attackTrigger() == null) continue;

            int cooldown = cooldowns.getOrDefault(entry.key(), 0);
            if (cooldown > 0) continue;

            if (effect.attackTrigger().test(attacker, target, damageDealt)) {
                effect.activatedAction().accept(attacker, effect.multiplier());
                cooldowns.put(entry.key(), effect.triggerCooldownTicks());
            }
        }
    }

    public static void onEatFood(ServerPlayerEntity player, ItemStack food) {
        Map<String, ActiveEffect> active = ACTIVE.get(player.getUuid());
        if (active == null || active.isEmpty()) return;

        Map<String, Integer> cooldowns = COOLDOWNS.computeIfAbsent(player.getUuid(), k -> new HashMap<>());
        for (ActiveEffect entry : active.values()) {
            MealEffect effect = entry.composite();
            if (effect.eatTrigger() == null) continue;

            int cooldown = cooldowns.getOrDefault(entry.key(), 0);
            if (cooldown > 0) continue;

            if (effect.eatTrigger().test(player, food)) {
                effect.activatedAction().accept(player, effect.multiplier());
                cooldowns.put(entry.key(), effect.triggerCooldownTicks());
            }
        }
    }

    public static boolean hasAlwaysEdibleAmbient(ServerPlayerEntity player) {
        Map<String, ActiveEffect> active = ACTIVE.get(player.getUuid());
        if (active == null) return false;
        for (ActiveEffect entry : active.values()) {
            if (entry.composite().ambientAlwaysEdible()) return true;
        }
        return false;
    }

    private static void reconcileAmbientModifiers(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        Map<String, ActiveEffect> active = ACTIVE.get(uuid);

        Map<UUID, ParsedAmbient.AmbientAttribute> desiredAttrs = new HashMap<>();
        Map<UUID, EntityAttributeModifier> desiredMods = new HashMap<>();
        if (active != null) {
            for (ActiveEffect entry : active.values()) {
                MealEffect effect = entry.composite();
                List<ParsedAmbient.AmbientAttribute> attrs = effect.ambientAttributes();
                for (int i = 0; i < attrs.size(); i++) {
                    ParsedAmbient.AmbientAttribute attr = attrs.get(i);
                    UUID modifierId = effect.modifierId(i);
                    desiredAttrs.put(modifierId, attr);
                    desiredMods.put(modifierId, new EntityAttributeModifier(
                            modifierId, "modulardelight:" + effect.id() + ":" + i,
                            attr.baseAmount() * effect.multiplier(), attr.operation()));
                }
            }
        }

        Map<UUID, EntityAttribute> applied = APPLIED_MODIFIERS.computeIfAbsent(uuid, k -> new HashMap<>());

        Iterator<Map.Entry<UUID, EntityAttribute>> appliedIt = applied.entrySet().iterator();
        while (appliedIt.hasNext()) {
            Map.Entry<UUID, EntityAttribute> entry = appliedIt.next();
            if (desiredAttrs.containsKey(entry.getKey())) continue;

            EntityAttributeInstance instance = player.getAttributeInstance(entry.getValue());
            if (instance != null) instance.removeModifier(entry.getKey());
            appliedIt.remove();
        }

        for (Map.Entry<UUID, ParsedAmbient.AmbientAttribute> entry : desiredAttrs.entrySet()) {
            UUID modifierId = entry.getKey();
            ParsedAmbient.AmbientAttribute attr = entry.getValue();
            if (applied.containsKey(modifierId)) continue;

            EntityAttributeInstance instance = player.getAttributeInstance(attr.attribute());
            if (instance == null) continue;

            instance.removeModifier(modifierId);
            instance.addTemporaryModifier(desiredMods.get(modifierId));
            applied.put(modifierId, attr.attribute());
        }

        if (applied.isEmpty()) {
            APPLIED_MODIFIERS.remove(uuid);
        }
    }

    public static List<ActiveMeal> getMeals(ServerPlayerEntity player) {
        Map<String, ActiveEffect> effects = ACTIVE.get(player.getUuid());
        if (effects == null) return List.of();

        return effects.values().stream()
                .map(effect -> new ActiveMeal(effect.ambient(), effect.condition(), effect.activated()))
                .toList();
    }

    private static final String NBT_KEY = "ModularDelightDigestion";

    public static void writeToNbt(ServerPlayerEntity player, NbtCompound nbt) {
        Map<String, ActiveEffect> active = ACTIVE.get(player.getUuid());
        if (active == null || active.isEmpty()) return;

        NbtList list = new NbtList();
        for (ActiveEffect effect : active.values()) {
            NbtCompound entry = new NbtCompound();
            entry.putString("Ambient", effect.ambient().id().toString());
            entry.putString("Condition", effect.condition().id().toString());
            entry.putString("Activated", effect.activated().id().toString());
            entry.putInt("Remaining", effect.remainingTicks());
            list.add(entry);
        }
        nbt.put(NBT_KEY, list);
    }

    public static void readFromNbt(ServerPlayerEntity player, NbtCompound nbt) {
        if (!nbt.contains(NBT_KEY, NbtElement.LIST_TYPE)) return;
        NbtList list = nbt.getList(NBT_KEY, NbtElement.COMPOUND_TYPE);

        Map<String, ActiveEffect> restored = new HashMap<>();
        for (NbtElement element : list) {
            NbtCompound entry = (NbtCompound) element;
            MealEffect ambient = MealEffect.byId(Identifier.tryParse(entry.getString("Ambient")));
            MealEffect condition = MealEffect.byId(Identifier.tryParse(entry.getString("Condition")));
            MealEffect activated = MealEffect.byId(Identifier.tryParse(entry.getString("Activated")));
            int remaining = entry.getInt("Remaining");

            if (ambient == null || condition == null || activated == null || remaining <= 0) continue;

            MealEffect composite = MealEffect.combine(ambient, condition, activated);
            String key = MealEffect.compositeKey(ambient, condition, activated);
            restored.put(key, new ActiveEffect(key, composite, ambient, condition, activated, remaining));
        }

        if (!restored.isEmpty()) {
            ACTIVE.put(player.getUuid(), restored);
        }
    }
}