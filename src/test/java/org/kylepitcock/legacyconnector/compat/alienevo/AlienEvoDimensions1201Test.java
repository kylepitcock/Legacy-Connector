package org.kylepitcock.legacyconnector.compat.alienevo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import net.minecraft.world.entity.EntityDimensions;
import org.junit.jupiter.api.Test;

class AlienEvoDimensions1201Test {
    private static final float EPSILON = 0.00001F;

    @Test
    void grayMatterUsesSmallCollisionAndIndependentEyeHeightMultipliers() {
        EntityDimensions original = EntityDimensions.fixed(0.6F, 1.8F).withEyeHeight(1.62F);

        // Gray Matter: overall 0.25, hitbox height 1.1, eye height 1.125.
        EntityDimensions scaled = AlienEvoDimensions1201.applyScales(
                original, 0.25F, 0.275F, 0.28125F);

        assertEquals(0.15F, scaled.width(), EPSILON);
        assertEquals(0.495F, scaled.height(), EPSILON);
        assertEquals(0.455625F, scaled.eyeHeight(), EPSILON);
        assertEquals(true, scaled.fixed(), "The adapter preserves the original fixed-dimensions flag");
    }

    @Test
    void diamondheadUsesLargeCollisionAndIndependentEyeHeightMultipliers() {
        EntityDimensions original = EntityDimensions.fixed(0.6F, 1.8F).withEyeHeight(1.62F);

        // Diamondhead: overall 1.35, hitbox height 1.15, eye height 1.23.
        EntityDimensions scaled = AlienEvoDimensions1201.applyScales(
                original, 1.35F, 1.5525F, 1.6605F);

        assertEquals(0.81F, scaled.width(), EPSILON);
        assertEquals(2.7945F, scaled.height(), EPSILON);
        assertEquals(2.69001F, scaled.eyeHeight(), EPSILON);
        assertEquals(true, scaled.fixed(), "The adapter preserves the original fixed-dimensions flag");
    }

    @Test
    void unitScalesKeepTheOriginalDimensionsInstance() {
        EntityDimensions original = EntityDimensions.scalable(0.6F, 1.8F);

        assertSame(original, AlienEvoDimensions1201.applyScales(original, 1.0F, 1.0F, 1.0F));
    }

    @Test
    void invalidScaleValuesFailClosedWithoutChangingDimensions() {
        EntityDimensions original = EntityDimensions.scalable(0.6F, 1.8F);

        assertSame(original, AlienEvoDimensions1201.applyScales(original, 0.0F, 1.0F, 1.0F));
        assertSame(original, AlienEvoDimensions1201.applyScales(original, 1.0F, Float.NaN, 1.0F));
        assertSame(original, AlienEvoDimensions1201.applyScales(original, 1.0F, 1.0F, Float.POSITIVE_INFINITY));
    }
}
