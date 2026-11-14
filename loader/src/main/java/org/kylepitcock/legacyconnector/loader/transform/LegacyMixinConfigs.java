package org.kylepitcock.legacyconnector.loader.transform;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.kylepitcock.legacyconnector.loader.LegacyJar;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

/**
 * Mixin configs shipped by a 1.12.2 mod (MixinBooter / Mixin 0.7 style: {@code MixinConfigs} manifest attribute, early and late
 * mixin loaders; all of them leave the config json in the jar). They are registered with NeoForge through {@code [[mixins]]} in the
 * generated {@code neoforge.mods.toml}, so Mixin applies them like any modern mod's.
 *
 * <p>Only mixins that target a class of a translated legacy jar survive: mixins aimed at vanilla Minecraft or Forge classes cannot
 * apply (the 1.12 classes are shim classes here, with other members). The config is rewritten to be harmless: renamed to
 * {@code legacyconnector.legacy.*} (the name the runtime mixin guard recognizes as ours), not required, no refmap (the names in
 * the annotations are the mod's own deobfuscated ones) and no config plugin (it runs legacy launcher code).
 */
final class LegacyMixinConfigs {
    static final String PREFIX = "legacyconnector.legacy.";
    private static final String MIXIN_DESC = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final Gson GSON = new Gson();

    private LegacyMixinConfigs() {}

    /**
     * Moves the mixin configs found in {@code other} to their rewritten form, returning the notes to report. The resource names of
     * the rewritten configs are added to {@code jar.mixinConfigResources}.
     */
    static List<String> apply(LegacyJar jar, Map<String, byte[]> other, Map<String, ClassNode> nodes, Hierarchy hierarchy) {
        List<String> notes = new ArrayList<>();
        List<String> candidates = new ArrayList<>();
        for (var e : other.entrySet()) {
            String name = e.getKey();
            if (!name.endsWith(".json") || name.startsWith("assets/") || name.startsWith("data/") || name.startsWith("META-INF/")) continue;
            if (e.getValue().length > 512 * 1024 || !contains(e.getValue(), "\"package\"")) continue;
            candidates.add(name);
        }
        String jarKey = jar.fileName().replaceAll("(?i)\\.jar$", "").replaceAll("[^A-Za-z0-9_-]", "_");
        for (String name : candidates) {
            try {
                JsonElement parsed = GSON.fromJson(new String(other.get(name), StandardCharsets.UTF_8), JsonElement.class);
                if (parsed == null || !parsed.isJsonObject()) continue;
                JsonObject cfg = parsed.getAsJsonObject();
                if (!cfg.has("package") || !cfg.get("package").isJsonPrimitive()) continue;
                String pkg = cfg.get("package").getAsString().replace('.', '/') + "/";
                int total = 0, kept = 0;
                for (String section : new String[] {"mixins", "client", "server"}) {
                    if (!cfg.has(section) || !cfg.get(section).isJsonArray()) continue;
                    JsonArray in = cfg.getAsJsonArray(section);
                    JsonArray out = new JsonArray();
                    for (JsonElement m : in) {
                        total++;
                        ClassNode node = nodes.get(pkg + m.getAsString().replace('.', '/'));
                        if (node != null && targetsLegacy(node, nodes, hierarchy)) {
                            out.add(m);
                            kept++;
                        }
                    }
                    cfg.add(section, out);
                }
                if (total == 0) continue; // not a mixin config after all
                if (kept == 0) {
                    notes.add("Mixin config " + name + ": none of its " + total + " mixin(s) target a translated mod class; not applied");
                    continue;
                }
                cfg.addProperty("required", false);
                cfg.remove("refmap");
                cfg.remove("plugin");
                JsonObject injectors = cfg.has("injectors") && cfg.get("injectors").isJsonObject() ? cfg.getAsJsonObject("injectors") : new JsonObject();
                injectors.addProperty("defaultRequire", 0);
                cfg.add("injectors", injectors);
                String base = name.substring(name.lastIndexOf('/') + 1);
                String out = PREFIX + jarKey + "." + base;
                other.remove(name);
                other.put(out, GSON.toJson(cfg).getBytes(StandardCharsets.UTF_8));
                jar.mixinConfigResources.add(out);
                notes.add("Mixin config " + name + ": " + kept + " of " + total + " mixin(s) target translated mod classes and are applied; the rest target Minecraft/Forge");
            } catch (RuntimeException ex) {
                notes.add("Mixin config " + name + " could not be read: " + ex);
            }
        }
        return notes;
    }

    private static boolean targetsLegacy(ClassNode mixin, Map<String, ClassNode> nodes, Hierarchy hierarchy) {
        List<AnnotationNode> annotations = mixin.invisibleAnnotations;
        if (annotations == null) return false;
        for (AnnotationNode a : annotations) {
            if (!MIXIN_DESC.equals(a.desc) || a.values == null) continue;
            for (int i = 0; i + 1 < a.values.size(); i += 2) {
                String key = (String) a.values.get(i);
                Object value = a.values.get(i + 1);
                if (!(value instanceof List<?> list)) continue;
                for (Object o : list) {
                    String target = o instanceof Type t ? t.getInternalName() : key.equals("targets") ? String.valueOf(o).replace('.', '/') : null;
                    if (target == null) continue;
                    if (nodes.containsKey(target) || hierarchy.isLocal(target) || hierarchy.isPeer(target)) return true;
                    // a mixin on a vanilla class the shim has (EntityWolf: Wolf Armor and Storage makes it implement its IArmoredWolf):
                    // the shim class is what the mod sees at run time, so the mixin applies there when its @Shadow/@Accessor members exist
                    if (hierarchy.isShim(target) && shadowsExist(mixin, target, hierarchy)) return true;
                }
            }
        }
        return false;
    }

    private static final String SHADOW_DESC = "Lorg/spongepowered/asm/mixin/Shadow;";
    private static final String ACCESSOR_DESC = "Lorg/spongepowered/asm/mixin/gen/Accessor;";
    private static final String INVOKER_DESC = "Lorg/spongepowered/asm/mixin/gen/Invoker;";

    /** True when every @Shadow, @Accessor and @Invoker of the mixin names a member the shim class (or a shim supertype) has. */
    static boolean shadowsExist(ClassNode mixin, String target, Hierarchy hierarchy) {
        for (var f : mixin.fields) {
            if (hasAnnotation(f.invisibleAnnotations, SHADOW_DESC) && !hasField(target, f.name, hierarchy)) return false;
        }
        for (var m : mixin.methods) {
            if (hasAnnotation(m.invisibleAnnotations, SHADOW_DESC)) {
                if (!hasMethod(target, m.name, hierarchy)) return false;
            } else if (hasAnnotation(m.invisibleAnnotations, ACCESSOR_DESC) || hasAnnotation(m.invisibleAnnotations, INVOKER_DESC)) {
                String name = annotationValue(m.invisibleAnnotations);
                if (name == null) {
                    String n = m.name;
                    for (String prefix : new String[] {"get", "set", "is", "call", "invoke"}) {
                        if (n.startsWith(prefix) && n.length() > prefix.length()) {
                            name = Character.toLowerCase(n.charAt(prefix.length())) + n.substring(prefix.length() + 1);
                            break;
                        }
                    }
                    if (name == null) return false;
                }
                boolean invoker = hasAnnotation(m.invisibleAnnotations, INVOKER_DESC);
                if (invoker ? !hasMethod(target, name, hierarchy) : !hasField(target, name, hierarchy)) return false;
            }
        }
        return true;
    }

    private static boolean hasAnnotation(List<AnnotationNode> list, String desc) {
        if (list == null) return false;
        for (AnnotationNode a : list) if (desc.equals(a.desc)) return true;
        return false;
    }

    private static String annotationValue(List<AnnotationNode> list) {
        if (list == null) return null;
        for (AnnotationNode a : list) {
            if (!ACCESSOR_DESC.equals(a.desc) && !INVOKER_DESC.equals(a.desc)) continue;
            if (a.values == null) return null;
            for (int i = 0; i + 1 < a.values.size(); i += 2) {
                if ("value".equals(a.values.get(i)) && a.values.get(i + 1) instanceof String s) return s;
            }
        }
        return null;
    }

    private static boolean hasField(String owner, String name, Hierarchy hierarchy) {
        for (String c = owner; c != null && c.startsWith("legacyconnector/"); ) {
            var info = hierarchy.find(c);
            if (info == null) return false;
            if (info.hasFieldNamed(name)) return true;
            if (hierarchy.shim().contains(c) && info.methods.keySet().stream().anyMatch(k -> k.startsWith("mcc$get$" + name + "("))) return true;
            c = info.superName;
        }
        return false;
    }

    private static boolean hasMethod(String owner, String name, Hierarchy hierarchy) {
        for (String c = owner; c != null && c.startsWith("legacyconnector/"); ) {
            var info = hierarchy.find(c);
            if (info == null) return false;
            for (String k : info.methods.keySet()) if (k.startsWith(name + "(")) return true;
            c = info.superName;
        }
        return false;
    }

    private static boolean contains(byte[] data, String needle) {
        return new String(data, StandardCharsets.ISO_8859_1).contains(needle);
    }
}
