package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Coremods compiled against ASM 4/5 subclass the tree nodes anonymously ({@code new MethodNode(...) { ... }}). ASM 6+ refuses the constructors without an API
 * level for a subclass ({@code IllegalStateException}), which broke Logistics Pipes' transformer on every class it patches. A direct subclass of
 * {@code MethodNode} or {@code ClassNode} gets {@link CompatMethodNode} / {@link CompatClassNode} as its superclass instead (they pass the current API level).
 */
final class AsmNodeCompat {
    private static final String METHOD_NODE = "org/objectweb/asm/tree/MethodNode";
    private static final String CLASS_NODE = "org/objectweb/asm/tree/ClassNode";
    private static final String COMPAT_PACKAGE = "org/kylepitcock/legacyconnector/loader/transform/";

    private AsmNodeCompat() {}

    /** The class with its tree-node superclass replaced, or {@code bytes} itself when it has none. */
    static byte[] apply(byte[] bytes) {
        try {
            String text = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
            if (!text.contains("org/objectweb/asm/tree/")) return bytes;
            ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, 0);
            if (!METHOD_NODE.equals(node.superName) && !CLASS_NODE.equals(node.superName)) return bytes;
            String original = node.superName;
            String compat = COMPAT_PACKAGE + (original.equals(METHOD_NODE) ? "CompatMethodNode" : "CompatClassNode");
            // only the constructors that have no API level (the ones ASM 6+ refuses for a subclass); a subclass that passes its own level is fine as it is
            java.util.List<MethodInsnNode> old = new java.util.ArrayList<>();
            int all = 0;
            for (MethodNode m : node.methods) {
                if (!m.name.equals("<init>")) continue;
                for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (!(insn instanceof MethodInsnNode call) || call.getOpcode() != Opcodes.INVOKESPECIAL || !call.owner.equals(original) || !call.name.equals("<init>")) continue;
                    all++;
                    if (call.desc.equals("()V") || original.equals(METHOD_NODE) && call.desc.equals("(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)V")) old.add(call);
                }
            }
            if (old.isEmpty() || old.size() != all) return bytes; // a mix cannot move to another superclass
            node.superName = compat;
            for (MethodInsnNode call : old) call.owner = compat;
            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            return writer.toByteArray();
        } catch (RuntimeException e) {
            return bytes;
        }
    }
}
