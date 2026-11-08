package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.Set;
import legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup;
import net.minecraft.world.entity.MobCategory;

/** Explicit Beta category-cap writes apply to native global and per-player natural-spawn caps. */
public final class SpawnCapsB173 {
    private static volatile Set<SpawnGroup> overrides=Set.of();
    private SpawnCapsB173() {}
    /** Called only after reflection has successfully written the resolved legacy field. */
    public static synchronized void afterWrite(Field field,Object instance) {
        if(field.getDeclaringClass()!=SpawnGroup.class||!field.getName().equals("capacity")||!(instance instanceof SpawnGroup group))return;
        var updated=EnumSet.noneOf(SpawnGroup.class);updated.addAll(overrides);updated.add(group);
        overrides=Set.copyOf(updated);
    }
    public static boolean hasOverrides(){return !overrides.isEmpty();}
    public static boolean isOverridden(SpawnGroup group){return overrides.contains(group);}
    public static int capacity(MobCategory category,int nativeCapacity) {
        for(var group:overrides)if(group.mcc$modern()==category)return group.getCapacity();
        return nativeCapacity;
    }
}
