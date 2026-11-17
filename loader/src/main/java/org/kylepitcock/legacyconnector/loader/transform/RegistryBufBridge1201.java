package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Network buffer parameters of library callbacks. Since Minecraft 1.20.5 play-phase code is handed a {@code RegistryFriendlyByteBuf} (a subclass of
 * {@code FriendlyByteBuf}), and the 1.21.1 builds of libraries declare their abstract methods and functional interfaces with it: Architectury's
 * {@code Message.write(RegistryFriendlyByteBuf)}, its {@code MessageDecoder}, codec helpers of other libraries. A 1.20.1 mod still implements
 * {@code write(FriendlyByteBuf)} or passes {@code MyPacket::new} taking a {@code FriendlyByteBuf}: the class loads, and the first packet dies with an
 * {@code AbstractMethodError} (FTB Library when a player joins). The library is a native 1.21.1 jar, so no rule table knows its methods.
 *
 * <p>Two additions, both harmless when nothing calls them:
 * <ul>
 *   <li>a mod class whose supertypes leave the mod and Minecraft (a library) gets, for each overridable method with {@code FriendlyByteBuf} parameters,
 *       an overload taking {@code RegistryFriendlyByteBuf} there that calls the mod's method;</li>
 *   <li>a lambda or method reference for a functional interface of a native library whose method takes {@code RegistryFriendlyByteBuf} today is
 *       retyped to that method ({@link NativeClasses1201} reads what the library declares).</li>
 * </ul>
 */
final class RegistryBufBridge1201 {
    private RegistryBufBridge1201() {}

    private static final String BUF = "Lnet/minecraft/network/FriendlyByteBuf;";
    private static final String REGISTRY_BUF = "Lnet/minecraft/network/RegistryFriendlyByteBuf;";
    private static final String LMF = "java/lang/invoke/LambdaMetafactory";

    /** @return the number of bridge methods and lambda bridges added */
    static int apply(ClassNode node, Mc1201Hierarchy hierarchy) {
        int n = lambdas(node, hierarchy);
        if (hierarchy != null && hierarchy.reachesUnknown(node.name)) n += methods(node);
        return n;
    }

    /** The descriptor with every FriendlyByteBuf PARAMETER replaced, or null when it has none. */
    static String widened(String desc) {
        int end = desc.indexOf(')');
        String params = desc.substring(0, end);
        if (!params.contains(BUF)) return null;
        return params.replace(BUF, REGISTRY_BUF) + desc.substring(end);
    }

    private static int methods(ClassNode node) {
        boolean itf = (node.access & Opcodes.ACC_INTERFACE) != 0;
        List<MethodNode> added = new ArrayList<>();
        for (MethodNode m : node.methods) {
            if (m.name.startsWith("<") || (m.access & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE)) != 0) continue;
            String desc = widened(m.desc);
            if (desc == null || has(node, m.name, desc) || has(added, m.name, desc)) continue;
            int access = (m.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE;
            MethodNode b = new MethodNode(access, m.name, desc, null, m.exceptions == null ? null : m.exceptions.toArray(new String[0]));
            b.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            int slot = 1;
            for (Type t : Type.getArgumentTypes(m.desc)) {
                b.instructions.add(new VarInsnNode(t.getOpcode(Opcodes.ILOAD), slot));
                slot += t.getSize();
            }
            b.instructions.add(new MethodInsnNode(itf ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, node.name, m.name, m.desc, itf));
            b.instructions.add(new InsnNode(Type.getReturnType(m.desc).getOpcode(Opcodes.IRETURN)));
            b.maxStack = slot + 1;
            b.maxLocals = slot;
            added.add(b);
        }
        node.methods.addAll(added);
        return added.size();
    }

    private static boolean has(ClassNode node, String name, String desc) {
        return has(node.methods, name, desc);
    }

    private static boolean has(List<MethodNode> methods, String name, String desc) {
        for (MethodNode m : methods) if (m.name.equals(name) && m.desc.equals(desc)) return true;
        return false;
    }

    /**
     * A lambda or method reference for a functional interface of a NATIVE library whose single method now takes RegistryFriendlyByteBuf: the call site is
     * retyped to that method (the implementation still takes a FriendlyByteBuf, which accepts it). Interfaces of the mod, of other translated jars and of
     * Minecraft keep the method the mod was compiled against; so does a library interface that still declares it.
     */
    private static int lambdas(ClassNode node, Mc1201Hierarchy hierarchy) {
        int n = 0;
        for (MethodNode m : node.methods) {
            for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof InvokeDynamicInsnNode indy)) continue;
                Handle bsm = indy.bsm;
                if (!LMF.equals(bsm.getOwner()) || !"metafactory".equals(bsm.getName()) || indy.bsmArgs.length != 3) continue;
                if (!(indy.bsmArgs[0] instanceof Type sam) || sam.getSort() != Type.METHOD || !(indy.bsmArgs[2] instanceof Type instantiated)) continue;
                String widened = widened(sam.getDescriptor());
                if (widened == null) continue;
                Type itf = Type.getReturnType(indy.desc);
                if (itf.getSort() != Type.OBJECT || (hierarchy != null && hierarchy.knows(itf.getInternalName()))) continue;
                java.util.Set<String> declared = NativeClasses1201.methods(itf.getInternalName());
                if (declared == null || declared.contains(indy.name + sam.getDescriptor()) || !declared.contains(indy.name + widened)) continue;
                String widenedInstantiated = widened(instantiated.getDescriptor());
                indy.bsmArgs = new Object[] {Type.getMethodType(widened), indy.bsmArgs[1],
                        widenedInstantiated == null ? instantiated : Type.getMethodType(widenedInstantiated)};
                n++;
            }
        }
        return n;
    }
}
