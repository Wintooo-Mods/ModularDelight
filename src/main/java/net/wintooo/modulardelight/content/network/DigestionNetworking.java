package net.wintooo.modulardelight.content.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.wintooo.modulardelight.ModularDelight;
import net.wintooo.modulardelight.content.util.ActiveMeal;
import net.wintooo.modulardelight.content.util.DigestionManager;

import java.util.List;

public class DigestionNetworking {
    public static final Identifier DIGESTION_SYNC = ModularDelight.id("digestion_sync");

    public static void sync(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        List<ActiveMeal> meals = DigestionManager.getMeals(player);

        buf.writeVarInt(meals.size());
        for (ActiveMeal meal : meals) {
            buf.writeIdentifier(meal.ambient().id());
            buf.writeIdentifier(meal.condition().id());
            buf.writeIdentifier(meal.activated().id());
        }

        ServerPlayNetworking.send(player, DIGESTION_SYNC, buf);
    }
}