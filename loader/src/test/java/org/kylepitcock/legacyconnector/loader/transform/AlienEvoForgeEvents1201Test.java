package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.LabelNode;

/** Regression tests for Forge event handlers whose NeoForge 1.21.1 event APIs changed shape. */
class AlienEvoForgeEvents1201Test implements Opcodes {
    private static final String PHASE = "net/neoforged/neoforge/event/TickEvent$Phase";
    private static final String PLAYER_EVENT = AlienEvoForgeEvents1201.PLAYER_EVENT;
    private static final String VANILLA_LAYERS = AlienEvoForgeEvents1201.VANILLA_GUI_LAYERS;
    private static final String NAMED_OVERLAY = AlienEvoForgeEvents1201.NAMED_GUI_OVERLAY;
    private static final String RENDER_LAYER_PRE = AlienEvoForgeEvents1201.RENDER_GUI_LAYER_PRE;
    private static final String RENDER_LAYER_POST = AlienEvoForgeEvents1201.RENDER_GUI_LAYER_POST;

    private static MethodNode addPhaseGuardedMethod(
            ClassNode node, String name, String descriptor, int eventSlot, String phase, boolean isStatic) {
        MethodNode method = new MethodNode(isStatic ? ACC_PUBLIC | ACC_STATIC : ACC_PUBLIC, name, descriptor, null, null);
        LabelNode skip = new LabelNode();
        method.instructions.add(new VarInsnNode(ALOAD, eventSlot));
        method.instructions.add(new FieldInsnNode(
                GETFIELD, node.name, "phase", "L" + PHASE + ";"));
        method.instructions.add(new FieldInsnNode(
                GETSTATIC, PHASE, phase, "L" + PHASE + ";"));
        method.instructions.add(new JumpInsnNode(IF_ACMPNE, skip));
        returnToSkip(method, skip);
        node.methods.add(method);
        return method;
    }

    private static void returnToSkip(MethodNode method, LabelNode skip) {
        method.instructions.add(skip);
        method.instructions.add(new InsnNode(RETURN));
        method.maxLocals = 2;
        method.maxStack = 2;
    }

    private static ClassNode playerTickFixture(String className, int playerReads) {
        ClassNode node = new ClassNode();
        node.name = className;
        MethodNode method = addPhaseGuardedMethod(
                node, "onPlayerTick", "(L" + AlienEvoForgeEvents1201.PLAYER_TICK_POST + ";)V", 0, "END", true);
        // Insert player reads before the branch target/return.
        AbstractInsnNode returnLabel = method.instructions.getLast().getPrevious();
        for (int i = 0; i < playerReads; i++) {
            method.instructions.insertBefore(returnLabel, new VarInsnNode(ALOAD, 0));
            method.instructions.insertBefore(returnLabel, new FieldInsnNode(
                    GETFIELD, AlienEvoForgeEvents1201.PLAYER_TICK_POST, "player",
                    AlienEvoForgeEvents1201.PLAYER_DESC));
            method.instructions.insertBefore(returnLabel, new InsnNode(POP));
        }
        return node;
    }

    private static ClassNode serverTickFixture() {
        ClassNode node = new ClassNode();
        node.name = "net/habb/alienevo/forge/event/ForgeAlienEntityEvents";
        MethodNode method = addPhaseGuardedMethod(
                node, "onServerTick", "(L" + AlienEvoForgeEvents1201.SERVER_TICK_POST + ";)V", 1, "END", false);
        AbstractInsnNode returnLabel = method.instructions.getLast().getPrevious();
        for (int i = 0; i < 6; i++) {
            method.instructions.insertBefore(returnLabel, new VarInsnNode(ALOAD, 1));
            method.instructions.insertBefore(returnLabel, new MethodInsnNode(
                    INVOKEVIRTUAL, AlienEvoForgeEvents1201.SERVER_TICK_POST, "getServer",
                    "()" + AlienEvoForgeEvents1201.MINECRAFT_SERVER_DESC, false));
            method.instructions.insertBefore(returnLabel, new InsnNode(POP));
        }
        node.innerClasses.add(new InnerClassNode(PHASE, "net/neoforged/neoforge/event/TickEvent",
                "Phase", ACC_PUBLIC | ACC_STATIC | ACC_ENUM));
        return node;
    }

    private static ClassNode clientEventsFixture() {
        ClassNode node = new ClassNode();
        node.name = "net/habb/alienevo/forge/client/ForgeClientEvents";
        MethodNode frame = addPhaseGuardedMethod(
                node, "onRenderTick", "(L" + AlienEvoForgeEvents1201.RENDER_FRAME_PRE + ";)V", 0, "START", true);
        AbstractInsnNode frameReturn = frame.instructions.getLast().getPrevious();
        frame.instructions.insertBefore(frameReturn, new MethodInsnNode(
                INVOKESTATIC, "net/habb/alienevo/client/model/animation/FirstPersonFovAnimator",
                "onFrameStart", "()V", false));

        MethodNode pre = new MethodNode(ACC_PUBLIC | ACC_STATIC, "onRenderOverlayPre",
                "(L" + RENDER_LAYER_PRE + ";)V", null, null);
        for (int i = 0; i < 5; i++) {
            pre.instructions.add(new VarInsnNode(ALOAD, 0));
            pre.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, RENDER_LAYER_PRE,
                    "getOverlay", "()L" + NAMED_OVERLAY + ";", false));
            String layer = switch (i) {
                case 0 -> "PLAYER_HEALTH";
                case 1 -> "FOOD_LEVEL";
                case 2 -> "ARMOR_LEVEL";
                case 3 -> "AIR_LEVEL";
                default -> "MOUNT_HEALTH";
            };
            pre.instructions.add(new FieldInsnNode(GETSTATIC, VANILLA_LAYERS, layer,
                    "L" + VANILLA_LAYERS + ";"));
            pre.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, VANILLA_LAYERS,
                    "type", "()L" + NAMED_OVERLAY + ";", false));
            pre.instructions.add(new JumpInsnNode(IF_ACMPNE, new LabelNode()));
            pre.instructions.add(new VarInsnNode(ALOAD, 0));
            pre.instructions.add(new InsnNode(ICONST_1));
            pre.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, RENDER_LAYER_PRE,
                    "setCanceled", "(Z)V", false));
        }
        // The optional external-overlay check uses NamedGuiOverlay.id() in Forge.
        pre.instructions.add(new VarInsnNode(ALOAD, 0));
        pre.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, RENDER_LAYER_PRE,
                "getOverlay", "()L" + NAMED_OVERLAY + ";", false));
        pre.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, NAMED_OVERLAY,
                "id", "()" + AlienEvoForgeEvents1201.RESOURCE_LOCATION, false));
        pre.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, "net/minecraft/resources/ResourceLocation",
                "toString", "()Ljava/lang/String;", false));
        pre.instructions.add(new LdcInsnNode("overflowingbars:toughness_level"));
        pre.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String",
                "equals", "(Ljava/lang/Object;)Z", false));
        pre.instructions.add(new InsnNode(POP));
        pre.instructions.add(new InsnNode(RETURN));
        node.methods.add(pre);

        MethodNode post = new MethodNode(ACC_PUBLIC | ACC_STATIC, "onRenderOverlayPost",
                "(L" + RENDER_LAYER_POST + ";)V", null, null);
        for (String layer : new String[] {"MOUNT_HEALTH", "FOOD_LEVEL", "HOTBAR"}) {
            post.instructions.add(new VarInsnNode(ALOAD, 0));
            post.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, RENDER_LAYER_POST,
                    "getOverlay", "()L" + NAMED_OVERLAY + ";", false));
            post.instructions.add(new FieldInsnNode(GETSTATIC, VANILLA_LAYERS, layer,
                    "L" + VANILLA_LAYERS + ";"));
            post.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, VANILLA_LAYERS,
                    "type", "()L" + NAMED_OVERLAY + ";", false));
            post.instructions.add(new JumpInsnNode(IF_ACMPNE, new LabelNode()));
        }
        post.instructions.add(new InsnNode(RETURN));
        node.methods.add(post);
        node.innerClasses.add(new InnerClassNode(PHASE, "net/neoforged/neoforge/event/TickEvent",
                "Phase", ACC_PUBLIC | ACC_STATIC | ACC_ENUM));
        return node;
    }

    private static MethodNode method(ClassNode node, String name) {
        return node.methods.stream().filter(candidate -> candidate.name.equals(name)).findFirst().orElseThrow();
    }

    private static int countCalls(MethodNode method, String owner, String name, String descriptor) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call
                    && (owner == null || owner.equals(call.owner))
                    && call.name.equals(name) && call.desc.equals(descriptor)) count++;
        }
        return count;
    }

    private static int countField(MethodNode method, String owner, String name, String descriptor) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == GETSTATIC
                    && field.owner.equals(owner) && field.name.equals(name) && field.desc.equals(descriptor)) count++;
        }
        return count;
    }

    private static boolean hasPhaseField(MethodNode method) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field && field.name.equals("phase")) return true;
        }
        return false;
    }

    @Test
    void migratesPlayerTickHandlersToPostAndPlayerEventGetter() {
        ClassNode scale = playerTickFixture("net/habb/alienevo/forge/ScaleReachForgeHandler", 5);
        ClassNode ride = playerTickFixture("net/habb/alienevo/forge/event/ForgeRideAbilityHandler", 2);

        assertEquals(6, AlienEvoForgeEvents1201.apply(scale));
        assertEquals(3, AlienEvoForgeEvents1201.apply(ride));

        for (ClassNode node : new ClassNode[] {scale, ride}) {
            MethodNode method = method(node, "onPlayerTick");
            assertFalse(hasPhaseField(method));
            assertEquals(0, countField(method, AlienEvoForgeEvents1201.PLAYER_TICK_POST,
                    "player", AlienEvoForgeEvents1201.PLAYER_DESC));
            assertEquals(node == scale ? 5 : 2, countCalls(method, PLAYER_EVENT, "getEntity",
                    "()" + AlienEvoForgeEvents1201.PLAYER_DESC));
            assertEquals(0, AlienEvoForgeEvents1201.apply(node));
        }
    }

    @Test
    void migratesServerTickHandlerToPostAndRemovesLegacyPhaseGate() {
        ClassNode node = serverTickFixture();
        assertEquals(1, AlienEvoForgeEvents1201.apply(node));
        MethodNode method = method(node, "onServerTick");
        assertFalse(hasPhaseField(method));
        assertEquals(6, countCalls(method, AlienEvoForgeEvents1201.SERVER_TICK_POST,
                "getServer", "()" + AlienEvoForgeEvents1201.MINECRAFT_SERVER_DESC));
        assertFalse(node.innerClasses.stream().anyMatch(inner -> inner.name.equals(PHASE)));
        assertEquals(0, AlienEvoForgeEvents1201.apply(node));
    }

    @Test
    void migratesRenderFrameAndGuiLayerHandlersToNeoForgeApi() {
        ClassNode node = clientEventsFixture();
        assertTrue(AlienEvoForgeEvents1201.apply(node) > 0);

        MethodNode frame = method(node, "onRenderTick");
        assertFalse(hasPhaseField(frame));
        assertEquals(1, countCalls(frame, "net/habb/alienevo/client/model/animation/FirstPersonFovAnimator",
                "onFrameStart", "()V"));

        MethodNode pre = method(node, "onRenderOverlayPre");
        MethodNode post = method(node, "onRenderOverlayPost");
        assertEquals(6, countCalls(pre, AlienEvoForgeEvents1201.RENDER_GUI_LAYER, "getName",
                AlienEvoForgeEvents1201.LAYER_GET_NAME_DESC));
        assertEquals(3, countCalls(post, AlienEvoForgeEvents1201.RENDER_GUI_LAYER, "getName",
                AlienEvoForgeEvents1201.LAYER_GET_NAME_DESC));
        assertEquals(5, countField(pre, VANILLA_LAYERS, "VEHICLE_HEALTH",
                AlienEvoForgeEvents1201.RESOURCE_LOCATION)
                + countField(pre, VANILLA_LAYERS, "PLAYER_HEALTH", AlienEvoForgeEvents1201.RESOURCE_LOCATION)
                + countField(pre, VANILLA_LAYERS, "FOOD_LEVEL", AlienEvoForgeEvents1201.RESOURCE_LOCATION)
                + countField(pre, VANILLA_LAYERS, "ARMOR_LEVEL", AlienEvoForgeEvents1201.RESOURCE_LOCATION)
                + countField(pre, VANILLA_LAYERS, "AIR_LEVEL", AlienEvoForgeEvents1201.RESOURCE_LOCATION));
        assertEquals(5, countCalls(pre, AlienEvoForgeEvents1201.CANCELLABLE_EVENT,
                "setCanceled", "(Z)V"));
        assertEquals(0, countCalls(pre, NAMED_OVERLAY, "id",
                "()" + AlienEvoForgeEvents1201.RESOURCE_LOCATION));
        assertEquals(0, countCalls(pre, VANILLA_LAYERS, "type",
                "()L" + NAMED_OVERLAY + ";"));
        assertEquals(0, countCalls(pre, null, "getOverlay", "()L" + NAMED_OVERLAY + ";"));

        int cancelInterfaces = 0;
        for (AbstractInsnNode instruction : pre.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.name.equals("setCanceled")) {
                assertEquals(INVOKEINTERFACE, call.getOpcode());
                assertEquals(AlienEvoForgeEvents1201.CANCELLABLE_EVENT, call.owner);
                cancelInterfaces++;
            }
        }
        assertEquals(5, cancelInterfaces);
        assertEquals(0, AlienEvoForgeEvents1201.apply(node));
    }

    @Test
    void ignoresUnrelatedClasses() {
        ClassNode node = new ClassNode();
        node.name = "net/habb/alienevo/forge/Unrelated";
        assertEquals(0, AlienEvoForgeEvents1201.apply(node));
    }
}
