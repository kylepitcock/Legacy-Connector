package org.kylepitcock.legacyconnector.loader.transform;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.mapping.ForwardRenames;

/**
 * The hook lists of Minecraft 1.6.4, 1.5.2 and 1.4.7 are replayed by the same run time places as 1.7.10's ({@code CoremodSites}), which key a place by the
 * 1.7.10 name of the Minecraft method. The pre-1.7 layers run on the 1.7.10 shim, so their lists are written in 1.7.10's class names: every Minecraft class a
 * site key or a hook descriptor names goes through the version's forward renames ({@link ForwardRenames}: 1.4.7's flat {@code net/minecraft/src/<Name>}
 * to the packaged names, 1.5.2's and 1.6.4's class renames). Method names stay as the scan wrote them (SRG ids): those of methods that exist in both versions are
 * the same, which the first replayed hook confirms ({@code EntityPlayer.func_71065_l}, 1.6.4 and 1.7.10); a method that was renumbered is simply not found
 * by a place and shows up in the report as without a place.
 */
public final class PreSeventeenKeys {
    private static final Pattern MINECRAFT_CLASS = Pattern.compile("net/minecraft/[A-Za-z0-9_/$]+");

    private PreSeventeenKeys() {}

    /** Whether the list of this Minecraft version is keyed through the forward renames (1.4.7, 1.5.2, 1.6.4). */
    public static boolean applies(String mc) {
        LegacyVersion v = version(mc);
        return v != null && v.forward() != null;
    }

    /** {@code text} (a site key or a descriptor) with every Minecraft class named by its 1.7.10 name; unchanged for other versions. */
    public static String toSeventeen(String mc, String text) {
        LegacyVersion v = version(mc);
        if (v == null || v.forward() == null || text == null) return text;
        Matcher m = MINECRAFT_CLASS.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) m.appendReplacement(out, Matcher.quoteReplacement(ForwardRenames.map(v, m.group())));
        m.appendTail(out);
        return out.toString();
    }

    private static LegacyVersion version(String mc) {
        for (LegacyVersion v : LegacyVersion.values()) if (v.id().equals(mc)) return v;
        return null;
    }
}
