package org.kylepitcock.mccompat.loader.transform;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/**
 * Stand-in for {@link ClassWriter} in coremod classes written against ASM 5: they build methods on a writer that has not
 * been given a class yet and emit compressed frames, which ASM 9 rejects until a class version is known (it treats an
 * unset version as 1.0). The writer starts out as a modern class; the real {@code visit} replaces that.
 */
public class CompatClassWriter extends ClassWriter {
    public CompatClassWriter(int flags) {
        super(flags);
        visit(Opcodes.V1_8, 0, "mccompat/Placeholder", null, "java/lang/Object", null);
    }

    public CompatClassWriter(ClassReader reader, int flags) {
        super(reader, flags);
    }
}
