package net.wintooo.modulardelight.content.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;
import net.wintooo.modulardelight.ModularDelight;
import net.wintooo.modulardelight.content.util.DigestionManager;

public final class KeyPressNetworking {
    public static final Identifier KEY_PRESS_C2S = ModularDelight.id("key_press");

    public static void registerServerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(KEY_PRESS_C2S, (server, player, handler, buf, sender) -> {
            Identifier keyId = buf.readIdentifier();
            server.execute(() -> DigestionManager.onKeyPress(player, keyId));
        });
    }
}