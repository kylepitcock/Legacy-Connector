package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

/**
 * Stand-in superclass for coremod classes written against ASM 4/5 that subclass {@link MethodNode} anonymously ({@code new MethodNode(access, name, desc,
 * sig, exceptions) { ... }}): ASM 6+ rejects the constructors without an API level for a subclass ({@code IllegalStateException}). See {@link AsmNodeCompat}.
 */
public class CompatMethodNode extends MethodNode {
    public CompatMethodNode() {
        super(Opcodes.ASM9);
    }

    public CompatMethodNode(int access, String name, String descriptor, String signature, String[] exceptions) {
        super(Opcodes.ASM9, access, name, descriptor, signature, exceptions);
    }
}
