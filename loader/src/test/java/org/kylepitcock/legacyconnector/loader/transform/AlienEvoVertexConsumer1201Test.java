package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Verifies the removed superclass and old vertex-call shapes are migrated. */
class AlienEvoVertexConsumer1201Test implements Opcodes {
    private static final String VERTEX = AlienEvoVertexConsumer1201.VERTEX;

    private static ClassNode flat() {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.access = ACC_FINAL;
        node.name = AlienEvoVertexConsumer1201.FLAT;
        node.superName = AlienEvoVertexConsumer1201.DEFAULTED;
        node.fields.add(new FieldNode(ACC_PRIVATE | ACC_FINAL, "delegate", "L" + VERTEX + ";", null, null));
        node.fields.add(new FieldNode(ACC_PRIVATE | ACC_FINAL, "full", "Z", null, null));

        MethodNode init = new MethodNode(ACC_PUBLIC, "<init>", "(L" + VERTEX + ";IIIIZ)V", null, null);
        node.methods.add(init);
        init.instructions.add(new VarInsnNode(ALOAD, 0));
        init.instructions.add(new MethodInsnNode(INVOKESPECIAL,
                AlienEvoVertexConsumer1201.DEFAULTED, "<init>", "()V", false));
        init.instructions.add(new VarInsnNode(ALOAD, 0));
        init.instructions.add(new VarInsnNode(ALOAD, 1));
        init.instructions.add(new FieldInsnNode(PUTFIELD, node.name, "delegate", "L" + VERTEX + ";"));
        init.instructions.add(new VarInsnNode(ALOAD, 0));
        for (int slot = 2; slot <= 5; slot++) init.instructions.add(new VarInsnNode(ILOAD, slot));
        init.instructions.add(new MethodInsnNode(INVOKESPECIAL,
                AlienEvoVertexConsumer1201.DEFAULTED, "defaultColor", "(IIII)V", false));
        init.instructions.add(new InsnNode(RETURN));
        init.maxLocals = 7;
        init.maxStack = 5;

        MethodNode endVertex = new MethodNode(ACC_PUBLIC, "endVertex", "()V", null, null);
        node.methods.add(endVertex);
        endVertex.instructions.add(new InsnNode(RETURN));

        MethodNode emit = new MethodNode(ACC_PRIVATE, "emit", "(DDDFF)V", null, null);
        node.methods.add(emit);
        emit.instructions.add(new VarInsnNode(ALOAD, 0));
        emit.instructions.add(new FieldInsnNode(GETFIELD, node.name, "delegate", "L" + VERTEX + ";"));
        emit.instructions.add(new VarInsnNode(DLOAD, 1));
        emit.instructions.add(new VarInsnNode(DLOAD, 3));
        emit.instructions.add(new VarInsnNode(DLOAD, 5));
        emit.instructions.add(new MethodInsnNode(INVOKEINTERFACE, VERTEX, "vertex",
                "(DDD)L" + VERTEX + ";", true));
        for (String field : List.of("defaultR", "defaultG", "defaultB", "defaultA")) {
            emit.instructions.add(new VarInsnNode(ALOAD, 0));
            emit.instructions.add(new FieldInsnNode(GETFIELD, AlienEvoVertexConsumer1201.DEFAULTED, field, "I"));
        }
        emit.instructions.add(new MethodInsnNode(INVOKEINTERFACE, VERTEX, "color", "(IIII)L" + VERTEX + ";", true));
        emit.instructions.add(new VarInsnNode(FLOAD, 7));
        emit.instructions.add(new VarInsnNode(FLOAD, 8));
        emit.instructions.add(new MethodInsnNode(INVOKEINTERFACE, VERTEX, "uv", "(FF)L" + VERTEX + ";", true));
        emit.instructions.add(new MethodInsnNode(INVOKEINTERFACE, VERTEX, "endVertex", "()V", true));
        emit.instructions.add(new InsnNode(RETURN));
        emit.maxLocals = 9;
        emit.maxStack = 10;

        MethodNode full = new MethodNode(ACC_PRIVATE, "emitFull", "(FFFFFFFFFIIFFF)V", null, null);
        node.methods.add(full);
        full.instructions.add(new VarInsnNode(ALOAD, 0));
        full.instructions.add(new FieldInsnNode(GETFIELD, node.name, "delegate", "L" + VERTEX + ";"));
        // The old full vertex method takes nine floats, two ints, then three floats.
        int[] slots = {1,2,3,4,5,6,7,8,9,10,11,12,13,14};
        for (int slot : slots) {
            int opcode = slot == 10 || slot == 11 ? ILOAD : FLOAD;
            full.instructions.add(new VarInsnNode(opcode, slot));
        }
        full.instructions.add(new MethodInsnNode(INVOKEINTERFACE, VERTEX, "vertex",
                "(FFFFFFFFFIIFFF)V", true));
        full.instructions.add(new InsnNode(RETURN));
        full.maxLocals = 15;
        full.maxStack = 15;

        // Original concrete implementations that must be renamed to the 1.21.1 interface methods.
        for (String[] method : new String[][] {
                {"color", "(IIII)L" + VERTEX + ";"},
                {"uv", "(FF)L" + VERTEX + ";"},
                {"overlayCoords", "(II)L" + VERTEX + ";"},
                {"uv2", "(II)L" + VERTEX + ";"},
                {"normal", "(FFF)L" + VERTEX + ";"}
        }) {
            MethodNode implementation = new MethodNode(ACC_PUBLIC, method[0], method[1], null, null);
            implementation.instructions.add(new VarInsnNode(ALOAD, 0));
            implementation.instructions.add(new InsnNode(ARETURN));
            node.methods.add(implementation);
        }
        return node;
    }

    @Test
    void flatConsumerBecomesAValidOneTwentyOneVertexConsumerShape() {
        ClassNode node = flat();
        assertTrue(AlienEvoVertexConsumer1201.apply(node) > 0);

        assertEquals("java/lang/Object", node.superName);
        assertTrue(node.interfaces.contains(VERTEX));
        assertTrue(node.interfaces.contains(AlienEvoVertexConsumer1201.LEGACY_END));
        assertTrue(node.fields.stream().anyMatch(field ->
                field.name.equals("legacyPendingVertex") && field.desc.equals("Z")));
        assertNotNull(find(node, "legacyFlushPending", "()V"));
        MethodNode legacyEnd = find(node, "legacyEndVertex", "()V");
        assertNotNull(legacyEnd);
        assertNotNull(findCall(legacyEnd, node.name, "legacyFlushPending", "()V"));
        for (String color : List.of("defaultR", "defaultG", "defaultB", "defaultA")) {
            assertTrue(node.fields.stream().anyMatch(f -> f.name.equals(color) && f.desc.equals("I")),
                    "the subclass must own inherited color state " + color);
        }

        MethodNode init = find(node, "<init>", "(L" + VERTEX + ";IIIIZ)V");
        assertNotNull(init);
        assertNotNull(findCall(init, "java/lang/Object", "<init>", "()V"));
        assertNotNull(findCall(init, node.name, "alienevo$initializeDefaultColor", "(IIII)V"));

        MethodNode defaults = find(node, "alienevo$initializeDefaultColor", "(IIII)V");
        assertNotNull(defaults);
        assertEquals(List.of("defaultR", "defaultG", "defaultB", "defaultA"),
                writes(defaults, node.name));

        MethodNode emit = find(node, "emit", "(DDDFF)V");
        assertNotNull(findCall(emit, AlienEvoVertexConsumer1201.HELPER, "vertex",
                "(L" + VERTEX + ";DDD)L" + VERTEX + ";"));
        assertNotNull(findCall(emit, VERTEX, "setColor", "(IIII)L" + VERTEX + ";"));
        assertNotNull(findCall(emit, VERTEX, "setUv", "(FF)L" + VERTEX + ";"));
        assertNotNull(findCall(emit, AlienEvoVertexConsumer1201.HELPER, "endVertex", "(L" + VERTEX + ";)V"));
        for (var instruction : emit.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field) {
                assertFalse(field.owner.equals(AlienEvoVertexConsumer1201.DEFAULTED));
            }
        }

        MethodNode full = find(node, "emitFull", "(FFFFFFFFFIIFFF)V");
        assertNotNull(findCall(full, AlienEvoVertexConsumer1201.HELPER, "vertex",
                "(L" + VERTEX + ";FFFFFFFFFIIFFF)V"));
        assertNotNull(find(node, "addVertex", "(FFF)L" + VERTEX + ";"));
        assertNull(findCall(find(node, "setUv2", "(II)L" + VERTEX + ";"),
                node.name, "legacyFlushPending", "()V"),
                "overlay/light setters occur before the final normal and must not flush early");
        assertNotNull(findCall(find(node, "setNormal", "(FFF)L" + VERTEX + ";"),
                node.name, "legacyFlushPending", "()V"));
        for (String method : List.of("setColor", "setUv", "setUv1", "setUv2", "setNormal")) {
            assertTrue(node.methods.stream().anyMatch(m -> m.name.equals(method)),
                    "missing new interface method " + method);
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        assertTrue(writer.toByteArray().length > 0);
    }

    private static MethodNode find(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) if (method.name.equals(name) && method.desc.equals(desc)) return method;
        return null;
    }

    private static MethodInsnNode findCall(MethodNode method, String owner, String name, String desc) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner)
                    && call.name.equals(name) && call.desc.equals(desc)) return call;
        }
        return null;
    }

    private static List<String> writes(MethodNode method, String owner) {
        return java.util.Arrays.stream(method.instructions.toArray())
                .filter(in -> in instanceof FieldInsnNode field && field.getOpcode() == PUTFIELD && field.owner.equals(owner))
                .map(in -> ((FieldInsnNode) in).name)
                .toList();
    }
}
