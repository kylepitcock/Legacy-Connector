package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.google.gson.JsonObject;
import java.io.File;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.PlayerDataStorage;

/** Files, saved data, profile cache and JSON helpers of 1.20.1 whose signatures changed in 1.21.1 ({@code File} became {@code Path}, results became {@code Optional}...). */
public final class StorageCompat {
    private StorageCompat() {}

    /** {@code Util.safeReplaceFile(File, File, File)}: the {@code Path} overload does the same. */
    public static void safeReplaceFile(File current, File latest, File oldBackup) {
        Util.safeReplaceFile(current.toPath(), latest.toPath(), oldBackup.toPath());
    }

    /** {@code PlayerDataStorage.load(player)}: null when the player has no saved data (1.21.1 answers an {@code Optional}). */
    public static CompoundTag loadPlayer(PlayerDataStorage storage, Player player) {
        return storage.load(player).orElse(null);
    }

    /** {@code GameProfileCache.getAsync(name, callback)}: the callback receives the profile when the lookup completes (1.21.1 answers a future). */
    public static void getAsync(GameProfileCache cache, String name, Consumer<Optional<com.mojang.authlib.GameProfile>> callback) {
        cache.getAsync(name).thenAccept(callback);
    }

    /** {@code MapItem.makeKey(mapId)}: the data key of a map ({@code "map_" + id}, what {@code MapId.key()} answers). */
    public static String mapKey(int id) {
        return "map_" + id;
    }

    /** {@code GsonHelper.getAsItem(json, name, default)}: the item named by the member, or the default when there is none. */
    public static Item getAsItem(JsonObject json, String name, Item fallback) {
        return json.has(name) ? GsonHelper.convertToItem(json.get(name), name).value() : fallback;
    }

    /** {@code new MapDecoration(type, x, y, rot, name)}: the name became an {@code Optional}; the type holder is converted by the row. */
    public static Optional<Component> optionalName(Component name) {
        return Optional.ofNullable(name);
    }
}
