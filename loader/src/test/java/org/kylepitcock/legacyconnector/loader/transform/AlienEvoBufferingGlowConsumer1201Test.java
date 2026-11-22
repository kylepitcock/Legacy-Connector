package org.kylepitcock.legacyconnector.loader.transform;

import org.kylepitcock.legacyconnector.loader.transform.compat.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Regression tests for the AlienEvo buffered glow consumer's 1.21.1 position contract. */
class AlienEvoBufferingGlowConsumer1201Test implements Opcodes {
    private static final String TARGET = AlienEvoBufferingGlowConsumer1201.TARGET;
    private static final String VERTEX = AlienEvoBufferingGlowConsumer1201.VERTEX;
    private static final String POSITION_DESC = "(DDD)L" + VERTEX + ";";
    private static final String FLOAT_POSITION_DESC = "(FFF)L" + VERTEX + ";";
    private static final String ATTRIBUTE_DESC = "(FFF)L" + VERTEX + ";";

    private static ClassNode bufferingGlowConsumer() {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.access = ACC_PUBLIC;
        node.name = TARGET;
        node.superName = "java/lang/Object";
        node.interfaces.add(VERTEX);

        node.fields.add(new FieldNode(ACC_PRIVATE | ACC_FINAL, "solid", "L" + VERTEX + ";", null, null));
        node.fields.add(new FieldNode(ACC_PRIVATE | ACC_FINAL, "glowType", "Lnet/minecraft/client/renderer/RenderType;", null, null));
        node.fields.add(new FieldNode(ACC_PRIVATE | ACC_FINAL, "recordedVertices", "Ljava/util/List;", null, null));
        for (String field : new String[] {"vx", "vy", "vz"}) {
            node.fields.add(new FieldNode(ACC_PRIVATE, field, "D", null, null));
        }
        for (String field : new String[] {"vnx", "vny", "vnz"}) {
            node.fields.add(new FieldNode(ACC_PRIVATE, field, "F", null, null));
        }

        MethodNode constructor = new MethodNode(ACC_PUBLIC, "<init>",
                "(L" + VERTEX + ";Lnet/minecraft/client/renderer/RenderType;)V", null, null);
        constructor.instructions.add(new VarInsnNode(ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        constructor.instructions.add(new VarInsnNode(ALOAD, 0));
        constructor.instructions.add(new VarInsnNode(ALOAD, 1));
        constructor.instructions.add(new FieldInsnNode(PUTFIELD, TARGET, "solid", "L" + VERTEX + ";"));
        constructor.instructions.add(new VarInsnNode(ALOAD, 0));
        constructor.instructions.add(new VarInsnNode(ALOAD, 2));
        constructor.instructions.add(new FieldInsnNode(PUTFIELD, TARGET, "glowType",
                "Lnet/minecraft/client/renderer/RenderType;"));
        constructor.instructions.add(new InsnNode(RETURN));
        constructor.maxLocals = 3;
        constructor.maxStack = 2;
        node.methods.add(constructor);

        MethodNode position = new MethodNode(ACC_PUBLIC, "vertex", POSITION_DESC, null, null);
        node.methods.add(position);
        int[] doubleSlots = {1, 3, 5};
        String[] positionFields = {"vx", "vy", "vz"};
        for (int i = 0; i < positionFields.length; i++) {
            position.instructions.add(new VarInsnNode(ALOAD, 0));
            position.instructions.add(new VarInsnNode(DLOAD, doubleSlots[i]));
            position.instructions.add(new FieldInsnNode(PUTFIELD, TARGET, positionFields[i], "D"));
        }
        position.instructions.add(new VarInsnNode(ALOAD, 0));
        position.instructions.add(new InsnNode(ARETURN));
        position.maxLocals = 7;
        position.maxStack = 3;

        MethodNode endVertex = new MethodNode(ACC_PUBLIC, "endVertex", "()V", null, null);
        node.methods.add(endVertex);
        endVertex.instructions.add(new VarInsnNode(ALOAD, 0));
        endVertex.instructions.add(new FieldInsnNode(GETFIELD, TARGET, "recordedVertices", "Ljava/util/List;"));
        endVertex.instructions.add(new InsnNode(ACONST_NULL));
        endVertex.instructions.add(new MethodInsnNode(INVOKEINTERFACE, "java/util/List",
                "add", "(Ljava/lang/Object;)Z", true));
        endVertex.instructions.add(new InsnNode(POP));
        endVertex.instructions.add(new InsnNode(RETURN));
        endVertex.maxLocals = 1;
        endVertex.maxStack = 2;

        MethodNode normal = new MethodNode(ACC_PUBLIC, "normal", ATTRIBUTE_DESC, null, null);
        node.methods.add(normal);
        int[] floatSlots = {1, 2, 3};
        String[] normalFields = {"vnx", "vny", "vnz"};
        for (int i = 0; i < normalFields.length; i++) {
            normal.instructions.add(new VarInsnNode(ALOAD, 0));
            normal.instructions.add(new VarInsnNode(FLOAD, floatSlots[i]));
            normal.instructions.add(new FieldInsnNode(PUTFIELD, TARGET, normalFields[i], "F"));
        }
        normal.instructions.add(new VarInsnNode(ALOAD, 0));
        normal.instructions.add(new InsnNode(ARETURN));
        normal.maxLocals = 4;
        normal.maxStack = 2;

        addReturningAttribute(node, "color", "(IIII)L" + VERTEX + ";");
        addReturningAttribute(node, "uv", "(FF)L" + VERTEX + ";");
        addReturningAttribute(node, "overlayCoords", "(II)L" + VERTEX + ";");
        addReturningAttribute(node, "uv2", "(II)L" + VERTEX + ";");
        return node;
    }

    private static void addReturningAttribute(ClassNode node, String name, String desc) {
        MethodNode method = new MethodNode(ACC_PUBLIC, name, desc, null, null);
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new InsnNode(ARETURN));
        node.methods.add(method);
    }

    @Test
    void addsAddVertexAndFlushesTheBufferedGlowVertexAfterNormal() {
        ClassNode node = bufferingGlowConsumer();

        int changes = AlienEvoBufferingGlowConsumer1201.apply(node);

        assertTrue(changes >= 7);
        assertTrue(node.interfaces.contains(AlienEvoBufferingGlowConsumer1201.LEGACY_END));
        assertTrue(node.fields.stream().anyMatch(field ->
                field.name.equals("legacyPendingVertex") && field.desc.equals("Z")));

        FieldNode solid = field(node, "solid", "L" + VERTEX + ";");
        FieldNode glowType = field(node, "glowType", "Lnet/minecraft/client/renderer/RenderType;");
        FieldNode recordedVertices = field(node, "recordedVertices", "Ljava/util/List;");
        assertNotNull(solid);
        assertNotNull(glowType);
        assertNotNull(recordedVertices);
        assertEquals(0, solid.access & ACC_FINAL);
        assertTrue((glowType.access & ACC_FINAL) != 0);
        assertTrue((recordedVertices.access & ACC_FINAL) != 0);

        MethodNode setSolidType = find(node, "legacySetSolidType",
                "(Lnet/minecraft/client/renderer/RenderType;)V");
        MethodNode refreshSolid = find(node, "legacyRefreshSolid",
                "(Lnet/minecraft/client/renderer/MultiBufferSource;)V");
        assertNotNull(setSolidType);
        assertNotNull(refreshSolid);
        assertTrue(hasFieldWrite(setSolidType, TARGET, "legacySolidType"));
        assertNotNull(findCall(refreshSolid, AlienEvoBufferingGlowConsumer1201.BUFFER_REFRESH,
                "needsRefresh", "(L" + VERTEX + ";)Z"));
        assertNotNull(findCall(refreshSolid, "net/minecraft/client/renderer/MultiBufferSource",
                "getBuffer", "(Lnet/minecraft/client/renderer/RenderType;)L" + VERTEX + ";"));
        assertTrue(hasFieldWrite(refreshSolid, TARGET, "solid"));
        assertFalse(hasFieldWrite(refreshSolid, TARGET, "glowType"));
        assertFalse(hasFieldWrite(refreshSolid, TARGET, "recordedVertices"));
        assertEquals(0, countOpcode(refreshSolid, NEW));

        MethodNode bridge = find(node, "addVertex", FLOAT_POSITION_DESC);
        assertNotNull(bridge);
        assertNotNull(findCall(bridge, TARGET, "legacyFlushPending", "()V"));
        assertNotNull(findCall(bridge, TARGET, "vertex", POSITION_DESC));
        assertEquals(3, countOpcode(bridge, F2D));
        assertTrue(hasFieldWrite(bridge, TARGET, "legacyPendingVertex"));

        MethodNode normal = find(node, "setNormal", ATTRIBUTE_DESC);
        assertNotNull(normal);
        assertNotNull(findCall(normal, TARGET, "legacyFlushPending", "()V"));
        assertEquals(1, countOpcode(normal, ARETURN));

        MethodNode flush = find(node, "legacyFlushPending", "()V");
        assertNotNull(flush);
        assertNotNull(findCall(flush, TARGET, "endVertex", "()V"));
        assertTrue(hasFieldWrite(flush, TARGET, "legacyPendingVertex"));

        MethodNode legacyEnd = find(node, "legacyEndVertex", "()V");
        assertNotNull(legacyEnd);
        assertNotNull(findCall(legacyEnd, TARGET, "legacyFlushPending", "()V"));

        for (String[] method : new String[][] {
                {"setColor", "(IIII)L" + VERTEX + ";"},
                {"setUv", "(FF)L" + VERTEX + ";"},
                {"setUv1", "(II)L" + VERTEX + ";"},
                {"setUv2", "(II)L" + VERTEX + ";"},
                {"setNormal", ATTRIBUTE_DESC}
        }) {
            assertNotNull(find(node, method[0], method[1]), "missing 1.21.1 VertexConsumer method " + method[0]);
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        assertTrue(writer.toByteArray().length > 0);
    }

    @Test
    void adapterIsIdempotentAfterAllBridgePiecesAreInstalled() {
        ClassNode node = bufferingGlowConsumer();
        assertTrue(AlienEvoBufferingGlowConsumer1201.apply(node) > 0);
        assertEquals(0, AlienEvoBufferingGlowConsumer1201.apply(node));
    }

    @Test
    void ignoresUnrelatedClasses() {
        ClassNode node = bufferingGlowConsumer();
        node.name = "example/UnrelatedConsumer";
        assertEquals(0, AlienEvoBufferingGlowConsumer1201.apply(node));
    }

    @Test
    void failsClosedWhenLegacyPositionMethodIsMissing() {
        ClassNode node = bufferingGlowConsumer();
        node.methods.remove(find(node, "vertex", POSITION_DESC));
        assertThrows(IllegalStateException.class, () -> AlienEvoBufferingGlowConsumer1201.apply(node));
    }

    @Test
    void failsClosedWhenTheLegacyGlowFlushNoLongerRecordsVertices() {
        ClassNode node = bufferingGlowConsumer();
        MethodNode end = find(node, "endVertex", "()V");
        end.instructions.clear();
        end.instructions.add(new InsnNode(RETURN));
        assertThrows(IllegalStateException.class, () -> AlienEvoBufferingGlowConsumer1201.apply(node));
    }

    @Test
    void failsClosedWhenNormalHasAnUnexpectedReturnShape() {
        ClassNode node = bufferingGlowConsumer();
        MethodNode normal = find(node, "normal", ATTRIBUTE_DESC);
        normal.instructions.add(new VarInsnNode(ALOAD, 0));
        normal.instructions.add(new InsnNode(ARETURN));
        assertThrows(IllegalStateException.class, () -> AlienEvoBufferingGlowConsumer1201.apply(node));
    }

    private static FieldNode field(ClassNode node, String name, String desc) {
        for (FieldNode field : node.fields) {
            if (field.name.equals(name) && field.desc.equals(desc)) return field;
        }
        return null;
    }

    private static MethodNode find(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) return method;
        }
        return null;
    }

    private static MethodInsnNode findCall(MethodNode method, String owner, String name, String desc) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner)
                    && call.name.equals(name) && call.desc.equals(desc)) return call;
        }
        return null;
    }

    private static boolean hasFieldWrite(MethodNode method, String owner, String name) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field && field.getOpcode() == PUTFIELD
                    && field.owner.equals(owner) && field.name.equals(name)) return true;
        }
        return false;
    }

    private static int countOpcode(MethodNode method, int opcode) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() == opcode) count++;
        }
        return count;
    }
}
