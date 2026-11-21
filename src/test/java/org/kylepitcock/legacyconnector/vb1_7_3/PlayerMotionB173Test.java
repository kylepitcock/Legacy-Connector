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

    @Test void thrustAddedToReportedVelocityKeepsTheClientsOwnMomentum() {
        // a jetpack adds a little forward speed to the velocity the client reported (.2, .1, 0); the client meanwhile moves at (.25, .1, .05)
        double bx = .2, by = .1, bz = 0, vx = bx + .0024, vy = .3, vz = bz - .0024;
        int mask = PlayerMotionB173.mask(vx, vy, vz, bx, by, bz);
        assertEquals(7, mask);
        var after = PlayerMotionB173.add(new Vec3(.25, .1, .05), mask, vx - bx, vy - by, vz - bz);
        assertEquals(.2524, after.x, 1e-9);
        assertEquals(.3, after.y, 1e-9, "a component set from the reported value arrives as that value");
        assertEquals(.0476, after.z, 1e-9);
    }
}
