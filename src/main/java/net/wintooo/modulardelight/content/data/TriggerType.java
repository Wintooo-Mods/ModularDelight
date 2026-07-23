package net.wintooo.modulardelight.content.data;

import com.google.gson.JsonObject;

@FunctionalInterface
public interface TriggerType {
    ParsedTrigger parse(JsonObject json);
}