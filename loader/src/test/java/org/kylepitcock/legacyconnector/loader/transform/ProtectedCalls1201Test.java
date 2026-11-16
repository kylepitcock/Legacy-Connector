package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

class ProtectedCalls1201Test {
    private static final String BLOCK = "net/minecraft/world/level/block/Block";
    private static final String ON_REMOVE_DESC =
            "(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V";

    private static ClassNode caller(String name, String superName, String receiver) {
        ClassNode n = new ClassNode();
        n.version = Opcodes.V17;
        n.name = name;
        n.superName = superName;
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, "poke", "(L" + receiver + ";)V", null, null);
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        m.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        m.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        m.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        m.instructions.add(new InsnNode(Opcodes.ICONST_0));
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, receiver, "onRemove", ON_REMOVE_DESC, false));
        m.instructions.add(new InsnNode(Opcodes.RETURN));
        n.methods.add(m);
        return n;
    }

    private static List<String> calls(ClassNode n) {
        List<String> out = new ArrayList<>();
        for (AbstractInsnNode in : n.methods.get(0).instructions) if (in instanceof MethodInsnNode mi) out.add(mi.owner + "." + mi.name);
        return out;
    }

    @Test
    void protectedCallOnAnotherBlockGoesThroughTheHelper() {
        ClassNode n = caller("mod/Gong", BLOCK, BLOCK);
        assertEquals(1, ProtectedCalls1201.apply(n, new Mc1201Hierarchy().add(n)));
        // the boolean argument is boxed into the argument array
        assertEquals(List.of("java/lang/Boolean.valueOf", "org/kylepitcock/legacyconnector/v1_20_1/migrate/ProtectedAccess1201.invoke"), calls(n));
    }

    @Test
    void protectedCallOnTheOwnClassStaysLegal() {
        ClassNode n = caller("mod/Gong", BLOCK, "mod/Gong");
        assertEquals(0, ProtectedCalls1201.apply(n, new Mc1201Hierarchy().add(n)));
        assertEquals(List.of("mod/Gong.onRemove"), calls(n));
    }

    @Test
    void publicMethodsAreLeftAlone() {
        ClassNode n = caller("mod/Other", "java/lang/Object", BLOCK);
        MethodInsnNode call = (MethodInsnNode) n.methods.get(0).instructions.getLast().getPrevious();
        call.name = "defaultBlockState";
        call.desc = "()Lnet/minecraft/world/level/block/state/BlockState;";
        assertEquals(0, ProtectedCalls1201.apply(n, new Mc1201Hierarchy().add(n)));
    }
}
