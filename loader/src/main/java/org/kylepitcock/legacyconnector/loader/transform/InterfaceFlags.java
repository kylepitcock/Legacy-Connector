package org.kylepitcock.legacyconnector.loader.transform;

import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Makes the "owner is an interface" flag of every method call and method handle agree with the owner class.
 * Java 8's JVM tolerated a plain method reference to an interface's method (some toolchains, such as Retrolambda or
 * old ECJ, emit them, FiskHeroes' lambdas in interface default methods among them); Java 9 and later reject the
 * class with "Inconsistent constant pool data ... is CONSTANT_MethodRef and should be CONSTANT_InterfaceMethodRef".
 */
public final class InterfaceFlags {
    private InterfaceFlags() {}

    /** @return the number of call sites and handles that were corrected */
    public static int apply(ClassNode node, Hierarchy h) {
        int fixed = 0;
        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode m) {
                    if (m.owner.startsWith("[")) continue;
                    Boolean itf = interfaceOwner(m.owner, h);
                    if (m.getOpcode() == Opcodes.INVOKEINTERFACE) itf = Boolean.TRUE;
                    if (itf != null && m.itf != itf) {
                        m.itf = itf;
                        fixed++;
                    }
                } else if (insn instanceof InvokeDynamicInsnNode d) {
                    Handle bsm = fix(d.bsm, h);
                    if (bsm != d.bsm) {
                        d.bsm = bsm;
                        fixed++;
                    }
                    for (int i = 0; i < d.bsmArgs.length; i++) {
                        if (d.bsmArgs[i] instanceof Handle handle) {
                            Handle f = fix(handle, h);
                            if (f != handle) {
                                d.bsmArgs[i] = f;
                                fixed++;
                            }
                        }
                    }
                }
            }
        }
        return fixed;
    }

    private static Handle fix(Handle handle, Hierarchy h) {
        if (handle.getTag() < Opcodes.H_INVOKEVIRTUAL) return handle; // field handles
        Boolean itf = handle.getTag() == Opcodes.H_INVOKEINTERFACE ? Boolean.TRUE : interfaceOwner(handle.getOwner(), h);
        if (itf == null || itf == handle.isInterface()) return handle;
        return new Handle(handle.getTag(), handle.getOwner(), handle.getName(), handle.getDesc(), itf);
    }

    /** Null when the class is not known (an absent dependency): its call sites stay as they are. */
    private static Boolean interfaceOwner(String owner, Hierarchy h) {
        ClassInfo info = h.find(owner);
        if (info == null) return null;
        return (info.access & Opcodes.ACC_INTERFACE) != 0;
    }
}
