package org.kylepitcock.legacyconnector.v1_6_4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import legacyconnector.v1_6_4.net.minecraft.src.EntityDragonBase;
import legacyconnector.v1_7_10.net.minecraft.entity.IEntityMultiPart;
import legacyconnector.v1_7_10.net.minecraft.entity.boss.EntityDragonPart;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import org.junit.jupiter.api.Test;

/** 1.2.5's {@code EntityDragonBase} (Feather {@code BossEntity}): the rules read from the real class, with a synthetic subclass and no game. */
class EntityDragonBase125Test {
    public static class Boss125 extends EntityDragonBase {
        int partHits;
        int lastAmount;

        Boss125() {
            super((World) null);
        }

        @Override
        public boolean takeDamage(EntityDragonPart part, DamageSource source, int amount) {
            partHits++;
            lastAmount = amount;
            return true;
        }
    }

    public static class Plain125 extends EntityDragonBase {
        Plain125() {
            super((World) null);
        }
    }

    @Test
    void defaultMaxHealthIsOneHundredAndTheBaseIsNotHurtDirectly() {
        Plain125 b = new Plain125();
        assertEquals(100.0F, b.mcc$legacyMaxHealth());
        assertFalse(b.attackEntityFrom(null, 5.0F), "a(DamageSource,int) of the real class returns false");
        assertFalse(b.attackEntityFromPart(null, null, 3.0F), "the part hit goes to attackEntityFrom, which refuses");
    }

    @Test
    void aPartHitReachesTheSubclassOverrideWithAnIntAmount() {
        Boss125 b = new Boss125();
        assertTrue(b.attackEntityFromPart(null, null, 7.9F));
        assertEquals(1, b.partHits);
        assertEquals(7, b.lastAmount);
        assertTrue(IEntityMultiPart.class.isAssignableFrom(EntityDragonBase.class), "EntityDragonPart's owner type");
    }
}
