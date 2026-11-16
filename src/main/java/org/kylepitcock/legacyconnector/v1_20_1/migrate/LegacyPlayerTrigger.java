package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.resources.ResourceLocation;

/**
 * Forge 1.20.1's {@code new PlayerTrigger(id)}: a trigger that carries its own id (1.21.1's takes its id from the registry). {@link MiscCompat#registerTrigger}
 * reads {@code getId()} to register it under that name.
 */
public final class LegacyPlayerTrigger extends PlayerTrigger {
    private final ResourceLocation id;

    public LegacyPlayerTrigger(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation getId() {
        return id;
    }

    /** The constructor call of the mod. */
    public static PlayerTrigger create(ResourceLocation id) {
        return new LegacyPlayerTrigger(id);
    }
}
