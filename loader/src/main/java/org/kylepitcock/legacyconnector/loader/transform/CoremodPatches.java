package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * MOD-SPECIFIC (keep minimal and isolated; remove when a generic fix exists): patches of individual coremod classes that
 * cannot work as written.
 *
 * <ul>
 *   <li><b>Galacticraft / Micdoodle8 core</b> ({@code micdoodle8/mods/miccore/DepLoader}): its coremod plugin downloads
 *   {@code commons-codec-1.9.jar} from {@code central.maven.org}, a host that no longer exists, and aborts the plugin when that
 *   fails. Forge only ever had it succeed because the library was already on the class path (it is part of the game), so the
 *   download step is skipped and the plugin goes on to register its class transformers (gravity, sound and oxygen hooks).</li>
 * </ul>
 */
final class CoremodPatches {
    private CoremodPatches() {}

    static byte[] apply(byte[] bytes) {
        try {
            ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, 0);
            // the static entry point and the inner worker both reach the download
            if (!node.name.equals("micdoodle8/mods/miccore/DepLoader") && !node.name.equals("micdoodle8/mods/miccore/DepLoader$DepLoadInst")) return bytes;
            boolean patched = false;
            for (MethodNode m : node.methods) {
                if (m.name.equals("load") && m.desc.equals("()V")) {
                    m.instructions = new InsnList();
                    m.instructions.add(new InsnNode(Opcodes.RETURN));
                    m.tryCatchBlocks.clear();
                    m.localVariables = null;
                    m.maxStack = 0;
                    m.maxLocals = 0;
                    patched = true;
                }
            }
            if (!patched) return bytes;
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            return writer.toByteArray();
        } catch (RuntimeException e) {
            return bytes;
        }
    }
}
