package org.kylepitcock.legacyconnector.loader.transform;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.*;

class OverrideBridgesTest {
    private static int[] from(String oldDesc, String newDesc) {
        OverrideBridges.Plan p = OverrideBridges.plan(oldDesc, newDesc, new String[1]);
        assertNotNull(p, oldDesc + " -> " + newDesc);
        return p.from();
    }

    @Test
    void addedTrailingArgumentsAreDropped() {
        // Item.onItemUse(stack, player, world, x, y, z, side) -> (..., side, hitX, hitY, hitZ)
        assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5, 6}, from("(La;Lb;Lc;IIII)Z", "(La;Lb;Lc;IIIIFFF)Z"));
    }

    @Test
    void aReferenceBecomesASuperTypeAndAnArgumentIsFoundByType() {
        // addCollidingBlockToList(world, x, y, z, box, ArrayList) -> (world, x, y, z, box, List, entity)
        assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5}, from("(La;IIILb;Ljava/util/ArrayList;)V", "(La;IIILb;Ljava/util/List;Lc;)V"));
        // Slot.onItemRemoved(stack) -> (player, stack)
        assertArrayEquals(new int[] {1}, from("(Lstack;)V", "(Lplayer;Lstack;)V"));
    }

    @Test
    void parametersTheNewSignatureLacksAreDefaulted() {
        OverrideBridges.Plan p = OverrideBridges.plan("(La;IIILb;)Z", "(La;III)Z", new String[1]);
        assertNotNull(p);
        assertEquals(1, p.defaults());
    }

    @Test
    void changedPrimitivesOrResultKindAreRefused() {
        String[] reason = new String[1];
        assertNull(OverrideBridges.plan("(I)Z", "(I)Ljava/lang/String;", reason));
        assertNotNull(reason[0]);
        assertNull(OverrideBridges.plan("(IIII)V", "(La;Lb;Lc;Ld;)V", reason));
    }

    @Test
    void emittedBridgeVerifiesAndPassesTheMatchedArguments() throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "T", null, "java/lang/Object", null);
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "hook", "(Ljava/lang/String;I)Z", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ILOAD, 2);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        String newDesc = "(Ljava/lang/Object;IFD)Z"; // object first, the int we want, floats added
        OverrideBridges.Plan plan = OverrideBridges.plan("(Ljava/lang/String;I)Z", newDesc, new String[1]);
        assertNotNull(plan);
        OverrideBridges.emit(cw, "T", "hook", "(Ljava/lang/String;I)Z", newDesc, plan);
        cw.visitEnd();
        byte[] bytes = cw.toByteArray();
        ClassLoader l = new ClassLoader() {
            @Override
            protected Class<?> findClass(String n) throws ClassNotFoundException {
                if (n.equals("T")) return defineClass("T", bytes, 0, bytes.length);
                throw new ClassNotFoundException(n);
            }
        };
        Class<?> t = l.loadClass("T");
        Object o = t.getDeclaredConstructor().newInstance();
        assertEquals(Boolean.TRUE, t.getMethod("hook", Object.class, int.class, float.class, double.class).invoke(o, "s", 7, 1f, 2d));
        assertEquals(Boolean.FALSE, t.getMethod("hook", Object.class, int.class, float.class, double.class).invoke(o, "s", 0, 1f, 2d));
    }
}
