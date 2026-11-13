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
import org.kylepitcock.legacyconnector.loader.transform.mixin1201.InjectionRepair;
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
    private static final String LOCAL_CAPTURE = "Lorg/spongepowered/asm/mixin/injection/callback/LocalCapture;";
    private static final Pattern MEMBER = Pattern.compile("^(L[^;]+;)?([^(:\\s]*)(\\(.*|:.*)?$");

    /** Totals for the report line. */
    static final class Stats {
        int mixinClasses;
        int stringsMoved;
        int requireRemoved;
        int localsRelaxed;
        final List<String> dropped = new ArrayList<>();
        /** internal names of mixin classes dropped from their configs */
        final Set<String> droppedClasses = new HashSet<>();
        /** the target proof and handler re-signing of {@link InjectionRepair} */
        final InjectionRepair.Counts repair = new InjectionRepair.Counts();
        /** member renames applied from the tables (the "renamed" class of the proof) */
        int membersRenamed;
        /** old descriptors from the jar's refmaps (mixin class to annotation string to descriptor), set by the transformer before the classes are processed */
        Map<String, Map<String, String>> refmaps = Map.of();

        String summary() {
            return mixinClasses + " mixin class(es), " + stringsMoved + " annotation name(s) moved to 1.21.1, " + requireRemoved + " injector requirement(s) relaxed, " + localsRelaxed + " local capture(s) set to FAILSOFT (a vanilla method whose locals changed skips that injection with a Mixin warning naming the mod's mixin), " + dropped.size()
                    + " mixin(s) dropped (target missing in 1.21.1); target proof: " + repair.summary() + ", " + membersRenamed + " member name(s) renamed";
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
    static int apply(ClassNode node, Map<String, String> srg, Mc1201Hierarchy h, Mc1201Tables t, Stats st) {
        AnnotationNode mixin = find(node.invisibleAnnotations, MIXIN);
        if (mixin == null) mixin = find(node.visibleAnnotations, MIXIN);
        if (mixin == null) return 0;
        st.mixinClasses++;
        List<String> targets = targets(mixin);
        Ctx ctx = new Ctx(srg, t, targets, st);
        // the refmap's 1.20.1 descriptors of the selectors, read before the strings are rewritten
        java.util.Map<AnnotationNode, List<String>> hints = InjectionRepair.refmapHints(node, st.refmaps.get(node.name), d -> mapDescriptor(d, t));
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
        for (MethodNode m : node.methods) defineSynchedDataHandler(m);
        String shadow = missingShadow(node, targets, h);
        if (shadow != null) {
            st.dropped.add(node.name + " (@Shadow " + shadow + " does not exist in " + targets.get(0) + ")");
            st.droppedClasses.add(node.name);
            return 0;
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
                return 0;
            }
        }
        // target proof: every injector against the 1.21.1 classes; added parameters re-signed, gone or unrepairable injections removed one by one
        int before = st.repair.handlersChanged;
        InjectionRepair.run(node, targets, new McHostIndex1201(h), hints, st.repair);
        return st.repair.handlersChanged - before;
    }

    private static final String SHADOW = "Lorg/spongepowered/asm/mixin/Shadow;";

    /**
     * The first {@code @Shadow} member of the mixin that its single Minecraft target (and the classes above it) does not have in 1.21.1, or null. Mixin would
     * refuse the whole mixin with an {@code InvalidMixinException} at apply time; dropping it from the config names the reason in the report instead.
     */
    static String missingShadow(ClassNode node, List<String> targets, Mc1201Hierarchy h) {
        if (targets.size() != 1) return null;
        String target = targets.get(0);
        if (!target.startsWith("net/minecraft/") || McMembers1201.get(target) == null) return null;
        for (MethodNode m : node.methods) {
            if (!hasShadow(m.invisibleAnnotations) && !hasShadow(m.visibleAnnotations)) continue;
            if (shadowHasPrefix(m.invisibleAnnotations) || shadowHasPrefix(m.visibleAnnotations)) continue;
            Boolean found = hasMember(target, m.name + m.desc, true, h, new java.util.HashSet<>());
            if (found != null && !found) return m.name + m.desc;
        }
        for (MethodNode m : node.methods) {
            // an @Accessor of a field whose type changed in 1.21.1 (LootPool.entries: array to List) cannot be found by Mixin either
            AnnotationNode acc = find(m.invisibleAnnotations, "Lorg/spongepowered/asm/mixin/gen/Accessor;");
            if (acc == null) acc = find(m.visibleAnnotations, "Lorg/spongepowered/asm/mixin/gen/Accessor;");
            if (acc == null) continue;
            String field = null;
            if (acc.values != null) for (int i = 0; i + 1 < acc.values.size(); i += 2) if ("value".equals(acc.values.get(i)) && acc.values.get(i + 1) instanceof String s) field = s;
            if (field == null) {
                String n = m.name;
                if (n.startsWith("get") || n.startsWith("set")) n = n.substring(3);
                else if (n.startsWith("is")) n = n.substring(2);
                else continue;
                if (n.isEmpty()) continue;
                field = Character.toLowerCase(n.charAt(0)) + n.substring(1);
            }
            Type[] args = Type.getArgumentTypes(m.desc);
            String desc = args.length == 1 && Type.getReturnType(m.desc).getSort() == Type.VOID ? args[0].getDescriptor()
                    : args.length == 0 ? Type.getReturnType(m.desc).getDescriptor() : null;
            if (desc == null) continue;
            Boolean found = hasMember(target, field + ":" + desc, false, h, new java.util.HashSet<>());
            // the name may not be the field's own (a mapped name): only a field with this name and another type is a change we know of
            Boolean byName = hasFieldNamed(target, field, h, new java.util.HashSet<>());
            if (found != null && !found && Boolean.TRUE.equals(byName)) return m.name + m.desc + " (@Accessor " + field + ")";
        }
        for (FieldNode f : node.fields) {
            if (!hasShadow(f.invisibleAnnotations) && !hasShadow(f.visibleAnnotations)) continue;
            if (shadowHasPrefix(f.invisibleAnnotations) || shadowHasPrefix(f.visibleAnnotations)) continue;
            Boolean found = hasMember(target, f.name + ":" + f.desc, false, h, new java.util.HashSet<>());
            if (found != null && !found) return f.name + ":" + f.desc;
        }
        return null;
    }

    private static boolean hasShadow(List<AnnotationNode> list) {
        return find(list, SHADOW) != null;
    }

    private static boolean shadowHasPrefix(List<AnnotationNode> list) {
        AnnotationNode a = find(list, SHADOW);
        return a != null && a.values != null && a.values.contains("prefix");
    }

    /** True/false when the class chain is known, null when it leaves into classes the index does not know (then nothing is claimed). */
    private static Boolean hasMember(String owner, String key, boolean method, Mc1201Hierarchy h, java.util.Set<String> seen) {
        if (owner.equals("java/lang/Object")) return method && java.util.Set.of("equals(Ljava/lang/Object;)Z", "hashCode()I", "toString()Ljava/lang/String;").contains(key);
        if (!seen.add(owner)) return false;
        if (!method && owner.startsWith("java/")) return false; // a shadowed field is never one of the JDK's (an interface above the target, BiFunction, made the answer unknown)
        McMembers1201.Members mm = McMembers1201.get(owner);
        if (mm == null) return null;
        if ((method ? mm.methods() : mm.fields()).contains(key)) return true;
        Boolean result = false;
        for (String s : h.supers(owner)) {
            Boolean r = hasMember(s, key, method, h, seen);
            if (r == null) result = null;
            else if (r) return true;
        }
        return result;
    }

    /** True when the class (or one above it) declares a field of this name, whatever its type; null when the chain leaves the index. */
    private static Boolean hasFieldNamed(String owner, String name, Mc1201Hierarchy h, java.util.Set<String> seen) {
        if (owner.startsWith("java/") || !seen.add(owner)) return false;
        McMembers1201.Members mm = McMembers1201.get(owner);
        if (mm == null) return null;
        for (String f : mm.fields()) if (f.startsWith(name + ":")) return true;
        Boolean result = false;
        for (String s : h.supers(owner)) {
            Boolean r = hasFieldNamed(s, name, h, seen);
            if (r == null) result = null;
            else if (r) return true;
        }
        return result;
    }

    private static final String BUILDER ="Lnet/minecraft/network/syncher/SynchedEntityData$Builder;";
    private static final String CALLBACK = "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;";

    /**
     * An {@code @Inject} into {@code Entity.defineSynchedData()} (the place a mod defines its own entity data): the method takes the data builder in 1.21.1, so the
     * target gets the new descriptor and the handler {@code (CallbackInfo)} becomes {@code (Builder, CallbackInfo)}; its locals move up by one and the builder is
     * remembered for {@code SynchedEntityData.define}, which the call table redirects to it. Without this the injection silently matches nothing, the entity data
     * the mod's static {@code defineId} reserved is never defined, and every entity of that class fails in its constructor.
     */
    static void defineSynchedDataHandler(MethodNode m) {
        if (!m.desc.equals("(" + CALLBACK + ")V")) return;
        AnnotationNode inject = null;
        for (List<AnnotationNode> list : java.util.Arrays.asList(m.invisibleAnnotations, m.visibleAnnotations)) {
            if (list != null) for (AnnotationNode a : list) if (a.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")) inject = a;
        }
        if (inject == null || inject.values == null) return;
        boolean changed = false;
        for (int i = 0; i + 1 < inject.values.size(); i += 2) {
            if (!"method".equals(inject.values.get(i)) || !(inject.values.get(i + 1) instanceof List<?> l)) continue;
            @SuppressWarnings("unchecked") List<Object> list = (List<Object>) l;
            for (int k = 0; k < list.size(); k++) {
                if (list.get(k) instanceof String s && s.matches("(L[^;]+;)?defineSynchedData(\\(\\)V)?")) {
                    // an owner-qualified selector keeps its owner
                    int semi = s.indexOf(';');
                    list.set(k, (semi >= 0 ? s.substring(0, semi + 1) : "") + "defineSynchedData(" + BUILDER + ")V");
                    changed = true;
                }
            }
        }
        if (!changed) return;
        int slot = (m.access & org.objectweb.asm.Opcodes.ACC_STATIC) != 0 ? 0 : 1;
        for (org.objectweb.asm.tree.AbstractInsnNode in : m.instructions) {
            if (in instanceof org.objectweb.asm.tree.VarInsnNode v && v.var >= slot) v.var++;
            else if (in instanceof org.objectweb.asm.tree.IincInsnNode v && v.var >= slot) v.var++;
        }
        if (m.localVariables != null) for (org.objectweb.asm.tree.LocalVariableNode lv : m.localVariables) if (lv.index >= slot) lv.index++;
        m.desc = "(" + BUILDER + CALLBACK + ")V";
        m.signature = null;
        org.objectweb.asm.tree.InsnList head = new org.objectweb.asm.tree.InsnList();
        head.add(new org.objectweb.asm.tree.VarInsnNode(org.objectweb.asm.Opcodes.ALOAD, slot));
        head.add(new org.objectweb.asm.tree.MethodInsnNode(org.objectweb.asm.Opcodes.INVOKESTATIC, "org/kylepitcock/legacyconnector/v1_20_1/migrate/MigrateContext",
                "remember", "(Ljava/lang/Object;)V", false));
        m.instructions.insert(head);
        m.maxLocals++;
        m.maxStack = Math.max(m.maxStack, 1);
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
            // @Inject(locals = CAPTURE_FAILHARD/FAILEXCEPTION/PRINT) throws when the 1.21.1 locals differ (not covered by require): FAILSOFT skips the injection
            for (int i = 0; i + 1 < a.values.size(); i += 2) {
                if ("locals".equals(a.values.get(i)) && a.values.get(i + 1) instanceof String[] e && e.length == 2 && LOCAL_CAPTURE.equals(e[0])
                        && !"CAPTURE_FAILSOFT".equals(e[1]) && !"NO_CAPTURE".equals(e[1])) {
                    a.values.set(i + 1, new String[] {LOCAL_CAPTURE, "CAPTURE_FAILSOFT"});
                    ctx.st.localsRelaxed++;
                }
            }
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
        if (renamed != null) {
            name = renamed;
            ctx.st.membersRenamed++;
        }
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
