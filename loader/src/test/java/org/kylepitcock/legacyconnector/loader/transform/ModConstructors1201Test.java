package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

class ModConstructors1201Test implements Opcodes {
    private static final String CONTEXT = "Llegacyconnector/v1_20_1/net/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext;";

    private static ClassNode mod(String ctorDesc) {
        ClassNode n = new ClassNode();
        n.version = V17;
        n.access = ACC_PUBLIC;
        n.name = "mod/Main";
        n.superName = "java/lang/Object";
        n.visibleAnnotations = new java.util.ArrayList<>(List.of(new AnnotationNode("Lnet/neoforged/fml/common/Mod;")));
        MethodNode init = new MethodNode(ACC_PUBLIC, "<init>", ctorDesc, null, null);
        init.visitVarInsn(ALOAD, 0);
        init.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(RETURN);
        n.methods.add(init);
        return n;
    }

    @Test
    void contextConstructorGetsAPublicNeoForgeConstructor() {
        ClassNode n = mod("(" + CONTEXT + ")V");
        assertEquals(1, ModConstructors1201.apply(List.of(n)));
        long publicCtors = n.methods.stream().filter(m -> m.name.equals("<init>") && (m.access & ACC_PUBLIC) != 0).count();
        assertEquals(1, publicCtors);
        assertTrue(n.methods.stream().anyMatch(m -> m.desc.equals("(Lnet/neoforged/bus/api/IEventBus;Lnet/neoforged/fml/ModContainer;)V") && (m.access & ACC_PUBLIC) != 0));
    }

    @Test
    void plainConstructorsAreUntouched() {
        ClassNode n = mod("(Lnet/neoforged/bus/api/IEventBus;)V");
        assertEquals(0, ModConstructors1201.apply(List.of(n)));
        assertEquals(1, n.methods.size());
    }
}
