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

    /** {@code getKeys(type)}: the ids of the loaded loot data of one type. */
    public java.util.Collection<ResourceLocation> getKeys(net.minecraft.world.level.storage.loot.LootDataType<?> type) {
        return server.reloadableRegistries().getKeys(type.registryKey());
    }

    /** {@code getElementOptional(type, id)}: one loaded loot table, predicate or modifier. */
    @SuppressWarnings("unchecked")
    public <T> java.util.Optional<T> getElementOptional(net.minecraft.world.level.storage.loot.LootDataType<T> type, ResourceLocation id) {
        return (java.util.Optional<T>) server.reloadableRegistries().get().registryOrThrow((ResourceKey<net.minecraft.core.Registry<T>>) type.registryKey())
                .getOptional(id);
    }

    public LootTable getLootTable(ResourceLocation id) {
        return server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id));
    }
}
