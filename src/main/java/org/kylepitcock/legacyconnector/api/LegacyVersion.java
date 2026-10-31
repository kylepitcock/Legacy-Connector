package org.kylepitcock.legacyconnector.api;

import java.util.Optional;

/** A Minecraft version whose mods Legacy Connector runs. */
public enum LegacyVersion {
    V1_7_10("1.7.10"),
    V1_12_2("1.12.2");

    private final String id;

    LegacyVersion(String id) {
        this.id = id;
    }

    /** The Minecraft version as the mods name it, {@code "1.7.10"}. */
    public String id() {
        return id;
    }

    public static Optional<LegacyVersion> byId(String id) {
        for (LegacyVersion v : values()) if (v.id.equals(id)) return Optional.of(v);
        return Optional.empty();
    }
}
