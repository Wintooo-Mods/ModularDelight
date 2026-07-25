package net.wintooo.modulardelight.content.input;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.wintooo.modulardelight.ModularDelight;
import net.wintooo.modulardelight.content.network.KeyPressNetworking;

public final class ModKeyBindings {
    private static final KeyBinding ACTION_1 = new KeyBinding(
            "key.modulardelight.action_1", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), "key.categories.modulardelight");
    private static final KeyBinding ACTION_2 = new KeyBinding(
            "key.modulardelight.action_2", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), "key.categories.modulardelight");
    private static final KeyBinding ACTION_3 = new KeyBinding(
            "key.modulardelight.action_3", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), "key.categories.modulardelight");
    private static final KeyBinding ACTION_4 = new KeyBinding(
            "key.modulardelight.action_4", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), "key.categories.modulardelight");

    public static void register() {
        KeyBindingHelper.registerKeyBinding(ACTION_1);
        KeyBindingHelper.registerKeyBinding(ACTION_2);
        KeyBindingHelper.registerKeyBinding(ACTION_3);
        KeyBindingHelper.registerKeyBinding(ACTION_4);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            checkAndSend(ACTION_1, ModularDelight.id("action_1"));
            checkAndSend(ACTION_2, ModularDelight.id("action_2"));
            checkAndSend(ACTION_3, ModularDelight.id("action_3"));
            checkAndSend(ACTION_4, ModularDelight.id("action_4"));
        });
    }

    private static void checkAndSend(KeyBinding binding, Identifier keyId) {
        while (binding.wasPressed()) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeIdentifier(keyId);
            ClientPlayNetworking.send(KeyPressNetworking.KEY_PRESS_C2S, buf);
        }
    }
}