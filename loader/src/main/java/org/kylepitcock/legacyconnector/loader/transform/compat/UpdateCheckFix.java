package org.kylepitcock.legacyconnector.loader.transform.compat;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * MOD-SPECIFIC FIX (bspkrs mods: bspkrsCore, TreeCapitator, ...), kept isolated so it is easy to remove. Their update checker downloads a version
 * file in the constructor of {@code ModVersionChecker} and reads line 0 of the answer; the hosts it names are gone or answer with an empty
 * redirect, so the answer has no line and the mod fails with an {@code ArrayIndexOutOfBoundsException} instead of reporting "check_error" as it
 * does for a failed download. {@code CommonUtils.loadTextFromURL} returns the caller's default for an empty answer.
 */
public final class UpdateCheckFix {
    private static final String CLASS = "bspkrs/util/CommonUtils";
    private static final String NAME = "loadTextFromURL";
    private static final String DESC = "(Ljava/net/URL;Ljava/util/logging/Logger;[Ljava/lang/String;I)[Ljava/lang/String;";
    private static final String HELPER = "org/kylepitcock/legacyconnector/compat/jdk/JdkCompat";

    private UpdateCheckFix() {}

    public static int apply(ClassNode node) {
        if (!node.name.equals(CLASS)) return 0;
        for (MethodNode m : node.methods) {
            if (!m.name.equals(NAME) || !m.desc.equals(DESC) || (m.access & Opcodes.ACC_STATIC) == 0) continue;
            MethodNode wrapper = new MethodNode(m.access, NAME, DESC, m.signature, m.exceptions == null ? null : m.exceptions.toArray(String[]::new));
            m.name = "mcc$" + NAME;
            m.access = (m.access & ~(Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PRIVATE;
            wrapper.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            wrapper.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            wrapper.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
            wrapper.instructions.add(new VarInsnNode(Opcodes.ILOAD, 3));
            wrapper.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, node.name, m.name, DESC, false));
            wrapper.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
            wrapper.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, "nonEmpty", "([Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;", false));
            wrapper.instructions.add(new InsnNode(Opcodes.ARETURN));
            wrapper.maxStack = 4;
            wrapper.maxLocals = 4;
            node.methods.add(wrapper);
            return 1;
        }
        return 0;
    }
}
