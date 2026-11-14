package org.kylepitcock.legacyconnector.api;

import java.util.Optional;

/** A Minecraft version whose mods Legacy Connector runs. */
public enum LegacyVersion {
    V1_7_10("1.7.10"),
    V1_12_2("1.12.2"),
    /** Added in API 0.2: numeric block/item ids (own id space per version), objects are the 1.7.10 shim's, like 1.7.10. */
    V1_4_7("1.4.7"),
    /** Added in API 0.2: see {@link #V1_4_7}. */
    V1_5_2("1.5.2"),
    /** Added in API 0.2: see {@link #V1_4_7}. */
    V1_6_4("1.6.4"),
    /** Added in API 0.2: Forge 1.20.1 mods remapped in place; their blocks, items, stacks and tiles ARE the modern objects. */
    V1_20_1("1.20.1"),
    /**
     * Added in API 0.3: Beta 1.7.3 mods (ModLoader / Forge 1.0). Own platform and shim (numeric block and item ids 0-255 / 256+, one id
     * space, mods keep their ids); there is no Forge API, no capabilities of their own and no energy or fluid containers.
     */
    VB1_7_3("b1.7.3");

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

    /**
     * The shim family the version's legacy objects belong to: 1.4.7, 1.5.2 and 1.6.4 mods run on the 1.7.10 shim (the same
     * {@code ItemStack}/{@code Block}/{@code TileEntity}/{@code World} classes), so this is {@link #V1_7_10} for them; every other
     * version is its own family. Added in API 0.2.
     */
    public LegacyVersion family() {
        return this == V1_4_7 || this == V1_5_2 || this == V1_6_4 ? V1_7_10 : this;
    }

    /** True for 1.4.7, 1.5.2 and 1.6.4: blocks and items have numeric ids (a separate id space per version). Added in API 0.2. */
    public boolean usesNumericIds() {
        return this == V1_4_7 || this == V1_5_2 || this == V1_6_4;
    }

    /**
     * True for 1.20.1: the mod's classes run remapped in place, so "legacy" blocks, items, stacks, block entities and levels are the
     * modern objects themselves and the conversions return them unchanged. Added in API 0.2.
     */
    public boolean isInPlace() {
        return this == V1_20_1;
    }

    /** True for Beta 1.7.3 (its own platform and shim, never the 1.7.10 one). Added in API 0.3. */
    public boolean isBeta() {
        return this == VB1_7_3;
    }
}
