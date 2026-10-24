package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * MOD-SPECIFIC FIX (Custom Main Menu), kept isolated so it is easy to remove: its {@code #mcversion#} token is
 * {@code Loader.MC_VERSION}, a compile-time constant that javac inlined as the literal "1.7.10", so no shim can change
 * what the main menu prints. The literal is replaced by the version actually running.
 */
public final class CustomMainMenuVersionFix {
    private static final String CLASS = "lumien/custommainmenu/lib/StringReplacer";
    private static final String RUNNING = "1.21.1";

    private CustomMainMenuVersionFix() {}

    public static int apply(ClassNode node) {
        if (!node.name.equals(CLASS)) return 0;
        int changed = 0;
        for (MethodNode m : node.methods) {
            for (AbstractInsnNode insn : m.instructions) {
                if (insn instanceof LdcInsnNode ldc && "1.7.10".equals(ldc.cst)) {
                    ldc.cst = RUNNING;
                    changed++;
                }
            }
        }
        return changed;
    }
}
