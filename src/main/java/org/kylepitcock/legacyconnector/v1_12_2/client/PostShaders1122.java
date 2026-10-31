package org.kylepitcock.legacyconnector.v1_12_2.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.Reader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decides whether a legacy post-processing chain can run on the modern renderer. The modern game compiles GLSL for the core profile
 * ({@code #version 150}); the programs of 1.12 mods are mostly {@code #version 120} ({@code varying}, {@code gl_FragColor}) and either fail
 * to link or, worse, run with unset uniforms and tint the whole screen. Such a chain is ignored (once-only log line) instead.
 */
public final class PostShaders1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Shaders");
    private static final Map<String, Boolean> CACHE = new ConcurrentHashMap<>();

    private PostShaders1122() {}

    /** Whether the chain (a {@code shaders/post/*.json} location) can run: false when it uses a mod program written for an old GLSL. */
    public static boolean usable(ResourceLocation chain) {
        if (chain == null) return false;
        return CACHE.computeIfAbsent(chain.toString(), k -> check(chain));
    }

    private static boolean check(ResourceLocation chain) {
        if (chain.getNamespace().equals("minecraft")) return true;
        try {
            ResourceManager rm = net.minecraft.client.Minecraft.getInstance().getResourceManager();
            var res = rm.getResource(chain);
            if (res.isEmpty()) return true;
            JsonObject root;
            try (Reader r = new BufferedReader(res.get().openAsReader())) {
                root = JsonParser.parseReader(r).getAsJsonObject();
            }
            if (!root.has("passes")) return true;
            for (JsonElement p : root.getAsJsonArray("passes")) {
                String name = p.getAsJsonObject().get("name").getAsString();
                ResourceLocation prog = ResourceLocation.parse(name);
                if (prog.getNamespace().equals("minecraft")) continue;
                String why = oldProgram(rm, prog);
                if (why != null) {
                    LOG.warn("The post shader {} is ignored: its program {} {} (the modern renderer needs GLSL 150)", chain, prog, why);
                    return false;
                }
            }
        } catch (Exception | LinkageError e) {
            LOG.debug("The post shader {} could not be inspected: {}", chain, e.toString());
        }
        return true;
    }

    private static String oldProgram(ResourceManager rm, ResourceLocation prog) throws java.io.IOException {
        var res = rm.getResource(ResourceLocation.fromNamespaceAndPath(prog.getNamespace(), "shaders/program/" + prog.getPath() + ".json"));
        if (res.isEmpty()) return null;
        JsonObject o;
        try (Reader r = new BufferedReader(res.get().openAsReader())) {
            o = JsonParser.parseReader(r).getAsJsonObject();
        }
        for (String[] kind : new String[][] {{"vertex", "vsh"}, {"fragment", "fsh"}}) {
            if (!o.has(kind[0])) continue;
            ResourceLocation src = ResourceLocation.parse(o.get(kind[0]).getAsString());
            if (src.getNamespace().equals("minecraft")) continue;
            var s = rm.getResource(ResourceLocation.fromNamespaceAndPath(src.getNamespace(), "shaders/program/" + src.getPath() + "." + kind[1]));
            if (s.isEmpty()) continue;
            try (BufferedReader r = new BufferedReader(s.get().openAsReader())) {
                String line;
                while ((line = r.readLine()) != null) {
                    String t = line.trim();
                    if (t.startsWith("#version")) {
                        String[] parts = t.split("\\s+");
                        int v = parts.length > 1 ? Integer.parseInt(parts[1].replaceAll("\\D", "")) : 0;
                        if (v < 150) return "uses #version " + v;
                        break;
                    }
                }
            }
        }
        return null;
    }
}
