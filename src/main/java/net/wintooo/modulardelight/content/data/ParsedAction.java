package net.wintooo.modulardelight.content.data;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.function.BiConsumer;
import java.util.function.Function;

public record ParsedAction(
        BiConsumer<ServerPlayerEntity, Double> action,
        Function<Double, Object[]> describeArgs,
        Function<Double, Text> defaultDescription
) {}