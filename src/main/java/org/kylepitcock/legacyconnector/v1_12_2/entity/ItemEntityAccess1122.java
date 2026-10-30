package org.kylepitcock.legacyconnector.v1_12_2.entity;

import java.lang.reflect.Field;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.fml.util.ObfuscationReflectionHelper;

/** Private state of modern item entities that 1.12 code reads and writes as fields (age, pickup delay). */
public final class ItemEntityAccess1122 {
    private ItemEntityAccess1122() {}

    private static Field find(String name) {
        try {
            return ObfuscationReflectionHelper.findField(ItemEntity.class, name);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static final Field AGE = find("age");
    private static final Field PICKUP_DELAY = find("pickupDelay");

    public static void setAge(ItemEntity item, int age) {
        try {
            if (AGE != null) AGE.setInt(item, age);
        } catch (IllegalAccessException e) {
            // keeps the modern age
        }
    }

    public static int pickupDelay(ItemEntity item) {
        try {
            return PICKUP_DELAY == null ? (item.hasPickUpDelay() ? 10 : 0) : PICKUP_DELAY.getInt(item);
        } catch (IllegalAccessException e) {
            return item.hasPickUpDelay() ? 10 : 0;
        }
    }

    private static final java.lang.reflect.Method MERGE_WITH_NEIGHBOURS = findMerge();

    private static java.lang.reflect.Method findMerge() {
        try {
            return ObfuscationReflectionHelper.findMethod(ItemEntity.class, "mergeWithNeighbours");
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** The modern merge of an item entity with the mergeable item entities around it. */
    public static void mergeWithNeighbours(ItemEntity item) {
        try {
            if (MERGE_WITH_NEIGHBOURS != null) MERGE_WITH_NEIGHBOURS.invoke(item);
        } catch (ReflectiveOperationException e) {
            // no merge this time
        }
    }
}
