package org.kylepitcock.legacyconnector.loader.transform;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Mixins of a Forge 1.20.1 mod, made to survive Minecraft 1.21.1 (milestone 2). A 1.20.1 mixin that cannot apply must never abort the launch (Mixin
 * aborts on a required config, and on an injector whose {@code require} is not met whatever the config says), so the translator:
 * <ul>
 *   <li>moves the names inside annotation strings (the refmap is gone): SRG names to Mojang names, class and member renames of {@code mc-classes.tsv} and
 *       {@code mc-members.tsv} in {@code method}, {@code targets} and {@code @At(target)};</li>
 *   <li>drops the {@code require} of every injector (a miss then injects nothing instead of throwing {@code InjectionError});</li>
 *   <li>drops mixin classes whose targets are all Minecraft/NeoForge classes that do not exist in 1.21.1 (or Forge classes that have no equivalent) from
 *       the config, noting each one;</li>
 *   <li>marks the config {@code required: false} with {@code defaultRequire: 0} and no refmap, so an invalid mixin (missing shadow member, bad accessor) is
 *       logged and skipped.</li>
 * </ul>
 */
final class Mixin1201 {
    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final Pattern MEMBER = Pattern.compile("^(L[^;]+;)?([^(:\\s]*)(\\(.*|:.*)?$");

    /** Totals for the report line. */
    static final class Stats {
        int mixinClasses;
        int stringsMoved;
        int requireRemoved;
        final List<String> dropped = new ArrayList<>();
        /** internal names of mixin classes dropped from their configs */
        final Set<String> droppedClasses = new HashSet<>();

        String summary() {
            return mixinClasses + " mixin class(es), " + stringsMoved + " annotation name(s) moved to 1.21.1, " + requireRemoved + " injector requirement(s) relaxed, " + dropped.size()
                    + " mixin(s) dropped (target missing in 1.21.1)";
        }
    }

    private Mixin1201() {}

    static boolean isMixin(ClassNode node) {
        return find(node.invisibleAnnotations, MIXIN) != null || find(node.visibleAnnotations, MIXIN) != null;
    }

    private static AnnotationNode find(List<AnnotationNode> list, String desc) {
        if (list != null) for (AnnotationNode a : list) if (desc.equals(a.desc)) return a;
        return null;
    }

    /** Fixes the annotations of one class (already renamed to Mojang class names); records it in {@code st} when it must be dropped from its config. */
    static void apply(ClassNode node, Map<String, String> srg, Mc1201Hierarchy h, Mc1201Tables t, Stats st) {
        AnnotationNode mixin = find(node.invisibleAnnotations, MIXIN);
        if (mixin == null) mixin = find(node.visibleAnnotations, MIXIN);
        if (mixin == null) return;
        st.mixinClasses++;
        List<String> targets = targets(mixin);
        Ctx ctx = new Ctx(srg, t, targets, st);
        // class level: string targets
        fixAnnotation(mixin, ctx);
        targets = targets(mixin);
        for (MethodNode m : node.methods) {
            fixAll(m.invisibleAnnotations, ctx);
            fixAll(m.visibleAnnotations, ctx);
        }
        for (FieldNode f : node.fields) {
            fixAll(f.invisibleAnnotations, ctx);
            fixAll(f.visibleAnnotations, ctx);
        }
        // a mixin whose targets are all gone from 1.21.1 cannot apply
        if (!targets.isEmpty()) {
            boolean anyPresent = false;
            String missing = null;
            for (String target : targets) {
                if (isMissing(target, h)) missing = target;
                else anyPresent = true;
            }
            if (!anyPresent && missing != null) {
                st.dropped.add(node.name + " (target " + missing + ")");
                st.droppedClasses.add(node.name);
            }
        }
    }

    private static boolean isMissing(String target, Mc1201Hierarchy h) {
        if (target.startsWith("net/minecraftforge/")) return true; // mapClass would have moved it to NeoForge or a facade
        if (target.startsWith("net/minecraft/") || target.startsWith("com/mojang/blaze3d/") || target.startsWith("com/mojang/math/")
                || target.startsWith("net/neoforged/neoforge/")) return !h.isMinecraft(target);
        return false;
    }

    /** Targets of a {@code @Mixin}: class values and string targets (internal names). */
    private static List<String> targets(AnnotationNode mixin) {
        List<String> out = new ArrayList<>();
        if (mixin.values == null) return out;
        for (int i = 0; i + 1 < mixin.values.size(); i += 2) {
            String key = (String) mixin.values.get(i);
            if (!(mixin.values.get(i + 1) instanceof List<?> list)) continue;
            for (Object o : list) {
                if (o instanceof Type ty) out.add(ty.getInternalName());
                else if (key.equals("targets")) out.add(String.valueOf(o).replace('.', '/'));
            }
        }
        return out;
    }

    private record Ctx(Map<String, String> srg, Mc1201Tables t, List<String> targets, Stats st) {}

    private static void fixAll(List<AnnotationNode> list, Ctx ctx) {
        if (list == null) return;
        for (AnnotationNode a : list) fixAnnotation(a, ctx);
    }

    private static boolean ours(String desc) {
        return desc.startsWith("Lorg/spongepowered/asm/mixin/") || desc.startsWith("Lcom/llamalad7/mixinextras/");
    }

    private static void fixAnnotation(AnnotationNode a, Ctx ctx) {
        if (!ours(a.desc) || a.values == null) return;
        boolean injector = a.desc.contains("/injection/") || a.desc.startsWith("Lcom/llamalad7/mixinextras/injector/") || a.desc.startsWith("Lcom/llamalad7/mixinextras/expression/");
        boolean accessor = a.desc.endsWith("/gen/Accessor;") || a.desc.endsWith("/gen/Invoker;");
        for (int i = 0; i + 1 < a.values.size(); i += 2) {
            String key = (String) a.values.get(i);
            Object v = a.values.get(i + 1);
            boolean isAt = a.desc.endsWith("/injection/At;");
            if (a.desc.equals(MIXIN) && key.equals("targets") && v instanceof List<?> l) {
                a.values.set(i + 1, mapEach(l, s -> mapClassName(s, ctx)));
            } else if ((key.equals("method") || key.equals("value") && accessor) && v instanceof List<?> l) {
                a.values.set(i + 1, mapEach(l, s -> mapMember(s, ctx)));
            } else if (key.equals("value") && accessor && v instanceof String s) {
                a.values.set(i + 1, mapMember(s, ctx));
            } else if (key.equals("target") && isAt && v instanceof String s) {
                a.values.set(i + 1, mapMember(s, ctx));
            } else if (v instanceof AnnotationNode nested) {
                fixAnnotation(nested, ctx);
            } else if (v instanceof List<?> l) {
                for (Object o : l) if (o instanceof AnnotationNode nested) fixAnnotation(nested, ctx);
            }
        }
        if (injector) {
            for (int i = 0; i + 1 < a.values.size(); i += 2) {
                if ("require".equals(a.values.get(i)) && a.values.get(i + 1) instanceof Integer n && n > 0) {
                    a.values.set(i + 1, 0);
                    ctx.st.requireRemoved++;
                }
            }
        }
    }

    private static List<Object> mapEach(List<?> in, java.util.function.UnaryOperator<String> f) {
        List<Object> out = new ArrayList<>(in.size());
        for (Object o : in) out.add(o instanceof String s ? f.apply(s) : o);
        return out;
    }

    private static String mapClassName(String name, Ctx ctx) {
        String internal = name.replace('.', '/');
        String r = ctx.t.classes.get(internal);
        if (r != null && !r.startsWith("org/kylepitcock/")) {
            ctx.st.stringsMoved++;
            return name.indexOf('/') >= 0 ? r : r.replace('/', '.');
        }
        return name;
    }

    /** {@code name}, {@code name(desc)ret}, {@code Lowner;name(desc)ret} or {@code Lowner;field:Ldesc;} with Mojang names. */
    private static String mapMember(String spec, Ctx ctx) {
        Matcher m = MEMBER.matcher(spec);
        if (!m.matches()) return spec;
        String ownerPart = m.group(1);
        String name = m.group(2);
        String rest = m.group(3) == null ? "" : m.group(3);
        String owner = ownerPart == null ? null : ownerPart.substring(1, ownerPart.length() - 1);
        String origName = name;
        if (name.length() > 3 && name.charAt(1) == '_') {
            String mojang = ctx.srg.get(name);
            if (mojang != null) name = mojang;
        }
        String desc = rest;
        // member renames: the owner is the 1.20.1 class (annotation owner, or a mixin target)
        List<String> owners = new ArrayList<>();
        if (owner != null) owners.add(owner);
        else owners.addAll(ctx.targets);
        String renamed = null;
        for (String o : owners) {
            String d = descForLookup(desc);
            String r = ctx.t.renamedMember(o, name, d);
            if (!r.equals(name)) {
                renamed = r;
                break;
            }
            // targets were already moved by the class table: look for the old owner too
        }
        if (renamed == null) {
            for (String o : owners) {
                for (var e : ctx.t.classes.entrySet()) {
                    if (e.getValue().equals(o)) {
                        String r = ctx.t.renamedMember(e.getKey(), name, descForLookup(desc));
                        if (!r.equals(name)) renamed = r;
                    }
                }
                if (renamed != null) break;
            }
        }
        if (renamed != null) name = renamed;
        String newOwner = owner;
        if (owner != null) {
            String c = ctx.t.classes.get(owner);
            if (c != null && !c.startsWith("org/kylepitcock/")) newOwner = c;
        }
        String newDesc = desc.isEmpty() ? desc : mapDescriptor(desc, ctx.t);
        if (newOwner != null && newOwner.equals(owner) && name.equals(origName) && newDesc.equals(desc)) return spec;
        ctx.st.stringsMoved++;
        return (newOwner == null ? "" : "L" + newOwner + ";") + name + newDesc;
    }

    private static String descForLookup(String rest) {
        if (rest.startsWith(":")) return rest.length() > 1 ? rest : ":*";
        int close = rest.indexOf(')');
        return rest.startsWith("(") && close > 0 ? rest : "*";
    }

    /** Class names inside a descriptor that changed in Minecraft (not the compatibility classes that stand in for them in mod code). */
    private static String mapDescriptor(String desc, Mc1201Tables t) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < desc.length()) {
            char c = desc.charAt(i);
            if (c == 'L') {
                int semi = desc.indexOf(';', i);
                if (semi < 0) {
                    out.append(desc, i, desc.length());
                    break;
                }
                String cls = desc.substring(i + 1, semi);
                String r = t.classes.get(cls);
                out.append('L').append(r != null && !r.startsWith("org/kylepitcock/") ? r : cls).append(';');
                i = semi + 1;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    // ---- config -------------------------------------------------------------------------------------------------------------

    /**
     * A mixin config made harmless: no refmap (the annotations are Mojang-named), not required, injectors default to no requirement, and the mixin classes
     * in {@code dropped} (internal names) removed from its lists. Returns the original text when it is not a mixin config.
     */
    static String config(String json, Set<String> dropped, List<String> notes) {
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) return json;
            JsonObject root = parsed.getAsJsonObject();
            if (!root.has("package") || !root.has("mixins") && !root.has("client") && !root.has("server")) return json;
            String pkg = root.get("package").getAsString().replace('.', '/') + "/";
            root.remove("refmap");
            root.addProperty("required", false);
            JsonObject injectors = root.has("injectors") && root.get("injectors").isJsonObject() ? root.getAsJsonObject("injectors") : new JsonObject();
            injectors.addProperty("defaultRequire", 0);
            root.add("injectors", injectors);
            for (String section : new String[] {"mixins", "client", "server"}) {
                if (!root.has(section) || !root.get(section).isJsonArray()) continue;
                JsonArray out = new JsonArray();
                for (JsonElement e : root.getAsJsonArray(section)) {
                    String internal = pkg + e.getAsString().replace('.', '/');
                    if (dropped.contains(internal)) {
                        if (notes != null) notes.add("Mixin " + internal + " dropped: its target class does not exist in Minecraft 1.21.1");
                        continue;
                    }
                    out.add(e);
                }
                root.add(section, out);
            }
            return root.toString();
        } catch (RuntimeException e) {
            return json;
        }
    }
}
