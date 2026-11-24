package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Verifies the legacy MeleeAttackGoal range arithmetic retained by the bridge. */
class AlienEvoGruntMeleeGoal1201RuntimeTest {
    @Test
    void preservesVanillaWidthAndTargetAllowanceFormula() {
        float mobWidth = 1.5F;
        float targetWidth = 0.6F;
        double expected = (double) (mobWidth * 2.0F * mobWidth * 2.0F + targetWidth);

        assertEquals(expected, AlienEvoGruntMeleeGoal1201Runtime.baseAttackReachSqr(mobWidth, targetWidth));
    }

    @Test
    void supportsSmallAndLargeEntitiesWithoutClamping() {
        assertEquals((double) (0.25F * 2.0F * 0.25F * 2.0F + 0.3F),
                AlienEvoGruntMeleeGoal1201Runtime.baseAttackReachSqr(0.25F, 0.3F));
        assertEquals((double) (2.0F * 2.0F * 2.0F * 2.0F + 1.2F),
                AlienEvoGruntMeleeGoal1201Runtime.baseAttackReachSqr(2.0F, 1.2F));
    }
}
