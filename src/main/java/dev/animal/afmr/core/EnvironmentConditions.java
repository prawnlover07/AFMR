package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.Settings;

/** Cheap local conditions; no library traversal or Minecraft dependencies. */
public final class EnvironmentConditions {
    private EnvironmentConditions() {}

    public static boolean underground(int blockY, boolean covered, double coverDepth, Settings settings) {
        return covered && blockY <= settings.undergroundMaxY && coverDepth >= settings.undergroundDepth;
    }

    public static boolean dark(String dimension, boolean covered, int lightLevel, Settings settings) {
        if (dimension.equals("minecraft:the_nether")) return settings.darkInNether;
        if (dimension.equals("minecraft:the_end")) return settings.darkInEnd;
        return covered && lightLevel <= settings.darkMaxLight;
    }
}
