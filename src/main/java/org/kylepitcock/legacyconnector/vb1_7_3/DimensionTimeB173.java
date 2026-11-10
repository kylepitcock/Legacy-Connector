package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelTimeAccess;

/** Scoped provider time hook shared by native client and server levels. */
public final class DimensionTimeB173 {
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    private DimensionTimeB173() {}

    public static float timeOfDay(LevelTimeAccess access, float partialTick, float nativeTime) {
        if (ACTIVE.get() || !(access instanceof Level level) || DimensionsB173.registeredId(level) == null) return nativeTime;
        ACTIVE.set(true);
        try {
            var provider = DimensionsB173.providerFor(World.mcc$wrap(level));
            if (provider == null) return nativeTime;
            float time = provider.getTimeOfDay(level.getDayTime(), partialTick);
            return Float.isFinite(time) ? time : nativeTime;
        } catch (RuntimeException | LinkageError error) {
            return nativeTime;
        } finally {
            ACTIVE.remove();
        }
    }
}
