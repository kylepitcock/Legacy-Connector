package legacyconnector.v1_6_4.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import legacyconnector.v1_6_4.facade.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.ai.GoalTask;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.v1_6_4.bridge.Redirects147;

/** The arithmetic behind the int-health era bridges: whole hit points, absolute AI speeds, the old light curve. */
class LegacyEntityFacadesTest {
    @Test
    void healthIsWholeHitPointsAndASliverStaysAlive() {
        assertEquals(0, EntityLivingBase.healthOf(0.0F));
        assertEquals(0, EntityLivingBase.healthOf(-3.0F));
        assertEquals(1, EntityLivingBase.healthOf(0.2F));
        assertEquals(20, EntityLivingBase.healthOf(20.0F));
        assertEquals(20, EntityLivingBase.healthOf(19.5F));
    }

    @Test
    void absoluteAiSpeedsBecomeMultipliersOfTheMobsOwnSpeed() {
        // a zombie of 1.5.2 passes its moveSpeed (0.23) as the speed: that is 1.0 times its movement speed attribute
        assertEquals(1.0, GoalTask.scale(0.23, 0.23), 1.0E-9);
        // a faster chase than its walking speed
        assertEquals(2.0, GoalTask.scale(0.46, 0.23), 1.0E-9);
        // no usable speed: the value is passed as is
        assertEquals(0.3, GoalTask.scale(0.3, 0.0), 1.0E-9);
    }

    @Test
    void lightBrightnessFollowsTheOldCurve() {
        assertEquals(0.05F, Redirects147.lightBrightness(0), 1.0E-6F);
        assertEquals(1.0F, Redirects147.lightBrightness(15), 1.0E-6F);
        float previous = -1;
        for (int level = 0; level <= 15; level++) {
            float b = Redirects147.lightBrightness(level);
            assertTrue(b > previous, "level " + level);
            previous = b;
        }
        // out of range levels are clamped
        assertEquals(Redirects147.lightBrightness(15), Redirects147.lightBrightness(40), 0.0F);
        // vanilla's table at level 8: f = 7/15, (1 - f) / (3f + 1) * 0.95 + 0.05
        assertEquals(0.2611F, Redirects147.lightBrightness(8), 1.0E-3F);
    }

    @Test
    void packedBrightnessGivesTheBrighterLight() {
        assertEquals(15, Redirects147.lightLevel(15 << 20));
        assertEquals(7, Redirects147.lightLevel(7 << 4));
        assertEquals(12, Redirects147.lightLevel(3 << 20 | 12 << 4));
        assertEquals(0, Redirects147.lightLevel(0));
    }
}
