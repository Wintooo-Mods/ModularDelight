package net.wintooo.modulardelight.content.data;

public final class EffectMath {
    private EffectMath() {}

    public static int scaledDuration(int baseTicks, double multiplier) {
        return Math.max(20, Math.round(baseTicks * (float) multiplier));
    }

    public static int scaledAmplifier(int baseAmplifier, double multiplier) {
        return Math.max(0, baseAmplifier + (int) Math.floor(multiplier - 1.0));
    }

    public static String level(int amplifier) {
        String[] numerals = {"I", "II", "III", "IV", "V", "VI"};
        return numerals[Math.min(amplifier, numerals.length - 1)];
    }
}