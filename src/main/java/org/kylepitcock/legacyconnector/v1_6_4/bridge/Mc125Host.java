package org.kylepitcock.legacyconnector.v1_6_4.bridge;

/**
 * Whether Minecraft 1.2.5 hosting is on in this session: the loader sets the system property when it accepts a 1.2.5 jar (toggle
 * {@code experimental.minecraft125}, see {@code Mc125Jars.enabled()} in the loader, the one place that decides). Run-time code that only
 * 1.2.5 mods need but that sits on a path every version uses asks this, so with the toggle off it stays inert.
 */
public final class Mc125Host {
    public static final String PROPERTY = "legacyconnector.mc125.active";

    private Mc125Host() {}

    public static boolean active() {
        return Boolean.getBoolean(PROPERTY);
    }
}
