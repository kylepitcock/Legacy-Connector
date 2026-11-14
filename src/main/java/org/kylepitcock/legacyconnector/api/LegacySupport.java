package org.kylepitcock.legacyconnector.api;

import java.util.EnumMap;
import java.util.Map;

/**
 * What the API does for each legacy version, as data (added in API 0.3). The table in {@code docs/wiki/Addon-API.md} is generated from
 * it (a unit test fails when the two differ), so an addon and the documentation read the same answer:
 *
 * <pre>
 * if (LegacySupport.of(Feature.ENERGY_RF, version).level() == LegacySupport.Level.NOT_APPLICABLE) return; // e.g. Beta 1.7.3
 * </pre>
 *
 * An API call for a feature a version does not have never throws: it answers {@code Optional.empty()}, an empty list or an empty
 * stack (see each method), which is the {@link Level#NOT_APPLICABLE} and {@link Level#NO_OP} case.
 */
public final class LegacySupport {
    private LegacySupport() {}

    /** How well a feature works for a version. */
    public enum Level {
        /** Implemented and used by mods of that version. */
        WORKS,
        /** Works with the restriction named in the note. */
        LIMITED,
        /** The concept exists in that version but Legacy Connector provides nothing: the call answers "empty"; an addon can add it (rules, {@link LegacyAccess}). */
        NO_OP,
        /** The version has no such concept (no energy API in Beta): the call answers "empty". */
        NOT_APPLICABLE
    }

    /** One answer of the matrix. */
    public record Support(Level level, String note) {
        /** True unless the feature is not applicable or a no-op for the version. */
        public boolean available() {
            return level == Level.WORKS || level == Level.LIMITED;
        }
    }

    /** The API features the matrix covers, in the order of the documentation table. */
    public enum Feature {
        MOD_QUERY("`LegacyApi.mods()` / `mod(id)` / `isLoaded` / `mods(version)`"),
        LIFECYCLE("Phases, addon callbacks, mod bus events, `runWhen`"),
        FAILURE_EVENTS("`onLegacyModFailed` / `LegacyModFailedEvent` / `failedPhase()` / `missingDependencies()`"),
        VERSION_DETECTION("`LegacyApi.versionOf(object)` / `versionOfNamespace`"),
        LEGACY_ACCESS("`LegacyAccess` on mod objects"),
        ITEM_CONVERSION("`LegacyItems.toLegacy` / `fromLegacy`"),
        CONTENT_KEYS("`LegacyContentHooks.itemKey` / `legacyStack` / `legacyBlockStack` / `modernItemId`"),
        BLOCK_CONVERSION("`LegacyBlocks.legacyBlock` / `legacyMeta` / `fromLegacy(block, meta)` / `versionOf`"),
        BLOCK_STATE_CONVERSION("`LegacyBlocks.legacyState` / `fromLegacyState`"),
        TILE_CONVERSION("`LegacyBlocks.legacyTile` / `tileVersion`"),
        WORLD_CONVERSION("`LegacyWorlds.toLegacy`"),
        NUMERIC_IDS("`numericBlockId` / `numericItemId` / `legacyBlockById` / `legacyItemById`"),
        CAPABILITY_RULES("`LegacyCapabilities.forTile` / `forAnyTile` (addon rules)"),
        ITEM_HANDLER_TILES("Built-in item handler capability of tiles"),
        FLUID_HANDLER_TILES("Built-in fluid handler capability of tiles"),
        ENERGY_RF("Built-in energy capability: RF / Forge Energy"),
        ENERGY_EU("Built-in energy capability: IC2 EU"),
        ENERGY_MJ("Built-in energy capability: BuildCraft MJ"),
        ITEM_CAPABILITIES("Built-in item/fluid/energy capabilities of items"),
        FLUID_LOOKUP("`LegacyFluids.modernFluid` / `legacyFluid` / `fromLegacy` / `toLegacy`"),
        BUCKET_LOOKUP("`LegacyFluids.bucketOf`"),
        CREATIVE_TAB_LOOKUP("`LegacyTabs.modernTab` / `modernTabs`");

        private final String label;

        Feature(String label) {
            this.label = label;
        }

        /** The documentation label (Markdown). */
        public String label() {
            return label;
        }
    }

    private static final LegacyVersion[] ORDER = {LegacyVersion.V1_4_7, LegacyVersion.V1_5_2, LegacyVersion.V1_6_4, LegacyVersion.V1_7_10,
            LegacyVersion.V1_12_2, LegacyVersion.VB1_7_3, LegacyVersion.V1_20_1};

    private static final Map<Feature, Support[]> MATRIX = new EnumMap<>(Feature.class);

    private static final Support NA = new Support(Level.NOT_APPLICABLE, "");

    private static Support w() {
        return new Support(Level.WORKS, "");
    }

    private static Support w(String note) {
        return new Support(Level.WORKS, note);
    }

    private static Support l(String note) {
        return new Support(Level.LIMITED, note);
    }

    private static Support noop(String note) {
        return new Support(Level.NO_OP, note);
    }

    private static Support na(String note) {
        return new Support(Level.NOT_APPLICABLE, note);
    }

    /** One row; the arguments follow the version order 1.4.7, 1.5.2, 1.6.4, 1.7.10, 1.12.2, b1.7.3, 1.20.1. */
    private static void row(Feature f, Support v147, Support v152, Support v164, Support v1710, Support v1122, Support beta, Support v1201) {
        MATRIX.put(f, new Support[] {v147, v152, v164, v1710, v1122, beta, v1201});
    }

    private static void all(Feature f, Support s) {
        row(f, s, s, s, s, s, s, s);
    }

    static {
        row(Feature.MOD_QUERY, w(), w(), w(), w(), w(), w(), l("mods are plain NeoForge mods: `failed()` is false, `instance()` is empty"));
        row(Feature.LIFECYCLE, w(), w(), w(), w(), w(), w(), l("DISCOVERED and REGISTERED run back to back after the last registry event"));
        row(Feature.FAILURE_EVENTS, w(), w(), w(), w(), w(), w(), na("FML loads these mods; its own error screen reports them"));
        all(Feature.VERSION_DETECTION, w());
        all(Feature.LEGACY_ACCESS, w());
        row(Feature.ITEM_CONVERSION, w("live view, null for empty"), w("live view, null for empty"), w("live view, null for empty"), w("live view, null for empty"),
                w("copy, EMPTY for empty"), w("null for empty; a modern item Beta lacks rides one reserved item"), w("the modern stack itself"));
        row(Feature.CONTENT_KEYS, w("stand-in for modern content"), w("stand-in for modern content"), w("stand-in for modern content"),
                w("stand-in for modern content"), w("stand-in item for modern content"), w("name = modern id"), w("key = registry id"));
        row(Feature.BLOCK_CONVERSION, w(), w(), w(), w(), w(), w(), l("the block is the modern block; no metadata"));
        row(Feature.BLOCK_STATE_CONVERSION, NA, NA, NA, NA, w("`IBlockState`"), NA, w("the state itself"));
        row(Feature.TILE_CONVERSION, w(), w(), w(), w(), w(), w(), w("the block entity itself"));
        row(Feature.WORLD_CONVERSION, w(), w(), w(), w(), w(), w(), w("the level itself"));
        row(Feature.NUMERIC_IDS, w(), w(), w(), NA, NA, w("ids 0-255 / items 256+"), NA);
        row(Feature.CAPABILITY_RULES, w(), w(), w(), w(), w(), w(), l("only the block entity types of the 1.20.1 mods"));
        row(Feature.ITEM_HANDLER_TILES, w(), w(), w(), w(), w(), w("`IInventory`, no sides"), w("native (Forge capability bridged)"));
        row(Feature.FLUID_HANDLER_TILES, w("ITankContainer"), w("ITankContainer"), w("ITankContainer"), w("IFluidHandler / IFluidTank"), w("Forge fluid handler"),
                na("Beta has no fluids API"), w("native (Forge capability bridged)"));
        row(Feature.ENERGY_RF, l("only mods that use the RF API"), l("only mods that use the RF API"), l("only mods that use the RF API"), w("1 RF = 1 FE"),
                w("Forge Energy and Redstone Flux"), na("Beta has no energy API"), w("native (Forge capability bridged)"));
        row(Feature.ENERGY_EU, noop("not built in"), noop("not built in"), noop("not built in"), w("`energy.fePerEu`"), noop("not built in"), na("Beta has no energy API"),
                noop("the mods' own capabilities"));
        row(Feature.ENERGY_MJ, noop("not built in"), noop("not built in"), noop("not built in"), w("receive only; `energy.fePerMj`"), noop("not built in"),
                na("Beta has no energy API"), noop("the mods' own capabilities"));
        row(Feature.ITEM_CAPABILITIES, l("RF items, fluid containers"), l("RF items, fluid containers"), l("RF items, fluid containers"),
                w("RF, IC2 EU, fluid containers"), w("`initCapabilities`"), na("no capability system"), w("native (Forge capability bridged)"));
        row(Feature.FLUID_LOOKUP, w(), w(), w(), w(), w(), na("Beta has no fluid registry"), w("the fluid is modern already"));
        row(Feature.BUCKET_LOOKUP, l("buckets registered with the container registry"), l("buckets registered with the container registry"),
                l("buckets registered with the container registry"), l("buckets registered with the container registry"), w("also generated buckets"),
                na("Beta has no fluid registry"), w("the fluid's bucket item"));
        row(Feature.CREATIVE_TAB_LOOKUP, w(), w(), w(), w(), w(), l("one tab per mod, no tab objects"), w("the tab itself"));
    }

    /** The answer for one feature and version. */
    public static Support of(Feature feature, LegacyVersion version) {
        for (int i = 0; i < ORDER.length; i++) if (ORDER[i] == version) return MATRIX.get(feature)[i];
        return NA;
    }

    /** The versions in the order of the documentation table. */
    public static LegacyVersion[] versions() {
        return ORDER.clone();
    }

    /** The Markdown table of the matrix as it appears in the documentation (between its generated markers). */
    public static String markdownTable() {
        StringBuilder sb = new StringBuilder("| API feature |");
        for (LegacyVersion v : ORDER) sb.append(' ').append(v.id()).append(" |");
        sb.append("\n| --- |");
        for (int i = 0; i < ORDER.length; i++) sb.append(" --- |");
        sb.append('\n');
        for (Feature f : Feature.values()) {
            sb.append("| ").append(f.label()).append(" |");
            for (LegacyVersion v : ORDER) {
                Support s = of(f, v);
                String cell = switch (s.level()) {
                    case WORKS -> "yes";
                    case LIMITED -> "limited";
                    case NO_OP -> "no-op";
                    case NOT_APPLICABLE -> "n.a.";
                };
                if (!s.note().isEmpty()) cell += " (" + s.note() + ")";
                sb.append(' ').append(cell).append(" |");
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
