package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** AM2 treats a file: code source as a class file on disk, even when it points to the translated JAR. */
final class Am2SpellIconFix {
    private Am2SpellIconFix() {}

    static void apply(ClassNode node) {
        if (!node.name.equals("am2/spell/SpellTextureHelper")) return;
        for (var method : node.methods) {
            if (!method.name.equals("getResourceListing") || !method.desc.equals("()Ljava/util/List;")) continue;
            method.instructions.clear();
            method.tryCatchBlocks.clear();
            method.localVariables = null;
            method.instructions.add(new LdcInsnNode(Type.getObjectType(node.name)));
            method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                    "org/kylepitcock/legacyconnector/compat/jdk/ResourceCompat",
                    "listAm2SpellIcons", "(Ljava/lang/Class;)Ljava/util/List;", false));
            method.instructions.add(new InsnNode(Opcodes.ARETURN));
            method.maxStack = 1;
            method.maxLocals = 0;
        }
    }
}
