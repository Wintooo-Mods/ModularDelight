package net.wintooo.modulardelight;

import net.fabricmc.api.ModInitializer;

import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModularDelight implements ModInitializer {
	public static final String MOD_ID = "modular-delight";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

	@Override
	public void onInitialize() {

	}
}
