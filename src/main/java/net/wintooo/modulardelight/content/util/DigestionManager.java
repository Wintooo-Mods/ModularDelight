package net.wintooo.modulardelight.content.util;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import net.wintooo.modulardelight.content.effect.ModStatusEffects;
import net.wintooo.modulardelight.content.item.custom.MealEffect;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class DigestionManager {
    private record ActiveEffect(MealEffect effect, int remainingTicks) {}

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

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                forget(handler.getPlayer().getUuid()));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                clearAllKnownModifiers(handler.getPlayer()));
    }

    public static void grant(ServerPlayerEntity player, MealEffect effect, int durationTicks) {
        ACTIVE.computeIfAbsent(player.getUuid(), k -> new HashMap<>())
                .put(effect.id(), new ActiveEffect(effect, durationTicks));
    }

    private static void tick(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        Map<String, ActiveEffect> active = ACTIVE.get(uuid);
        boolean hasDigestion = player.hasStatusEffect(ModStatusEffects.DIGESTION);

        if (active != null && !active.isEmpty()) {
            if (!hasDigestion) {
                active.clear();
            } else {
                applyAmbientStatusEffects(player, active);
                tickActiveEffects(player, active);
            }
        }

        reconcileAmbientModifiers(player);
    }

    private static void applyAmbientStatusEffects(ServerPlayerEntity player, Map<String, ActiveEffect> active) {
        for (ActiveEffect entry : active.values()) {
            MealEffect effect = entry.effect();
            if (effect.ambientStatusEffect() == null) continue;

            StatusEffectInstance desired = effect.ambientStatusEffect().apply(1.0);
            StatusEffectInstance current = player.getStatusEffect(desired.getEffectType());
            if (current == null || current.getDuration() < 100) {
                player.addStatusEffect(new StatusEffectInstance(
                        desired.getEffectType(), desired.getDuration(), desired.getAmplifier(), true, false));
            }
        }
    }

    private static void tickActiveEffects(ServerPlayerEntity player, Map<String, ActiveEffect> active) {
        Map<String, Integer> cooldowns = COOLDOWNS.computeIfAbsent(player.getUuid(), k -> new HashMap<>());
        Iterator<Map.Entry<String, ActiveEffect>> it = active.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, ActiveEffect> entry = it.next();
            MealEffect effect = entry.getValue().effect();
            int remaining = entry.getValue().remainingTicks() - 1;

            if (remaining <= 0) {
                it.remove();
                continue;
            }
            entry.setValue(new ActiveEffect(effect, remaining));

            if (effect.tickTrigger() == null) continue;

            int cooldown = cooldowns.getOrDefault(effect.id(), 0);
            if (cooldown > 0) {
                cooldowns.put(effect.id(), cooldown - 1);
            } else if (effect.tickTrigger().test(player)) {
                effect.activatedAction().accept(player, effect.difficulty().multiplier());
                cooldowns.put(effect.id(), effect.triggerCooldownTicks());
            }
        }
    }

    private static void onDamage(ServerPlayerEntity player, DamageSource source, float damageTaken) {
        Map<String, ActiveEffect> active = ACTIVE.get(player.getUuid());
        if (active == null || active.isEmpty()) return;

        Map<String, Integer> cooldowns = COOLDOWNS.computeIfAbsent(player.getUuid(), k -> new HashMap<>());
        for (ActiveEffect entry : active.values()) {
            MealEffect effect = entry.effect();
            if (effect.damageTrigger() == null) continue;

            int cooldown = cooldowns.getOrDefault(effect.id(), 0);
            if (cooldown > 0) continue;

            if (effect.damageTrigger().test(player, source, damageTaken)) {
                effect.activatedAction().accept(player, effect.difficulty().multiplier());
                cooldowns.put(effect.id(), effect.triggerCooldownTicks());
            }
        }
    }

    public static void onAttack(ServerPlayerEntity attacker, LivingEntity target, float damageDealt) {
        Map<String, ActiveEffect> active = ACTIVE.get(attacker.getUuid());
        if (active == null || active.isEmpty()) return;

        Map<String, Integer> cooldowns = COOLDOWNS.computeIfAbsent(attacker.getUuid(), k -> new HashMap<>());
        for (ActiveEffect entry : active.values()) {
            MealEffect effect = entry.effect();
            if (effect.attackTrigger() == null) continue;

            int cooldown = cooldowns.getOrDefault(effect.id(), 0);
            if (cooldown > 0) continue;

            if (effect.attackTrigger().test(attacker, target, damageDealt)) {
                effect.activatedAction().accept(attacker, effect.difficulty().multiplier());
                cooldowns.put(effect.id(), effect.triggerCooldownTicks());
            }
        }
    }

    private static void reconcileAmbientModifiers(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        Map<String, ActiveEffect> active = ACTIVE.get(uuid);

        Map<UUID, MealEffect> desired = new HashMap<>();
        if (active != null) {
            for (ActiveEffect entry : active.values()) {
                MealEffect effect = entry.effect();
                if (effect.ambientAttribute() == null) continue;
                desired.put(effect.modifierId(), effect);
            }
        }

        Map<UUID, EntityAttribute> applied = APPLIED_MODIFIERS.computeIfAbsent(uuid, k -> new HashMap<>());

        Iterator<Map.Entry<UUID, EntityAttribute>> appliedIt = applied.entrySet().iterator();
        while (appliedIt.hasNext()) {
            Map.Entry<UUID, EntityAttribute> entry = appliedIt.next();
            UUID modifierId = entry.getKey();
            if (desired.containsKey(modifierId)) continue;

            EntityAttributeInstance instance = player.getAttributeInstance(entry.getValue());
            if (instance != null) instance.removeModifier(modifierId);
            appliedIt.remove();
        }

        for (Map.Entry<UUID, MealEffect> entry : desired.entrySet()) {
            UUID modifierId = entry.getKey();
            MealEffect effect = entry.getValue();
            if (applied.containsKey(modifierId)) continue;

            EntityAttributeInstance instance = player.getAttributeInstance(effect.ambientAttribute());
            if (instance == null) continue;

            instance.removeModifier(modifierId);
            instance.addTemporaryModifier(effect.scaledAmbientModifier(1.0));
            applied.put(modifierId, effect.ambientAttribute());
        }

        if (applied.isEmpty()) {
            APPLIED_MODIFIERS.remove(uuid);
        }
    }

    private static void forget(UUID playerId) {
        ACTIVE.remove(playerId);
        COOLDOWNS.remove(playerId);
        APPLIED_MODIFIERS.remove(playerId);
    }

    private static void clearAllKnownModifiers(ServerPlayerEntity player) {
        for (MealEffect effect : MealEffect.all()) {
            if (effect.ambientAttribute() == null) continue;
            EntityAttributeInstance instance = player.getAttributeInstance(effect.ambientAttribute());
            if (instance != null) instance.removeModifier(effect.modifierId());
        }
    }
}