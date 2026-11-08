package org.kylepitcock.legacyconnector.loader;

/**
 * Human-readable notes for compatibility-report entries.
 *
 * <p>Mixin handling differs by legacy version: Forge 1.20.1 configs are migrated and registered
 * with NeoForge, while older versions have only the narrower translated-mod-class path.</p>
 */
final class LegacyModReportNotes {
    private LegacyModReportNotes() {}

    static String mixinStatus(LegacyVersion version, String mixinConfigs) {
        if (version == LegacyVersion.V1_20_1) {
            return "NOTE: ships mixins (" + mixinConfigs + "); their configuration and target metadata "
                    + "are rewritten for Minecraft 1.21.1 and registered with NeoForge. Individual injections "
                    + "may still fail if target methods, descriptors, or injection points changed (see notes "
                    + "and latest.log).";
        }
        if (version == LegacyVersion.V1_12_2) {
            return "WARNING: ships mixins (" + mixinConfigs + ") targeting legacy Minecraft; only mixins "
                    + "targeting translated mod classes are applied (see notes).";
        }
        return "WARNING: ships mixins (" + mixinConfigs + ") targeting legacy Minecraft; not applied.";
    }
}
