package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

/** Stand-in superclass for coremod classes written against ASM 4/5 that subclass {@link ClassNode} (see {@link CompatMethodNode}). */
public class CompatClassNode extends ClassNode {
    public CompatClassNode() {
        super(Opcodes.ASM9);
    }
}
