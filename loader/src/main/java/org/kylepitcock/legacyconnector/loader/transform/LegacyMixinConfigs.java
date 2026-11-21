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
                List<String> dropped = new ArrayList<>();
                // which mixin classes of the jar apply: a mixin whose superclass is a mixin that does not apply cannot apply either (Mixin
                // fails the whole target class, and a failed super call is a fatal transformer error)
                java.util.Set<String> mixinClasses = new java.util.HashSet<>(), ok = new java.util.HashSet<>();
                for (String section : new String[] {"mixins", "client", "server"}) {
                    if (!cfg.has(section) || !cfg.get(section).isJsonArray()) continue;
                    for (JsonElement m : cfg.getAsJsonArray(section)) {
                        String cls = pkg + m.getAsString().replace('.', '/');
                        mixinClasses.add(cls);
                        ClassNode node = nodes.get(cls);
                        if (node != null && targetsLegacy(node, nodes, hierarchy)) ok.add(cls);
                    }
                }
                for (boolean changed = true; changed; ) {
                    changed = false;
                    for (var it = ok.iterator(); it.hasNext(); ) {
                        String cls = it.next();
                        for (String sup = nodes.get(cls).superName; sup != null && mixinClasses.contains(sup); sup = nodes.containsKey(sup) ? nodes.get(sup).superName : null) {
                            if (!ok.contains(sup)) {
                                it.remove();
                                changed = true;
                                break;
                            }
                        }
                    }
                }
                for (String section : new String[] {"mixins", "client", "server"}) {
                    if (!cfg.has(section) || !cfg.get(section).isJsonArray()) continue;
                    JsonArray in = cfg.getAsJsonArray(section);
                    JsonArray out = new JsonArray();
                    for (JsonElement m : in) {
                        total++;
                        if (ok.contains(pkg + m.getAsString().replace('.', '/'))) {
                            out.add(m);
                            kept++;
                        } else {
                            dropped.add(m.getAsString());
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
                notes.add("Mixin config " + name + ": " + kept + " of " + total + " mixin(s) target translated mod classes or legacy API classes that have their members and are applied"
                        + (dropped.isEmpty() ? "" : "; not applied: " + String.join(", ", dropped.subList(0, Math.min(8, dropped.size()))) + (dropped.size() > 8 ? ", ..." : "")));
            } catch (RuntimeException ex) {
                notes.add("Mixin config " + name + " could not be read: " + ex);
            }
        }
        return notes;
    }

    private static boolean isShimMixin(ClassNode mixin, Hierarchy hierarchy) {
        if (mixin.invisibleAnnotations == null) return false;
        for (AnnotationNode a : mixin.invisibleAnnotations) {
            if (!MIXIN_DESC.equals(a.desc) || a.values == null) continue;
            for (int i = 0; i + 1 < a.values.size(); i += 2) {
                if (a.values.get(i + 1) instanceof List<?> list) {
                    for (Object o : list) if (o instanceof Type t && hierarchy.isShim(t.getInternalName())) return true;
                }
            }
        }
        return false;
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
                    if (hierarchy.isShim(target) && shadowsExist(mixin, target, hierarchy) && injectorsResolve(mixin, target, hierarchy)) return true;
                }
            }
        }
        return false;
    }

    /**
     * Before translation (legacy names): a mixin on a vanilla class reads and writes that class's fields through {@code @Shadow} copies
     * ({@code this.isDead}, {@code this.motionX}). Most of those are virtual on the shim class (a view of a vanilla wolf answers the
     * modern wolf's position, not a field), so the copies would read stale values. Every access of a shadowed field in the jar's mixin
     * classes is made an access of the target class instead, which the translation turns into the shim's accessor like any mod code,
     * and the {@code @Shadow} field declarations are dropped (no shadow is left for Mixin to look up). Mixins on the jar's own classes are
     * left alone. Returns the notes for the report.
     */
    static List<String> retargetShadowFields(Map<String, byte[]> classes) {
        Map<String, ClassNode> mixins = new java.util.HashMap<>();
        Map<String, String> target = new java.util.HashMap<>();
        for (var e : classes.entrySet()) {
            byte[] b = e.getValue();
            if (!contains(b, MIXIN_DESC) || !contains(b, SHADOW_DESC)) {
                if (contains(b, MIXIN_DESC)) {
                    // a mixin without shadows of its own can still inherit some from a mixin superclass: keep it for the chain walk
                    ClassNode n = read(b);
                    String t = n == null ? null : vanillaTarget(n, classes);
                    if (t != null) {
                        mixins.put(n.name, n);
                        target.put(n.name, t);
                    }
                }
                continue;
            }
            ClassNode n = read(b);
            if (n == null) continue;
            String t = vanillaTarget(n, classes);
            if (t == null) continue;
            mixins.put(n.name, n);
            target.put(n.name, t);
        }
        if (mixins.isEmpty()) return List.of();
        // the shadow fields each mixin sees: its own and its mixin superclasses'
        Map<String, java.util.Set<String>> shadows = new java.util.HashMap<>();
        for (ClassNode n : mixins.values()) {
            java.util.Set<String> names = new java.util.HashSet<>();
            for (ClassNode c = n; c != null; c = mixins.get(c.superName)) {
                for (var f : c.fields) if (hasAnnotation(anns(f.visibleAnnotations, f.invisibleAnnotations), SHADOW_DESC)) names.add(f.name);
            }
            shadows.put(n.name, names);
        }
        int moved = 0;
        java.util.Set<String> changed = new java.util.HashSet<>();
        for (ClassNode n : mixins.values()) {
            for (var m : n.methods) {
                for (var insn : m.instructions) {
                    if (!(insn instanceof org.objectweb.asm.tree.FieldInsnNode f)) continue;
                    var seen = shadows.get(f.owner);
                    if (seen == null || !seen.contains(f.name)) continue;
                    f.owner = target.get(f.owner);
                    moved++;
                    changed.add(n.name);
                }
            }
        }
        for (ClassNode n : mixins.values()) {
            if (n.fields.removeIf(f -> hasAnnotation(anns(f.visibleAnnotations, f.invisibleAnnotations), SHADOW_DESC))) changed.add(n.name);
        }
        for (String name : changed) {
            org.objectweb.asm.ClassWriter w = new org.objectweb.asm.ClassWriter(0);
            mixins.get(name).accept(w);
            classes.put(name, w.toByteArray());
        }
        return moved == 0 ? List.of() : List.of("Mixins on vanilla classes: " + moved + " access(es) of @Shadow fields in " + changed.size()
                + " mixin class(es) go to the vanilla class's fields (translated like the mod's own code)");
    }

    private static ClassNode read(byte[] b) {
        try {
            ClassNode n = new ClassNode();
            new org.objectweb.asm.ClassReader(b).accept(n, 0);
            return n;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** The single vanilla Minecraft/Forge class a mixin targets (not one of the jar's own classes), or null. */
    private static String vanillaTarget(ClassNode mixin, Map<String, byte[]> classes) {
        if (mixin.invisibleAnnotations == null) return null;
        String found = null;
        for (AnnotationNode a : mixin.invisibleAnnotations) {
            if (!MIXIN_DESC.equals(a.desc) || a.values == null) continue;
            for (int i = 0; i + 1 < a.values.size(); i += 2) {
                String key = (String) a.values.get(i);
                if (!(a.values.get(i + 1) instanceof List<?> list)) continue;
                for (Object o : list) {
                    String t = o instanceof Type type ? type.getInternalName() : key.equals("targets") ? String.valueOf(o).replace('.', '/') : null;
                    if (t == null) continue;
                    if (classes.containsKey(t) || !(t.startsWith("net/minecraft/") || t.startsWith("net/minecraftforge/"))) return null;
                    if (found != null && !found.equals(t)) return null; // several targets: a shadow access has no single owner
                    found = t;
                }
            }
        }
        return found;
    }

    private static final String SHADOW_DESC = "Lorg/spongepowered/asm/mixin/Shadow;";
    private static final String ACCESSOR_DESC = "Lorg/spongepowered/asm/mixin/gen/Accessor;";
    private static final String INVOKER_DESC = "Lorg/spongepowered/asm/mixin/gen/Invoker;";

    /** True when every @Shadow, @Accessor and @Invoker of the mixin names a member the shim class (or a shim supertype) has. */
    static boolean shadowsExist(ClassNode mixin, String target, Hierarchy hierarchy) {
        for (var f : mixin.fields) {
            if (hasAnnotation(anns(f.visibleAnnotations, f.invisibleAnnotations), SHADOW_DESC) && !hasField(target, f.name, hierarchy)) return false;
        }
        for (var m : mixin.methods) {
            if (hasAnnotation(anns(m.visibleAnnotations, m.invisibleAnnotations), SHADOW_DESC)) {
                if (!hasMethod(target, m.name, hierarchy)) return false;
            } else if (hasAnnotation(anns(m.visibleAnnotations, m.invisibleAnnotations), ACCESSOR_DESC) || hasAnnotation(anns(m.visibleAnnotations, m.invisibleAnnotations), INVOKER_DESC)) {
                String name = annotationValue(anns(m.visibleAnnotations, m.invisibleAnnotations));
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
                boolean invoker = hasAnnotation(anns(m.visibleAnnotations, m.invisibleAnnotations), INVOKER_DESC);
                if (invoker ? !hasMethod(target, name, hierarchy) : !hasField(target, name, hierarchy)) return false;
            }
        }
        return true;
    }

    private static final String OVERWRITE_DESC = "Lorg/spongepowered/asm/mixin/Overwrite;";

    /**
     * True when every injector of the mixin (Mixin's own, and MixinExtras') names methods the shim class declares itself, and every
     * {@code @Overwrite} overwrites one: Mixin only injects into methods the target class declares, not inherited ones. The names in
     * the annotations are the mod's deobfuscated (MCP) ones, as the shim uses them. A mixin that fails this stays dropped.
     */
    static boolean injectorsResolve(ClassNode mixin, String target, Hierarchy hierarchy) {
        var info = hierarchy.find(target);
        if (info == null) return false;
        for (var m : mixin.methods) {
            for (AnnotationNode a : anns(m.visibleAnnotations, m.invisibleAnnotations)) {
                if (OVERWRITE_DESC.equals(a.desc)) {
                    if (!declares(info, m.name)) return false;
                    continue;
                }
                if (!a.desc.startsWith("Lorg/spongepowered/asm/mixin/injection/") && !a.desc.startsWith("Lcom/llamalad7/mixinextras/injector/")) continue;
                if (a.values == null) continue;
                for (int i = 0; i + 1 < a.values.size(); i += 2) {
                    if (!"method".equals(a.values.get(i))) continue;
                    Object v = a.values.get(i + 1);
                    if (!(v instanceof List<?> list) || list.isEmpty()) return false;
                    for (Object o : list) {
                        String name = methodName(String.valueOf(o));
                        if (name.isEmpty() || !declares(info, name)) return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean declares(org.kylepitcock.legacyconnector.loader.shim.ClassInfo info, String name) {
        for (String k : info.methods.keySet()) if (k.startsWith(name + "(")) return true;
        return false;
    }

    /** The bare method name of a Mixin selector: {@code Lowner;name(desc)ret}, {@code name(desc)ret}, {@code name}. */
    private static String methodName(String selector) {
        String s = selector.trim();
        if (s.startsWith("L")) {
            int semi = s.indexOf(';');
            if (semi > 0 && semi < s.length() - 1) s = s.substring(semi + 1);
        }
        int paren = s.indexOf('(');
        if (paren >= 0) s = s.substring(0, paren);
        return s;
    }

    /** Both annotation lists of a member (Mixin's @Shadow, @Inject, @Accessor are runtime-visible, @Mixin is not). */
    private static List<AnnotationNode> anns(List<AnnotationNode> visible, List<AnnotationNode> invisible) {
        if (visible == null) return invisible == null ? List.of() : invisible;
        if (invisible == null) return visible;
        List<AnnotationNode> all = new ArrayList<>(visible);
        all.addAll(invisible);
        return all;
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
