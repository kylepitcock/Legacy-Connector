package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * FML's SideTransformer: fields and methods annotated {@code @SideOnly(Side.CLIENT)} do not exist on a dedicated
 * server (and {@code SERVER} ones not on the client). Mods relied on it, e.g. by declaring client-only method
 * signatures in common classes; keeping them would make verification load client classes on the server.
 *
 * <p>Lambdas and method references compile to private synthetic {@code lambda$...} methods; when the only user of
 * one was a removed method, its body (which may reference client-only classes) would stay behind, so those orphans are
 * removed too (iterated to a fixed point, never when anything remaining still refers to them). The idea follows
 * Sinytra Connector's {@code EnvironmentStripperTransformer} (MIT, see THIRD_PARTY_NOTICES.md); the code is our own.
 */
public final class SideOnlyStripper {
    private SideOnlyStripper() {}

    /**
     * Creative-listing methods ({@code Block.getSubBlocks}, {@code Item.getSubItems}; srg and mapped names) are annotated client-only in
     * Forge, but the layer asks them on both sides: they decide which subtypes (and so which registry keys) a legacy item has, and a
     * dedicated server without them registers other keys than its clients (the join fails with "registries with unknown keys").
     */
    private static boolean listsSubtypes(MethodNode m) {
        return switch (m.name) {
            case "getSubBlocks", "getSubItems", "func_149666_a", "func_150895_a" -> true;
            default -> false;
        };
    }

    /** Orphaned synthetic lambda bodies removed so far (statistics, read by diagnostics and tests). */
    public static final AtomicInteger LAMBDAS_REMOVED = new AtomicInteger();

    /** @param side "CLIENT" or "SERVER": the side we are running on */
    public static int apply(ClassNode node, String shimPrefix, String side) {
        String desc = "L" + shimPrefix + "cpw/mods/fml/relauncher/SideOnly;";
        int before = node.fields.size() + node.methods.size();
        node.fields.removeIf(f -> otherSide(f.visibleAnnotations, desc, side) || otherSide(f.invisibleAnnotations, desc, side));
        List<MethodNode> removed = new ArrayList<>();
        node.methods.removeIf(m -> {
            boolean r = !listsSubtypes(m) && (otherSide(m.visibleAnnotations, desc, side) || otherSide(m.invisibleAnnotations, desc, side));
            if (r) removed.add(m);
            return r;
        });
        if (!removed.isEmpty()) LAMBDAS_REMOVED.addAndGet(removeOrphanLambdas(node, removed));
        return before - node.fields.size() - node.methods.size();
    }

    private static boolean isLambdaBody(MethodNode m) {
        return m.name.startsWith("lambda$") && (m.access & Opcodes.ACC_SYNTHETIC) != 0 && (m.access & Opcodes.ACC_PRIVATE) != 0;
    }

    /** Removes the synthetic lambda methods only reachable from {@code removed}; returns how many. */
    private static int removeOrphanLambdas(ClassNode node, List<MethodNode> removed) {
        // Candidates: lambda bodies referenced by removed methods, plus lambdas only those reference (fixed point).
        Set<String> candidates = new HashSet<>();
        List<MethodNode> work = new ArrayList<>(removed);
        while (!work.isEmpty()) {
            MethodNode m = work.remove(work.size() - 1);
            for (String ref : references(node, m)) {
                if (candidates.contains(ref)) continue;
                MethodNode target = find(node, ref);
                if (target != null && isLambdaBody(target)) {
                    candidates.add(ref);
                    work.add(target);
                }
            }
        }
        if (candidates.isEmpty()) return 0;
        // Anything reachable from a method that stays keeps its lambdas alive (transitively).
        List<MethodNode> live = new ArrayList<>();
        for (MethodNode m : node.methods) if (!candidates.contains(m.name + m.desc)) live.add(m);
        while (!live.isEmpty()) {
            MethodNode m = live.remove(live.size() - 1);
            for (String ref : references(node, m)) {
                if (!candidates.remove(ref)) continue;
                live.add(find(node, ref));
            }
        }
        if (candidates.isEmpty()) return 0;
        node.methods.removeIf(m -> candidates.contains(m.name + m.desc));
        return candidates.size();
    }

    private static MethodNode find(ClassNode node, String nameDesc) {
        for (MethodNode m : node.methods) if (nameDesc.equals(m.name + m.desc)) return m;
        return null;
    }

    /** name+desc of every method of this class that {@code m} may use: indy/ldc handles, direct calls, name strings. */
    private static Set<String> references(ClassNode node, MethodNode m) {
        Set<String> refs = new HashSet<>();
        if (m.instructions == null) return refs;
        for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof InvokeDynamicInsnNode indy) {
                collect(node, indy.bsm, refs);
                for (Object arg : indy.bsmArgs) collect(node, arg, refs);
            } else if (insn instanceof MethodInsnNode call) {
                if (call.owner.equals(node.name)) refs.add(call.name + call.desc);
            } else if (insn instanceof LdcInsnNode ldc) {
                // Names as strings count too ($deserializeLambda$ compares implementation method names).
                if (ldc.cst instanceof String str && str.startsWith("lambda$")) {
                    for (MethodNode other : node.methods) if (other.name.equals(str)) refs.add(other.name + other.desc);
                } else {
                    collect(node, ldc.cst, refs);
                }
            }
        }
        return refs;
    }

    private static void collect(ClassNode node, Object cst, Set<String> refs) {
        if (cst instanceof Handle h) {
            if (h.getOwner().equals(node.name)) refs.add(h.getName() + h.getDesc());
        } else if (cst instanceof ConstantDynamic cd) {
            collect(node, cd.getBootstrapMethod(), refs);
            for (int i = 0; i < cd.getBootstrapMethodArgumentCount(); i++) collect(node, cd.getBootstrapMethodArgument(i), refs);
        }
    }

    private static boolean otherSide(List<AnnotationNode> annotations, String desc, String side) {
        if (annotations == null) return false;
        for (AnnotationNode a : annotations) {
            if (!a.desc.equals(desc) || a.values == null) continue;
            for (int i = 0; i + 1 < a.values.size(); i += 2) {
                if ("value".equals(a.values.get(i)) && a.values.get(i + 1) instanceof String[] e && e.length == 2 && !e[1].equals(side)) return true;
            }
        }
        return false;
    }
}
