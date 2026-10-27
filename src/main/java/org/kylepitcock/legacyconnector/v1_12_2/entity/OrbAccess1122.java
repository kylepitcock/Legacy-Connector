package org.kylepitcock.legacyconnector.v1_12_2.entity;

import java.lang.reflect.Field;
import net.minecraft.world.entity.ExperienceOrb;
import net.neoforged.fml.util.ObfuscationReflectionHelper;

/** The private value of modern experience orbs (an orb a mod constructs with an amount). */
public final class OrbAccess1122 {
    private OrbAccess1122() {}

    private static final Field VALUE = find();

    private static Field find() {
        try {
            return ObfuscationReflectionHelper.findField(ExperienceOrb.class, "value");
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static void setValue(ExperienceOrb orb, int value) {
        try {
            if (VALUE != null) VALUE.setInt(orb, value);
        } catch (IllegalAccessException e) {
            // keeps the modern value
        }
    }
}
