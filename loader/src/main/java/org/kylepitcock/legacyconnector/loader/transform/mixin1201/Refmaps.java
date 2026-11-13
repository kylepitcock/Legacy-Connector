package org.kylepitcock.legacyconnector.loader.transform.mixin1201;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * The refmaps of a 1.20.1 jar, read for one thing: the old descriptor of a mixin target the annotation names without one. The refmap value of an
 * annotation string is the SRG form {@code m_41651_(Lnet/minecraft/...;)V} (or with an owner); its descriptor is the 1.20.1 signature of the target, which
 * lets a handler that captured only a prefix of the parameters be matched to the 1.21.1 overload exactly. The refmaps themselves are removed from the
 * configs (the annotations carry Mojang names), so the repaired annotations and the (absent) refmap always agree.
 */
public final class Refmaps {
    private Refmaps() {}

    /** Mixin class (internal name) to annotation string to old method descriptor. */
    public static Map<String, Map<String, String>> parse(Map<String, byte[]> resources) {
        Map<String, Map<String, String>> out = new HashMap<>();
        for (var e : resources.entrySet()) {
            if (!e.getKey().endsWith(".refmap.json")) continue;
            try {
                JsonObject root = JsonParser.parseString(new String(e.getValue(), StandardCharsets.UTF_8)).getAsJsonObject();
                JsonObject mappings = root.has("mappings") && root.get("mappings").isJsonObject() ? root.getAsJsonObject("mappings") : null;
                if (mappings == null) continue;
                for (var cls : mappings.entrySet()) {
                    if (!cls.getValue().isJsonObject()) continue;
                    Map<String, String> m = out.computeIfAbsent(cls.getKey().replace('.', '/'), k -> new HashMap<>());
                    for (var sel : cls.getValue().getAsJsonObject().entrySet()) {
                        JsonElement v = sel.getValue();
                        if (!v.isJsonPrimitive()) continue;
                        String text = v.getAsString();
                        int paren = text.indexOf('(');
                        if (paren < 0 || text.indexOf(':') >= 0 && text.indexOf(':') < paren) continue;
                        m.putIfAbsent(sel.getKey(), text.substring(paren));
                    }
                }
            } catch (RuntimeException ignored) {
                // not a refmap we understand: no hints from it
            }
        }
        return out;
    }
}
