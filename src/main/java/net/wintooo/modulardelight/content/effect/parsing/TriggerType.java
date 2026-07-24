package net.wintooo.modulardelight.content.effect.parsing;

import com.google.gson.JsonObject;

@FunctionalInterface
public interface TriggerType {
    ParsedTrigger parse(JsonObject json);
}