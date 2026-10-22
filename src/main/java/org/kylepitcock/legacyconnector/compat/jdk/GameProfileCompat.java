package org.kylepitcock.legacyconnector.compat.jdk;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Arguments of legacy {@code new GameProfile(id, name)}: authlib 1.5 (Minecraft 1.7.10) accepted a null id or name,
 * which mods used for fake players. A missing id becomes the offline-mode id of the name (stable across restarts),
 * a missing name an empty one.
 */
public final class GameProfileCompat {
    private GameProfileCompat() {}

    public static UUID id(UUID id, String name) {
        if (id != null) return id;
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + (name == null ? "" : name)).getBytes(StandardCharsets.UTF_8));
    }

    public static String name(String name) {
        return name == null ? "" : name;
    }
}
