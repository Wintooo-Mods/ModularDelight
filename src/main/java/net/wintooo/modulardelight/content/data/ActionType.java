package net.wintooo.modulardelight.content.data;

import com.google.gson.JsonObject;

@FunctionalInterface
public interface ActionType {
    ParsedAction parse(JsonObject json);
}