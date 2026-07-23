package net.wintooo.modulardelight.content.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.wintooo.modulardelight.content.item.custom.MealEffect;
import net.wintooo.modulardelight.content.util.ActiveMeal;
import net.wintooo.modulardelight.content.util.ClientDigestionManager;

import java.util.ArrayList;
import java.util.List;

public class ModClientNetworking {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(DigestionNetworking.DIGESTION_SYNC,
                (client, handler, buf, responseSender) -> {
                    int count = buf.readVarInt();
                    List<ActiveMeal> meals = new ArrayList<>();

                    for (int i = 0; i < count; i++) {
                        MealEffect ambient = MealEffect.byId(buf.readIdentifier());
                        MealEffect condition = MealEffect.byId(buf.readIdentifier());
                        MealEffect activated = MealEffect.byId(buf.readIdentifier());

                        if (ambient != null && condition != null && activated != null) {
                            meals.add(new ActiveMeal(ambient, condition, activated));
                        }
                    }

                    client.execute(() -> ClientDigestionManager.set(meals));
                });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientDigestionManager.clear());
    }
}