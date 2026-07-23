package net.wintooo.modulardelight.content.data;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.wintooo.modulardelight.content.item.custom.MealEffect;

import java.util.function.Predicate;

public record ParsedTrigger(
        TriggerCategory category,
        Predicate<PlayerEntity> tick,
        MealEffect.DamageTrigger damage,
        MealEffect.AttackTrigger attack,
        MealEffect.EatTrigger eat,
        Text defaultDescription
) {
    public static ParsedTrigger tick(Predicate<PlayerEntity> predicate, Text description) {
        return new ParsedTrigger(TriggerCategory.TICK, predicate, null, null, null, description);
    }

    public static ParsedTrigger damage(MealEffect.DamageTrigger trigger, Text description) {
        return new ParsedTrigger(TriggerCategory.DAMAGE, null, trigger, null, null, description);
    }

    public static ParsedTrigger attack(MealEffect.AttackTrigger trigger, Text description) {
        return new ParsedTrigger(TriggerCategory.ATTACK, null, null, trigger, null, description);
    }

    public static ParsedTrigger eat(MealEffect.EatTrigger trigger, Text description) {
        return new ParsedTrigger(TriggerCategory.EAT, null, null, null, trigger, description);
    }
}