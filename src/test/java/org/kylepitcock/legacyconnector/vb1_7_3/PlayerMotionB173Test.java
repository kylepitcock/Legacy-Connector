package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/** Only velocity components an original callback changed replace the client's own player motion. */
class PlayerMotionB173Test {
    @Test void bounceChangesOnlyVerticalAndKeepsClientMomentum() {
        int mask = PlayerMotionB173.mask(.05, 2, -.01, .05, -.08, -.01);
        assertEquals(2, mask);
        assertEquals(new Vec3(.81, 2, .3), PlayerMotionB173.merge(new Vec3(.81, 1.2, .3), mask, .05, 2, -.01));
    }

    @Test void everyChangedComponentIsAppliedAndUnchangedOnesAreNot() {
        assertEquals(0, PlayerMotionB173.mask(1, 2, 3, 1, 2, 3));
        assertEquals(7, PlayerMotionB173.mask(0, 0, 0, 1, 2, 3));
        assertEquals(5, PlayerMotionB173.mask(0, 2, 0, 1, 2, 3));
        assertEquals(new Vec3(9, .5, 7), PlayerMotionB173.merge(new Vec3(4, .5, 6), 5, 9, 8, 7));
        assertEquals(25, PlayerMotionB173.encode(7, 1, 2, 3).length);
    }
}
