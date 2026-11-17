package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class ClassShape1201Test implements Opcodes {
    private static ClassNode implementing(String superName) {
        ClassNode n = new ClassNode();
        n.version = V21;
        n.access = ACC_PUBLIC;
        n.name = "mod/MySource";
        n.superName = superName;
        n.interfaces = new ArrayList<>(java.util.List.of(ClassShape1201.BLOCK_SOURCE));
        MethodNode init = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitVarInsn(ALOAD, 0);
        init.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(RETURN);
        n.methods.add(init);
        MethodNode getPos = new MethodNode(ACC_PUBLIC, "getPos", "()Lnet/minecraft/core/BlockPos;", null, null);
        getPos.visitInsn(ACONST_NULL);
        getPos.visitInsn(ARETURN);
        n.methods.add(getPos);
        return n;
    }

    @Test
    void blockSourceImplementorBecomesASubclassOfTheRecord() {
        ClassNode n = implementing("java/lang/Object");
        ClassShape1201 shape = new ClassShape1201();
        assertEquals(1, shape.apply(n));
        assertEquals(ClassShape1201.BLOCK_SOURCE, n.superName);
        assertFalse(n.interfaces.contains(ClassShape1201.BLOCK_SOURCE));
        assertTrue(n.interfaces.contains(ClassShape1201.LEGACY_BLOCK_SOURCE));
        assertTrue(shape.opened.contains("net.minecraft.core.dispenser.BlockSource"));
        MethodNode init = n.methods.stream().filter(m -> m.name.equals("<init>")).findFirst().orElseThrow();
        MethodInsnNode call = null;
        for (var in = init.instructions.getFirst(); in != null; in = in.getNext()) if (in instanceof MethodInsnNode mi) call = mi;
        assertEquals(ClassShape1201.BLOCK_SOURCE, call.owner);
        assertTrue(call.desc.startsWith("(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;"));
        for (String accessor : new String[] {"level", "pos", "state", "blockEntity", "center"}) {
            assertTrue(n.methods.stream().anyMatch(m -> m.name.equals(accessor) && m.desc.startsWith("()")), accessor);
        }
    }

    @Test
    void anImplementorWithAnotherSuperclassIsReportedNotConverted() {
        ClassNode n = implementing("mod/Base");
        ClassShape1201 shape = new ClassShape1201();
        assertEquals(0, shape.apply(n));
        assertEquals("mod/Base", n.superName);
        assertTrue(shape.unconverted.contains("mod.MySource"));
    }

    @Test
    void extendingAClosedGameClassOpensIt() {
        ClassNode n = new ClassNode();
        n.version = V21;
        n.access = ACC_PUBLIC;
        n.name = "mod/MyDecoration";
        n.superName = "net/minecraft/world/level/saveddata/maps/MapDecoration";
        ClassShape1201 shape = new ClassShape1201();
        shape.apply(n);
        assertTrue(shape.opened.contains("net.minecraft.world.level.saveddata.maps.MapDecoration"));
    }
}
