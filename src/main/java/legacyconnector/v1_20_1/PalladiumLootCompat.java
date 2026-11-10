package legacyconnector.v1_20_1;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.level.storage.loot.LootPool;
import net.neoforged.neoforge.common.CommonHooks;
import com.mojang.serialization.JsonOps;

/**
 * Runtime codec bridge for Palladium 4.5.x's extra loot-pool JSON files.
 *
 * Palladium's Gson is still used by SimpleJsonResourceReloadListener to parse
 * resource files as JsonElements. The one special Gson.fromJson(..., LootPool.class)
 * operation is replaced by this bridge, which decodes a singleton pool through
 * NeoForge 1.21.1's conditional loot-pool codec.
 */
public final class PalladiumLootCompat {
    private PalladiumLootCompat() {
    }

    /**
     * Decode one Palladium pool. The target type is retained in the bridge
     * descriptor so the loader can replace Gson.fromJson without changing the
     * original operand stack. Palladium separately requires every added pool
     * to carry an explicit "name" member before this method is called.
     *
     * @return the decoded pool, or null when its NeoForge conditions exclude it
     */
    public static LootPool decodeLootPool(Gson gson, JsonElement json, Class<?> targetType) {
        Objects.requireNonNull(gson, "Palladium loot Gson");
        Objects.requireNonNull(json, "Palladium loot-pool JSON");
        if (targetType != LootPool.class) {
            throw new JsonParseException("Palladium loot codec bridge expected " + LootPool.class.getName()
                    + " but received " + (targetType == null ? "null" : targetType.getName()));
        }
        if (!json.isJsonObject() || !json.getAsJsonObject().has("name")) {
            throw new JsonParseException("Palladium loot-pool JSON must contain an explicit name");
        }

        JsonArray singleton = new JsonArray();
        singleton.add(json);
        StringBuilder parseError = new StringBuilder();
        List<LootPool> pools = CommonHooks.lootPoolsCodec((pool, generatedName) -> {
            // Palladium's caller rejects nameless pools before reaching this bridge.
            // The no-op preserves only the explicit name carried in the input JSON.
        }).parse(JsonOps.INSTANCE, singleton)
                .resultOrPartial(parseError::append)
                .orElseThrow(() -> new JsonParseException(
                        "Could not decode Palladium loot pool: " + parseError));

        if (pools.isEmpty()) {
            return null; // a neoforge:conditions entry excluded this pool
        }
        if (pools.size() != 1) {
            throw new JsonParseException("Expected one Palladium loot pool, codec returned " + pools.size());
        }

        LootPool pool = pools.get(0);
        if (pool.getName() == null || pool.getName().isBlank()) {
            throw new JsonParseException("Decoded Palladium loot pool did not retain its explicit name");
        }
        return pool;
    }
}
