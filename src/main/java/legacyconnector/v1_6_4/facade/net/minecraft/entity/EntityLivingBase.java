package legacyconnector.v1_6_4.facade.net.minecraft.entity;

import legacyconnector.v1_7_10.net.minecraft.entity.SharedMonsterAttributes;
import legacyconnector.v1_7_10.net.minecraft.entity.ai.attributes.IAttributeInstance;

/**
 * Facade of the 1.7.10 shim's {@code EntityLivingBase} for the int-health era (1.5.2 and 1.4.7 mods): {@code entity.health}, {@code entity.moveSpeed} and
 * {@code getMaxHealth()} as ints and floats of the entity itself, over the modern health and the movement speed attribute. See {@code Hierarchy.facadeOf}.
 * Never instantiated.
 */
public final class EntityLivingBase {
    private EntityLivingBase() {}

    /** {@code entity.health}: the whole hit points left (rounded up, so a sliver of health still counts as alive). */
    public static int mcc$get$health(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase self) {
        return healthOf(self.getHealth());
    }

    /** {@code entity.health = n}: the hit points, between 0 (dead) and the maximum. */
    public static void mcc$set$health(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase self, int health) {
        self.setHealth(health);
    }

    /** {@code getMaxHealth()} as 1.5.2 had it. */
    public static int getMaxHealth(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase self) {
        return healthOf(self.getMaxHealth());
    }

    /** {@code entity.moveSpeed}: how fast the entity walks; 1.6.4 turned the field into the movement speed attribute (default 0.7 in both). */
    public static float mcc$get$moveSpeed(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase self) {
        IAttributeInstance speed = self.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
        return speed == null ? 0.7F : (float) speed.getBaseValue();
    }

    public static void mcc$set$moveSpeed(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase self, float value) {
        IAttributeInstance speed = self.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
        if (speed != null) speed.setBaseValue(value);
    }

    /** Whole hit points of a float health: 1.5.2 kept ints, so any health above 0 is at least 1. */
    public static int healthOf(float health) {
        return health <= 0 ? 0 : (int) Math.ceil(health);
    }
}
