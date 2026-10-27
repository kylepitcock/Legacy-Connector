package org.kylepitcock.legacyconnector.v1_12_2.bridge;

/** Start-up of the 1.12.2 world bridges: the world generator pass, the world events. */
public final class World1122 {
    private World1122() {}

    public static void register() {
        WorldGenBridge1122.hook();
        WorldEvents1122.register();
    }
}
