package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.resources.ResourceLocation;

/** Loot table registration of 1.20.1. */
public final class LootTableCompat {
    private LootTableCompat() {}

    /**
     * {@code BuiltInLootTables.register(id)}: it remembered the id as a built-in table and returned it. Tables are plain data in 1.21.1 (a data pack
     * path, a {@code ResourceKey} to look one up), so the id is returned as it is.
     */
    public static ResourceLocation lootTableId(ResourceLocation id) {
        return id;
    }
}
