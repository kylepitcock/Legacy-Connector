package org.kylepitcock.legacyconnector.api;

import net.minecraft.world.level.Level;
import org.kylepitcock.legacyconnector.api.internal.Convert;

/** The legacy {@code World} view of a modern level. */
public final class LegacyWorlds {
    private LegacyWorlds() {}

    /**
     * The legacy {@code World} (1.7.10) / {@code World} (1.12.2) wrapping {@code level}: the same object for the same level, so legacy
     * code that keys by world identity keeps working. An opaque object; call it through {@link LegacyAccess}.
     */
    public static Object toLegacy(Level level, LegacyVersion version) {
        return Convert.worldToLegacy(level, version);
    }
}
