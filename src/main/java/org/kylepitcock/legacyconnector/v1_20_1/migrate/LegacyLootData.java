package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Stands in for {@code net.minecraft.world.level.storage.loot.LootDataManager} (1.20.1, removed): the loot tables of the server come from its reloadable
 * registries now. Only the table lookups mods used are offered.
 */
public final class LegacyLootData {
    private final MinecraftServer server;

    LegacyLootData(MinecraftServer server) {
        this.server = server;
    }

    public LootTable getLootTable(ResourceLocation id) {
        return server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id));
    }
}
