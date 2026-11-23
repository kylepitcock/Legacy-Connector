package org.kylepitcock.legacyconnector.v1_12_2.client;

/**
 * Whether RenderWorldLastEvent is being posted on the render thread. Kept apart from {@link ClientEvents1122} so the event bus can ask on a
 * dedicated server without loading a class that names client types.
 */
public final class RenderShift1122 {
    public static volatile boolean worldLast;

    private RenderShift1122() {}
}
