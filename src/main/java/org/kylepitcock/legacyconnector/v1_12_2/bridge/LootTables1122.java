package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.server.packs.PackType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import org.kylepitcock.legacyconnector.core.LegacyMods;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.pack.GeneratedPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loot tables of 1.12.2 mods. 1.12 read them from {@code assets/<mod>/loot_tables/**.json} of the mod's own jar; the modern game only
 * reads {@code data/<mod>/loot_table/**.json}, so without this bridge no mob, chest or fishing table of a 1.12.2 mod existed (Atum's
 * mummies, nomads and tomb chests dropped and held nothing).
 *
 * <p>Each such file gets an empty placeholder table in the bottom data pack (so world data packs and KubeJS replace or remove it as
 * for any mod's table). When the table loads, a placeholder that is still the placeholder is replaced by the legacy table, parsed with
 * 1.12's own table format (mods' own functions and conditions run unchanged, see {@link Loot1122}).</p>
 */
public final class LootTables1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Loot1122");
    private static final String PLACEHOLDER_POOL = "legacyconnector_placeholder";
    private static final byte[] PLACEHOLDER = ("{\"pools\":[{\"name\":\"" + PLACEHOLDER_POOL + "\",\"rolls\":0,\"entries\":[]}]}").getBytes(StandardCharsets.UTF_8);
    /** {@code mod:path} of a table to the 1.12 JSON of the mod's jar. */
    private static final Map<String, String> LEGACY = new ConcurrentHashMap<>();
    private static boolean registered;

    private LootTables1122() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        GeneratedPack.addRecipeContributor(LootTables1122::contribute);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, LootTables1122::onLoad);
    }

    private static void contribute(PackType type, Map<String, Supplier<byte[]>> out) {
        if (type != PackType.SERVER_DATA) return;
        LEGACY.clear();
        int[] count = {0};
        for (LegacyModRecord mod : LegacyMods.all()) {
            if (!"1.12.2".equals(mod.version)) continue;
            String ns = mod.modernId;
            legacyconnector.v1_12_2.net.minecraftforge.common.crafting.CraftingHelper.findFiles(null, "assets/" + ns + "/loot_tables", null, (root, file) -> {
                String rel = root.relativize(file).toString().replace('\\', '/');
                if (!rel.endsWith(".json")) return true;
                String name = rel.substring(0, rel.length() - ".json".length());
                try {
                    LEGACY.put(ns + ":" + name, Files.readString(file, StandardCharsets.UTF_8));
                    out.putIfAbsent("data/" + ns + "/loot_table/" + name + ".json", () -> PLACEHOLDER);
                    count[0]++;
                } catch (java.io.IOException | RuntimeException e) {
                    LOG.warn("Cannot read the 1.12.2 loot table {}:{}: {}", ns, name, e.toString());
                }
                return true;
            }, true, true);
        }
        if (count[0] > 0) LOG.info("1.12.2 loot tables of mod jars: {}", count[0]);
    }

    private static void onLoad(LootTableLoadEvent e) {
        String json = LEGACY.get(e.getName().toString());
        if (json == null || e.getTable().getPool(PLACEHOLDER_POOL) == null) return; // not a legacy table, or a data pack replaced it
        try {
            var legacy = legacyconnector.v1_12_2.net.minecraft.world.storage.loot.LootTableManager.GSON_INSTANCE.fromJson(json,
                    legacyconnector.v1_12_2.net.minecraft.world.storage.loot.LootTable.class);
            if (legacy != null) e.setTable(named(legacy.mcc$modern(), e.getName()));
        } catch (RuntimeException | LinkageError ex) {
            LOG.warn("Cannot parse the 1.12.2 loot table {}: {}", e.getName(), ex.toString());
        }
    }

    /**
     * A replacement table for the load event, carrying the id of the table it replaces. NeoForge sets the id only on the table it parsed;
     * a table handed to {@code setTable} without one makes other mods fail when they copy it ({@code setLootTableId(null)}, issue #226).
     * The shared {@code LootTable.EMPTY} is never given an id: an empty table of its own stands in for it.
     */
    public static net.minecraft.world.level.storage.loot.LootTable named(net.minecraft.world.level.storage.loot.LootTable table,
            net.minecraft.resources.ResourceLocation id) {
        if (table == null || table == net.minecraft.world.level.storage.loot.LootTable.EMPTY) {
            table = net.minecraft.world.level.storage.loot.LootTable.lootTable().build();
        }
        try {
            if (id != null && table.getLootTableId() == null) table.setLootTableId(id);
        } catch (RuntimeException ex) {
            // already named by someone else
        }
        return table;
    }
}
