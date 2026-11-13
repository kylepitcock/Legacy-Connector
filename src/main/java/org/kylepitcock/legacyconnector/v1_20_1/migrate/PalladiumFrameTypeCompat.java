package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.advancements.AdvancementType;

/**
 * Retains the 1.20.1 FrameType#getTexture atlas-V offsets used by Palladium's
 * ability-tree widget. The 1.21.1 AdvancementType keeps the same three frame
 * constants but no longer exposes the old integer texture accessor.
 */
public final class PalladiumFrameTypeCompat {
    private PalladiumFrameTypeCompat() {}

    public static int getTexture(AdvancementType type) {
        return switch (type) {
            case TASK -> 0;
            case CHALLENGE -> 26;
            case GOAL -> 52;
        };
    }
}
