package org.kylepitcock.legacyconnector.compat.jdk;

/**
 * Package roots of the classes that make up the 1.7.10 API at run time: the hand-written shim ({@code legacyconnector.v1_7_10}) and the classes the
 * loader synthesized for what the shim lacks, which for the forward versions (1.6.4 and 1.5.2 jars link against the 1.7.10 shim, see
 * {@code LegacyVersion#forward()}) live under their own synth roots. Runtime lookups that find a legacy game class by name (vanilla block, item
 * and entity classes, SRG names, dispenser behaviours) must try all of them, not only {@code legacyconnector_synth.v1_7_10}.
 */
public final class LegacyPrefixes {
    public static final String[] API_1710 = {"legacyconnector.v1_7_10.", "legacyconnector_synth.v1_7_10.", "legacyconnector_synth.v1_6_4.",
            "legacyconnector_synth.v1_5_2."};

    private LegacyPrefixes() {}

    /** {@code suffix} (a dotted name relative to a package root, e.g. {@code net.minecraft.entity.}) under every 1.7.10-family root. */
    public static String[] api1710(String suffix) {
        String[] out = new String[API_1710.length];
        for (int i = 0; i < out.length; i++) out[i] = API_1710[i] + suffix;
        return out;
    }

    /** True when the class name belongs to the 1.7.10 API (shim or any synth root of the 1.7.10 family). */
    public static boolean isApi1710(String className) {
        for (String p : API_1710) if (className.startsWith(p)) return true;
        return false;
    }
}
