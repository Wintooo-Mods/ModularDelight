package net.wintooo.modulardelight.content.meal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.wintooo.modulardelight.content.effect.parsing.ParsedAmbient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public record DigestionEffect(
        Identifier id,
        MealProperty property,
        List<ParsedAmbient.AmbientAttribute> ambientAttributes,
        List<Function<Double, StatusEffectInstance>> ambientStatusEffects,
        List<AmbientDamageReaction> ambientDamageReactions,
        boolean ambientAlwaysEdible,
        Function<Double, Text> ambientDescription,
        Predicate<PlayerEntity> tickTrigger,
        DamageTrigger damageTrigger,
        AttackTrigger attackTrigger,
        EatTrigger eatTrigger,
        double multiplier,
        Text conditionDescription,
        int triggerCooldownTicks,
        BiConsumer<ServerPlayerEntity, Double> activatedAction,
        Function<Double, Text> activatedDescription
) {
    @FunctionalInterface
    public interface DamageTrigger {
        boolean test(PlayerEntity player, DamageSource source, float amount);
    }

    @FunctionalInterface
    public interface AttackTrigger {
        boolean test(PlayerEntity attacker, LivingEntity target, float amountDealt);
    }

    @FunctionalInterface
    public interface EatTrigger {
        boolean test(PlayerEntity player, ItemStack food);
    }

    @FunctionalInterface
    public interface AmbientDamageReaction {
        void react(ServerPlayerEntity player, DamageSource source, float amount, double multiplier);
    }

    private static final Map<Identifier, DigestionEffect> REGISTRY = new LinkedHashMap<>();

    public UUID modifierId(int attributeIndex) {
        return UUID.nameUUIDFromBytes(("modulardelight:ambient:" + id + ":" + attributeIndex).getBytes());
    }

    public static void clear() {
        REGISTRY.clear();
    }

    public static void register(Identifier id, DigestionEffect effect) {
        REGISTRY.put(id, effect);
    }

    public static int count() {
        return REGISTRY.size();
    }

    public static DigestionEffect byId(Identifier id) {
        return id == null ? null : REGISTRY.get(id);
    }

    public static DigestionEffect byProperty(MealProperty property) {
        for (DigestionEffect effect : REGISTRY.values()) {
            if (effect.property() == property) return effect;
        }
        return null;
    }

    public List<Text> getMealTooltip() {
        return List.of(
                Text.translatable("tooltip.modulardelight.meal.description.line1",
                        ambientDescription().apply(multiplier())),
                Text.translatable("tooltip.modulardelight.meal.description.line2",
                        conditionDescription(),
                        activatedDescription().apply(multiplier()))
        );
    }

    public List<Text> getActiveTooltip() {
        return List.of(
                Text.translatable("tooltip.modulardelight.active.line1",
                        ambientDescription().apply(multiplier())),
                Text.translatable("tooltip.modulardelight.active.line2",
                        conditionDescription(),
                        activatedDescription().apply(multiplier()))
        );
    }

    public static String compositeKey(DigestionEffect ambient, DigestionEffect condition, DigestionEffect activated) {
        return ambient.id() + "+" + condition.id() + "+" + activated.id();
    }

    public static DigestionEffect combine(DigestionEffect ambientSource, DigestionEffect conditionSource, DigestionEffect activatedSource) {
        return new DigestionEffect(
                activatedSource.id(),
                ambientSource.property(),
                ambientSource.ambientAttributes(),
                ambientSource.ambientStatusEffects(),
                ambientSource.ambientDamageReactions(),
                ambientSource.ambientAlwaysEdible(),
                ambientSource.ambientDescription(),
                conditionSource.tickTrigger(),
                conditionSource.damageTrigger(),
                conditionSource.attackTrigger(),
                conditionSource.eatTrigger(),
                conditionSource.multiplier(),
                conditionSource.conditionDescription(),
                conditionSource.triggerCooldownTicks(),
                activatedSource.activatedAction(),
                activatedSource.activatedDescription()
        );
    }
}