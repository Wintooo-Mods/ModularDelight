package net.wintooo.modulardelight.content.effect.parsing;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.wintooo.modulardelight.content.meal.DigestionEffect;

import java.util.function.Predicate;

public record ParsedTrigger(
        TriggerCategory category,
        Predicate<PlayerEntity> tick,
        DigestionEffect.DamageTrigger damage,
        DigestionEffect.AttackTrigger attack,
        DigestionEffect.EatTrigger eat,
        Text defaultDescription
) {
    public static ParsedTrigger tick(Predicate<PlayerEntity> predicate, Text description) {
        return new ParsedTrigger(TriggerCategory.TICK, predicate, null, null, null, description);
    }

    public static ParsedTrigger damage(DigestionEffect.DamageTrigger trigger, Text description) {
        return new ParsedTrigger(TriggerCategory.DAMAGE, null, trigger, null, null, description);
    }

    public static ParsedTrigger attack(DigestionEffect.AttackTrigger trigger, Text description) {
        return new ParsedTrigger(TriggerCategory.ATTACK, null, null, trigger, null, description);
    }

    public static ParsedTrigger eat(DigestionEffect.EatTrigger trigger, Text description) {
        return new ParsedTrigger(TriggerCategory.EAT, null, null, null, trigger, description);
    }
}