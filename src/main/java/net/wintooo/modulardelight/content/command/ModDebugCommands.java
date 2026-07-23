package net.wintooo.modulardelight.content.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.wintooo.modulardelight.content.item.custom.MealEffect;
import net.wintooo.modulardelight.content.item.custom.MealProperty;

public final class ModDebugCommands {
    private ModDebugCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("modulardelight")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("properties").executes(ModDebugCommands::listProperties))
                .then(CommandManager.literal("effects").executes(ModDebugCommands::listEffects)));
    }

    private static int listProperties(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        source.sendFeedback(() -> Text.literal("Loaded properties (" + MealProperty.all().size() + "):"), false);
        for (MealProperty property : MealProperty.all()) {
            boolean hasEffect = MealEffect.byProperty(property) != null;
            source.sendFeedback(() -> Text.literal(" - " + property.id() + " \"" + property.name().getString() + "\""
                    + (hasEffect ? "" : " (no effect)")), false);
        }
        return MealProperty.all().size();
    }

    private static int listEffects(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        source.sendFeedback(() -> Text.literal("Loaded meal effects (" + MealEffect.count() + "):"), false);
        for (MealProperty property : MealProperty.all()) {
            MealEffect effect = MealEffect.byProperty(property);
            if (effect == null) continue;
            source.sendFeedback(() -> Text.literal(" - " + effect.id() + ": "
                    + effect.conditionDescription().getString() + " -> "
                    + effect.activatedDescription().apply(1.0).getString()
                    + " (x" + effect.multiplier() + ", cooldown " + effect.triggerCooldownTicks() + "t)"), false);
        }
        return MealEffect.count();
    }
}