package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The guard behind the 1.4.7 texture index redirects (Mystcraft's BlockLinkModifier looped: getIcon -> getBlockTextureFromSide -> getIcon ...). */
class ReentryGuardTest {
    private final ReentryGuard guard = new ReentryGuard();

    /** A stand-in for the cycle: resolve() is the redirect, icon() the mod's getIcon that calls it again. */
    private int depth;

    private int resolve(Object block, int side) {
        if (!guard.enter(block, 2, side)) return -1;   // the fallback answer
        try {
            return icon(block, side);
        } finally {
            guard.leave();
        }
    }

    private int icon(Object block, int side) {
        if (++depth > 50) throw new StackOverflowError("not guarded");
        return resolve(block, side);
    }

    @Test
    void aNestedCallOfTheSameBlockAndSideGetsTheFallback() {
        assertEquals(-1, resolve(new Object(), 2));
        assertTrue(depth <= 2);
    }

    @Test
    void otherSidesOtherBlocksAndLaterCallsAreNotRefused() {
        Object a = new Object(), b = new Object();
        assertTrue(guard.enter(a, 2, 0));
        assertTrue(guard.enter(a, 2, 1));
        assertTrue(guard.enter(b, 2, 0));
        assertFalse(guard.enter(a, 2, 0));
        guard.leave();
        guard.leave();
        guard.leave();
        assertTrue(guard.enter(a, 2, 0));
        guard.leave();
    }
}
