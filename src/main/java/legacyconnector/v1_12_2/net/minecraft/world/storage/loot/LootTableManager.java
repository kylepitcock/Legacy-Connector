package legacyconnector.v1_12_2.net.minecraft.world.storage.loot;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 loot table manager: the server's (modern, data pack) loot tables by name, as legacy table views. */
public class LootTableManager {
    public static final com.google.gson.Gson GSON_INSTANCE = new com.google.gson.GsonBuilder()
            .registerTypeAdapter(RandomValueRange.class, new RandomValueRange.Serializer())
            .registerTypeAdapter(LootPool.class, new LootPool.Serializer())
            .registerTypeAdapter(LootTable.class, new LootTable.Serializer())
            .registerTypeHierarchyAdapter(LootEntry.class, new LootEntry.Serializer())
            .registerTypeAdapter(LootContext.EntityTarget.class, new LootContext.EntityTarget.Serializer())
            .create();

    private static volatile LootTableManager instance;
    private final java.io.File baseFolder;

    public LootTableManager(java.io.File folder) {
        this.baseFolder = folder;
    }

    /** The manager of the running server (one for all worlds, as in 1.12), or null without a server. */
    public static LootTableManager mcc$of(net.minecraft.server.MinecraftServer server) {
        if (server == null) return null;
        LootTableManager m = instance;
        if (m == null) instance = m = new LootTableManager(null);
        return m;
    }

    public LootTable getLootTableFromLocation(ResourceLocation location) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null || location == null) return LootTable.EMPTY_LOOT_TABLE;
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(location.getNamespace(), location.getPath()));
        return LootTable.mcc$view(server.reloadableRegistries().getLootTable(key));
    }

    /** Loot tables reload with the server's data packs ({@code /reload}). */
    public void reloadLootTables() {
    }
}
