package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class Subscribers1201Test implements Opcodes {
    private static ClassNode cls(String name, String sup) {
        ClassNode n = new ClassNode();
        n.version = V17;
        n.access = ACC_PUBLIC;
        n.name = name;
        n.superName = sup;
        return n;
    }

    private static void annotate(ClassNode n, String desc) {
        if (n.visibleAnnotations == null) n.visibleAnnotations = new java.util.ArrayList<>();
        n.visibleAnnotations.add(new AnnotationNode(desc));
    }

    @Test
    void classWithInstanceHandlerIsRegisteredByTheMainClass() {
        ClassNode main = cls("mod/Main", "java/lang/Object");
        annotate(main, "Lnet/neoforged/fml/common/Mod;");
        MethodNode init = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitVarInsn(ALOAD, 0);
        init.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(RETURN);
        main.methods.add(init);

        ClassNode proxy = cls("mod/Proxy", "java/lang/Object");
        annotate(proxy, "Lnet/neoforged/fml/common/EventBusSubscriber;");
        MethodNode handler = new MethodNode(ACC_PUBLIC, "onTick", "(Ljava/lang/Object;)V", null, null);
        handler.visibleAnnotations = new java.util.ArrayList<>(List.of(new AnnotationNode("Lnet/neoforged/bus/api/SubscribeEvent;")));
        handler.visitInsn(RETURN);
        proxy.methods.add(handler);

        ClassNode child = cls("mod/ClientProxy", "mod/Proxy");
        annotate(child, "Lnet/neoforged/fml/common/EventBusSubscriber;");

        assertEquals(2, Subscribers1201.apply(List.of(main, proxy, child)));
        assertTrue(proxy.visibleAnnotations.isEmpty());
        assertTrue(child.visibleAnnotations.isEmpty());
        int calls = 0;
        for (AbstractInsnNode in : init.instructions) if (in instanceof MethodInsnNode mi && mi.name.equals("subscribeStatics")) calls++;
        assertEquals(2, calls);
    }

    @Test
    void staticOnlySubscribersAreLeftToTheLoader() {
        ClassNode main = cls("mod/Main", "java/lang/Object");
        annotate(main, "Lnet/neoforged/fml/common/Mod;");
        ClassNode events = cls("mod/Events", "java/lang/Object");
        annotate(events, "Lnet/neoforged/fml/common/EventBusSubscriber;");
        MethodNode handler = new MethodNode(ACC_PUBLIC | ACC_STATIC, "onTick", "(Ljava/lang/Object;)V", null, null);
        handler.visibleAnnotations = new java.util.ArrayList<>(List.of(new AnnotationNode("Lnet/neoforged/bus/api/SubscribeEvent;")));
        handler.visitInsn(RETURN);
        events.methods.add(handler);
        assertEquals(0, Subscribers1201.apply(List.of(main, events)));
        assertFalse(events.visibleAnnotations.isEmpty());
    }
}
