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

    private static int applyWithStaticHandler(String eventType) {
        ClassNode main = cls("mod/Main", "java/lang/Object");
        annotate(main, "Lnet/neoforged/fml/common/Mod;");
        MethodNode init = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitVarInsn(ALOAD, 0);
        init.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(RETURN);
        main.methods.add(init);
        ClassNode events = cls("mod/Events", "java/lang/Object");
        annotate(events, "Lnet/neoforged/fml/common/EventBusSubscriber;");
        MethodNode handler = new MethodNode(ACC_PUBLIC | ACC_STATIC, "on", "(L" + eventType + ";)V", null, null);
        handler.visibleAnnotations = new java.util.ArrayList<>(List.of(new AnnotationNode("Lnet/neoforged/bus/api/SubscribeEvent;")));
        handler.visitInsn(RETURN);
        events.methods.add(handler);
        int n = Subscribers1201.apply(List.of(main, events));
        assertEquals(n == 0, !events.visibleAnnotations.isEmpty());
        return n;
    }

    /** NeoForge refuses to register a listener on an abstract event: the annotation scan must never be left to do it, whichever abstract event it is. */
    @Test
    void staticSubscriberToAnAbstractNeoForgeEventIsNeverLeftToNeoForge() {
        assertFalse(Subscribers1201.ABSTRACT_EVENTS.isEmpty());
        for (String event : Subscribers1201.ABSTRACT_EVENTS) {
            assertEquals(1, applyWithStaticHandler(event), "static subscriber to the abstract event " + event);
        }
    }

    @Test
    void generatedAbstractEventTableCoversTheEventsTheOldHandListNamedAndMore() {
        for (String simple : List.of("level/ExplosionEvent", "entity/player/AdvancementEvent", "entity/player/PlayerEvent", "entity/living/LivingEvent",
                "entity/EntityEvent", "level/LevelEvent", "level/BlockEvent", "entity/player/PlayerInteractEvent", "entity/item/ItemEvent",
                "level/ChunkEvent", "level/ChunkWatchEvent", "entity/living/MobEffectEvent",
                // missing from the hand list
                "level/PistonEvent", "level/NoteBlockEvent", "server/ServerLifecycleEvent", "entity/living/MobSpawnEvent", "entity/player/PlayerXpEvent")) {
            assertTrue(Subscribers1201.ABSTRACT_EVENTS.contains("net/neoforged/neoforge/event/" + simple), simple);
        }
        // concrete in 21.1.258 although the hand list named them
        assertFalse(Subscribers1201.ABSTRACT_EVENTS.contains("net/neoforged/neoforge/event/entity/living/LivingChangeTargetEvent"));
        assertFalse(Subscribers1201.ABSTRACT_EVENTS.contains("net/neoforged/neoforge/event/entity/ProjectileImpactEvent"));
    }

    @Test
    void concreteEventDoesNotRouteAStaticSubscriber() {
        assertEquals(0, applyWithStaticHandler("net/neoforged/neoforge/event/entity/living/LivingDeathEvent"));
    }
}
