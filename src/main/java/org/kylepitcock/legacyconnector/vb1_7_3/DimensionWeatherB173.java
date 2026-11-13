package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * A Beta dimension with no sky or no ground below its islands (the Aether) has no weather. A derived level shares the overworld's
 * weather data, so without this it rains there whenever the overworld does; the level's rain and thunder are held at zero.
 */
public final class DimensionWeatherB173 {
    private DimensionWeatherB173() {}
    private static boolean installed;
    private static final Map<ServerLevel, Boolean> RAINLESS = Collections.synchronizedMap(new WeakHashMap<>());

    public static synchronized void install() {
        if (installed) return;
        installed = true;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(DimensionWeatherB173::onTick);
    }

    private static boolean rainless(ServerLevel level) {
        return RAINLESS.computeIfAbsent(level, l -> {
            try {
                if (DimensionsB173.registeredId(l) == null) return false;
                var dimension = DimensionsB173.providerFor(legacyconnector.vb1_7_3.net.minecraft.world.World.mcc$wrap(l));
                return dimension != null && (dimension.hasNoSky || !dimension.hasGround());
            } catch (RuntimeException | LinkageError error) {
                return false;
            }
        });
    }

    private static void onTick(LevelTickEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !rainless(level)) return;
        if (level.getRainLevel(1F) > 0F) level.setRainLevel(0F);
        if (level.getThunderLevel(1F) > 0F) level.setThunderLevel(0F);
    }
}
