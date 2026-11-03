package org.kylepitcock.legacyconnector.v1_7_10.entity;

import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;

/**
 * Entity size changes of legacy code ({@code Entity.setSize}, player eye height). They are carried out by Pehkui, an optional
 * dependency (not bundled): without it entities keep their type's own size. Only {@link EntitySizesPehkui1710} touches Pehkui
 * classes, so nothing here fails to link when it is absent.
 */
public final class EntitySizes1710 {
    private static Boolean pehkui;

    private EntitySizes1710() {}

    private static boolean available() {
        Boolean p = pehkui;
        if (p == null) {
            ModList list = ModList.get();
            if (list == null) return false;
            pehkui = p = list.isLoaded("pehkui");
        }
        return p;
    }

    public static void setSize(Entity entity, float width, float height) {
        if (available()) EntitySizesPehkui1710.setSize(entity, width, height);
    }

    public static void setEyeHeight(Entity entity, float eye) {
        if (available()) EntitySizesPehkui1710.setEyeHeight(entity, eye);
    }
}
