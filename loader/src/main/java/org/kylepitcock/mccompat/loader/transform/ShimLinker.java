package org.kylepitcock.mccompat.loader.transform;

import org.kylepitcock.mccompat.loader.shim.ShimIndex;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Post-relocation pass that (1) turns field accesses on shim classes into accessor calls where the shim
 * models the field "virtually", and (2) records every reference the shim cannot satisfy.
 *
 * <p>Virtual fields exist because legacy mods read Minecraft state straight from fields
 * ({@code entity.posX}, {@code world.isRemote}) while the shim objects are views over modern objects
 * whose state lives elsewhere. A shim exposes such a field by declaring
 * {@code static T mcc$get$name(Owner)} and optionally {@code static void mcc$set$name(Owner, T)}.</p>
 */
public final class ShimLinker {
    private ShimLinker() {}

    public static int apply(ClassNode node, Hierarchy hierarchy, LinkReport report) {
        int redirected = 0;
        for (MethodNode method : node.methods) {
            InsnList insns = method.instructions;
            for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof FieldInsnNode f) {
                    AbstractInsnNode replacement = virtualField(f, hierarchy);
                    if (replacement != null) {
                        insns.set(f, replacement);
                        insn = replacement;
                        redirected++;
                        check(hierarchy, report, node.name, (MethodInsnNode) replacement);
                    } else if (isShimNamespace(f.owner) || hierarchy.isLocal(f.owner)) {
                        // Local owners matter too: `this.blockHardness` in a mod block resolves into the shim.
                        Hierarchy.Lookup l = hierarchy.resolveField(f.owner, f.name, f.desc);
                        record(report, l, node.name, f.owner, f.name + ":" + f.desc, true);
                    } else {
                        checkLibrary(hierarchy, report, node.name, f.owner, f.name + ":" + f.desc, hierarchy.resolveField(f.owner, f.name, f.desc));
                    }
                } else if (insn instanceof MethodInsnNode m) {
                    check(hierarchy, report, node.name, m);
                } else if (insn instanceof TypeInsnNode t) {
                    String type = t.desc.startsWith("[") ? Type.getType(t.desc).getElementType().getInternalName() : t.desc;
                    if (isShimNamespace(type) && !hierarchy.isShim(type)) report.missingClass(type, node.name);
                }
            }
        }
        // Supertypes are the most important links: a missing shim superclass fails at class load.
        if (node.superName != null && isShimNamespace(node.superName) && !hierarchy.isShim(node.superName)) {
            report.missingClass(node.superName, node.name);
        }
        for (String iface : node.interfaces) {
            if (isShimNamespace(iface) && !hierarchy.isShim(iface)) report.missingClass(iface, node.name);
        }
        return redirected;
    }

    private static AbstractInsnNode virtualField(FieldInsnNode f, Hierarchy hierarchy) {
        boolean isStatic = f.getOpcode() == Opcodes.GETSTATIC || f.getOpcode() == Opcodes.PUTSTATIC;
        boolean isGet = f.getOpcode() == Opcodes.GETSTATIC || f.getOpcode() == Opcodes.GETFIELD;
        if (hierarchy.isLocal(f.owner) && hierarchy.find(f.owner).hasField(f.name, f.desc)) return null;
        String declaring = hierarchy.findVirtualFieldOwner(f.owner, f.name, f.desc, isStatic);
        if (declaring == null) return null;
        String self = isStatic ? "" : "L" + declaring + ";";
        if (isGet) {
            return new MethodInsnNode(Opcodes.INVOKESTATIC, declaring, ShimIndex.GETTER_PREFIX + f.name, "(" + self + ")" + f.desc, false);
        }
        return new MethodInsnNode(Opcodes.INVOKESTATIC, declaring, ShimIndex.SETTER_PREFIX + f.name, "(" + self + f.desc + ")V", false);
    }

    private static void check(Hierarchy hierarchy, LinkReport report, String from, MethodInsnNode m) {
        if (m.owner.startsWith("[")) return; // array clone() etc.
        if (isShimNamespace(m.owner) || hierarchy.isLocal(m.owner)) {
            record(report, hierarchy.resolveMethod(m.owner, m.name, m.desc), from, m.owner, m.name + m.desc, true);
        } else {
            checkLibrary(hierarchy, report, from, m.owner, m.name + m.desc, hierarchy.resolveMethod(m.owner, m.name, m.desc));
        }
    }

    private static void checkLibrary(Hierarchy hierarchy, LinkReport report, String from, String owner, String member, Hierarchy.Lookup l) {
        if (l == null) return;
        if (l == Hierarchy.Lookup.MISSING_CLASS) {
            report.externalClass(owner, from);
        } else if (l == Hierarchy.Lookup.MISSING_MEMBER) {
            report.missingMember(owner, member, from, false);
        }
    }

    private static void record(LinkReport report, Hierarchy.Lookup l, String from, String owner, String member, boolean shim) {
        switch (l) {
            case MISSING_CLASS -> report.missingClass(owner, from);
            case MISSING_MEMBER -> report.missingMember(owner, member, from, shim);
            default -> { }
        }
    }

    static boolean isShimNamespace(String internalName) {
        return internalName.startsWith("mccompat/");
    }
}
