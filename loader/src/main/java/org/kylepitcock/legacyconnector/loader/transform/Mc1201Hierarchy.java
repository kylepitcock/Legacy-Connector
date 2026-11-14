package org.kylepitcock.legacyconnector.loader.transform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Class structure for the 1.20.1 migration: the classes of the mod jar being translated (declared methods, supertypes) on top of the Minecraft 1.21.1
 * and NeoForge class tree ({@code mc-hierarchy.tsv.gz}, made by {@code tools/dev/gen_mc_hierarchy.py}). The question it answers is "which class on
 * the way up from this receiver or mod class declares the method": a mod class (then the mod's own signature stands) or Minecraft (then the 1.21.1
 * signature applies and the call or override must be migrated).
 */
public final class Mc1201Hierarchy {
    /** Result of walking up from a class looking for a method. */
    public enum Origin { MOD, MINECRAFT, UNKNOWN }

    private record McClass(String superName, String[] interfaces) {}

    private static volatile Map<String, McClass> mc;

    private final Map<String, ClassNode> mod = new HashMap<>();
    /**
     * Classes of the other 1.20.1 jars of the mod set (names after the Forge renames; only the methods the migration tables name are kept, see
     * {@code Forge1201Transformer.scan}). A mod class often extends a library class of another jar ({@code BaseItem}, {@code ItemBlockNBT}): without it the
     * chain above such a class ends in "unknown" and calls through it ({@code super.appendHoverText(...)}) never reach the rules of Minecraft's classes.
     */
    private Map<String, org.kylepitcock.legacyconnector.loader.shim.ClassInfo> peers = Map.of();

    public Mc1201Hierarchy peers(Map<String, org.kylepitcock.legacyconnector.loader.shim.ClassInfo> peers) {
        this.peers = peers == null ? Map.of() : peers;
        return this;
    }

    /** Adds the classes of the jar being translated. */
    public Mc1201Hierarchy add(ClassNode node) {
        mod.put(node.name, node);
        return this;
    }

    private static Map<String, McClass> mc() {
        Map<String, McClass> m = mc;
        if (m != null) return m;
        synchronized (Mc1201Hierarchy.class) {
            if (mc != null) return mc;
            Map<String, McClass> map = new HashMap<>(16384);
            try (InputStream raw = Mc1201Hierarchy.class.getClassLoader().getResourceAsStream("legacyconnector/forward/1.20.1/mc-hierarchy.tsv.gz")) {
                if (raw != null) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(new GZIPInputStream(raw), StandardCharsets.UTF_8));
                    for (String l = r.readLine(); l != null; l = r.readLine()) {
                        String[] f = l.split("\t", -1);
                        if (f.length < 3) continue;
                        map.put(f[0], new McClass(f[1].isEmpty() ? null : f[1], f[2].isEmpty() ? new String[0] : f[2].split(",")));
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("Corrupt mc-hierarchy.tsv.gz", e);
            }
            return mc = map;
        }
    }

    /** Test hook: replaces the Minecraft class tree (name to {super, interface...}). */
    static void setMinecraftForTests(Map<String, String[]> classes) {
        Map<String, McClass> map = new HashMap<>();
        classes.forEach((k, v) -> map.put(k, new McClass(v[0], java.util.Arrays.copyOfRange(v, 1, v.length))));
        mc = map;
    }

    public boolean isMinecraft(String name) {
        return mc().containsKey(name);
    }

    public boolean isMod(String name) {
        return mod.containsKey(name);
    }

    /** Direct supertypes (super class first). */
    public List<String> supers(String name) {
        List<String> out = new ArrayList<>(3);
        ClassNode n = mod.get(name);
        if (n != null) {
            if (n.superName != null) out.add(n.superName);
            if (n.interfaces != null) out.addAll(n.interfaces);
            return out;
        }
        McClass c = mc().get(name);
        if (c != null) {
            if (c.superName() != null) out.add(c.superName());
            out.addAll(List.of(c.interfaces()));
            return out;
        }
        var p = peers.get(name);
        if (p != null) {
            if (p.superName != null) out.add(p.superName);
            out.addAll(p.interfaces);
        }
        String standIn = STAND_IN_SUPERS.get(name);
        if (standIn != null) out.add(standIn);
        return out;
    }

    /**
     * Stand-in classes of the migrate package that replace a Minecraft/NeoForge class under a new name, with the type whose call rules still apply to them
     * (the quad baking consumer is a {@code VertexConsumer}: {@code vertex(...)}, {@code uv(...)}, {@code endVertex()} are rewritten through it).
     */
    private static final Map<String, String> STAND_IN_SUPERS = Map.of(
            "org/kylepitcock/legacyconnector/v1_20_1/migrate/LegacyQuadBakingVertexConsumer", "com/mojang/blaze3d/vertex/VertexConsumer",
            "org/kylepitcock/legacyconnector/v1_20_1/migrate/LegacyQuadBakingVertexConsumer$Buffered", "org/kylepitcock/legacyconnector/v1_20_1/migrate/LegacyQuadBakingVertexConsumer");

    /** True when {@code name} is {@code ancestor} or inherits from it through any mix of mod and Minecraft classes. */
    public boolean isSubclassOf(String name, String ancestor) {
        return walk(name, ancestor, new HashSet<>()) == 1;
    }

    /** True when the chain above {@code name} leaves both the jar and Minecraft (a class of another mod): nothing can be said about it. */
    public boolean reachesUnknown(String name) {
        return walk(name, null, new HashSet<>()) == 2;
    }

    /** 1 = found the ancestor, 2 = ran into an unknown class, 0 = neither. */
    private int walk(String name, String ancestor, Set<String> seen) {
        if (name == null || !seen.add(name)) return 0;
        if (name.equals(ancestor)) return 1;
        if (name.equals("java/lang/Object")) return 0;
        if (!mod.containsKey(name) && !mc().containsKey(name) && !peers.containsKey(name)) return name.startsWith("java/") ? 0 : 2;
        int unknown = 0;
        for (String s : supers(name)) {
            int r = walk(s, ancestor, seen);
            if (r == 1) return 1;
            if (r == 2) unknown = 2;
        }
        return unknown;
    }

    /**
     * Who declares {@code name+desc} first when looking from {@code owner} upwards: a class of the mod, Minecraft's own tree (the method is not
     * in the mod, the rule table decides) or unknown. Walks only mod classes and stops at the first Minecraft class: whether Minecraft itself has
     * the method is the rule table's knowledge.
     */
    public Origin declaredIn(String owner, String name, String desc) {
        return declaredIn(owner, name, desc, new HashSet<>());
    }

    private Origin declaredIn(String owner, String name, String desc, Set<String> seen) {
        if (owner == null || !seen.add(owner)) return Origin.MINECRAFT;
        ClassNode n = mod.get(owner);
        if (n == null && peers.containsKey(owner) && !mc().containsKey(owner)) {
            // a class of another jar of the set: its own translation keeps the methods it declares as written, so a declaration counts like the mod's own
            if (peers.get(owner).methods.containsKey(name + desc)) return Origin.MOD;
            Origin result = Origin.MINECRAFT;
            for (String s : supers(owner)) {
                Origin o = declaredIn(s, name, desc, seen);
                if (o == Origin.MOD) return Origin.MOD;
                if (o == Origin.UNKNOWN) result = Origin.UNKNOWN;
            }
            return result;
        }
        if (n == null) return mc().containsKey(owner) ? Origin.MINECRAFT : (owner.startsWith("java/") ? Origin.MINECRAFT : Origin.UNKNOWN);
        for (MethodNode m : n.methods) if (m.name.equals(name) && m.desc.equals(desc) && (m.access & Opcodes.ACC_ABSTRACT) == 0) return Origin.MOD;
        Origin result = Origin.MINECRAFT;
        for (String s : supers(owner)) {
            Origin o = declaredIn(s, name, desc, seen);
            if (o == Origin.MOD) return Origin.MOD;
            if (o == Origin.UNKNOWN) result = Origin.UNKNOWN;
        }
        return result;
    }

    /**
     * A class writer that recomputes frames (and maxima) with this hierarchy: rewriting calls and constructors changes the stack at branch targets, so the
     * frames of a changed class cannot be kept. Unknown classes (other mods, libraries) fall back to the JVM's class loading, then to Object.
     */
    public org.objectweb.asm.ClassWriter frameWriter() {
        return new org.objectweb.asm.ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String a, String b) {
                return commonSuperClass(a, b, () -> super.getCommonSuperClass(a, b));
            }
        };
    }

    String commonSuperClass(String a, String b, java.util.function.Supplier<String> fallback) {
        if (a.equals(b)) return a;
        boolean known = (mod.containsKey(a) || mc().containsKey(a)) && (mod.containsKey(b) || mc().containsKey(b));
        if (!known) {
            try {
                return fallback.get();
            } catch (RuntimeException | LinkageError e) {
                return "java/lang/Object";
            }
        }
        if (isSubclassOf(b, a)) return a;
        if (isSubclassOf(a, b)) return b;
        String c = a;
        for (int guard = 0; guard < 64 && c != null; guard++) {
            ClassNode n = mod.get(c);
            String sup = n != null ? n.superName : (mc().containsKey(c) ? mc().get(c).superName() : null);
            if (sup == null) break;
            if (isSubclassOf(b, sup)) return sup;
            c = sup;
        }
        return "java/lang/Object";
    }

    /** The owner itself and its Minecraft ancestors, nearest first (rule lookup for calls through a subclass of the declaring class). */
    public List<String> minecraftAncestors(String owner) {
        List<String> out = new ArrayList<>();
        collect(owner, out, new HashSet<>());
        return out;
    }

    private void collect(String name, List<String> out, Set<String> seen) {
        if (name == null || !seen.add(name)) return;
        if (!mod.containsKey(name) && !peers.containsKey(name)) out.add(name);
        for (String s : supers(name)) collect(s, out, seen);
    }
}
