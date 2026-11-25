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
    private static final String PLUMBER_COLLECTOR = AlienEvoVertexConsumer1201.PLUMBER_COLLECTOR;

    private static ClassNode flat() {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.access = ACC_FINAL;
        node.name = AlienEvoVertexConsumer1201.FLAT;
        node.superName = AlienEvoVertexConsumer1201.DEFAULTED;
        node.fields.add(new FieldNode(ACC_PRIVATE | ACC_FINAL, "delegate", "L" + VERTEX + ";", null, null));
        node.fields.add(new FieldNode(ACC_PRIVATE | ACC_FINAL, "full", "Z", null, null));
        node.fields.add(new FieldNode(ACC_PRIVATE, "x", "D", null, null));
        node.fields.add(new FieldNode(ACC_PRIVATE, "y", "D", null, null));
        node.fields.add(new FieldNode(ACC_PRIVATE, "z", "D", null, null));

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

        MethodNode position = new MethodNode(ACC_PUBLIC, "vertex", "(DDD)L" + VERTEX + ";", null, null);
        node.methods.add(position);
        position.instructions.add(new VarInsnNode(ALOAD, 0));
        position.instructions.add(new VarInsnNode(DLOAD, 1));
        position.instructions.add(new FieldInsnNode(PUTFIELD, node.name, "x", "D"));
        position.instructions.add(new VarInsnNode(ALOAD, 0));
        position.instructions.add(new VarInsnNode(DLOAD, 3));
        position.instructions.add(new FieldInsnNode(PUTFIELD, node.name, "y", "D"));
        position.instructions.add(new VarInsnNode(ALOAD, 0));
        position.instructions.add(new VarInsnNode(DLOAD, 5));
        position.instructions.add(new FieldInsnNode(PUTFIELD, node.name, "z", "D"));
        position.instructions.add(new VarInsnNode(ALOAD, 0));
        position.instructions.add(new InsnNode(ARETURN));
        position.maxLocals = 7;
        position.maxStack = 3;

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

    private static ClassNode plumberCollector() {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.access = ACC_FINAL;
        node.name = PLUMBER_COLLECTOR;
        node.superName = "java/lang/Object";
        node.interfaces.add(VERTEX);

        MethodNode position = new MethodNode(ACC_PUBLIC, "vertex", "(DDD)L" + VERTEX + ";", null, null);
        position.instructions.add(new VarInsnNode(ALOAD, 0));
        position.instructions.add(new InsnNode(ARETURN));
        position.maxLocals = 7;
        position.maxStack = 1;
        node.methods.add(position);

        MethodNode endVertex = new MethodNode(ACC_PUBLIC, "endVertex", "()V", null, null);
        endVertex.instructions.add(new InsnNode(RETURN));
        endVertex.maxLocals = 1;
        endVertex.maxStack = 0;
        node.methods.add(endVertex);

        for (String[] method : new String[][] {
                {"color", "(IIII)L" + VERTEX + ";"},
                {"uv", "(FF)L" + VERTEX + ";"},
                {"overlayCoords", "(II)L" + VERTEX + ";"},
                {"uv2", "(II)L" + VERTEX + ";"},
                {"normal", "(FFF)L" + VERTEX + ";"}
        }) {
            MethodNode setter = new MethodNode(ACC_PUBLIC, method[0], method[1], null, null);
            setter.instructions.add(new VarInsnNode(ALOAD, 0));
            setter.instructions.add(new InsnNode(ARETURN));
            setter.maxLocals = 5;
            setter.maxStack = 1;
            node.methods.add(setter);
        }
        return node;
    }

    private static ClassNode plumberPrinted() {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.access = ACC_FINAL;
        node.name = "net/habb/alienevo/client/plumber/scan/PlumberScanHologram$Printed";
        node.superName = "java/lang/Object";
        node.interfaces.add(VERTEX);

        MethodNode position = new MethodNode(ACC_PUBLIC, "vertex", "(DDD)L" + VERTEX + ";", null, null);
        position.instructions.add(new VarInsnNode(ALOAD, 0));
        position.instructions.add(new InsnNode(ARETURN));
        position.maxLocals = 7;
        position.maxStack = 1;
        node.methods.add(position);

        MethodNode endVertex = new MethodNode(ACC_PUBLIC, "endVertex", "()V", null, null);
        endVertex.instructions.add(new InsnNode(RETURN));
        endVertex.maxLocals = 1;
        endVertex.maxStack = 0;
        node.methods.add(endVertex);

        for (String[] method : new String[][] {
                {"color", "(IIII)L" + VERTEX + ";"},
                {"uv", "(FF)L" + VERTEX + ";"},
                {"overlayCoords", "(II)L" + VERTEX + ";"},
                {"uv2", "(II)L" + VERTEX + ";"},
                {"normal", "(FFF)L" + VERTEX + ";"}
        }) {
            MethodNode setter = new MethodNode(ACC_PUBLIC, method[0], method[1], null, null);
            setter.instructions.add(new VarInsnNode(ALOAD, 0));
            setter.instructions.add(new InsnNode(ARETURN));
            setter.maxLocals = 5;
            setter.maxStack = 1;
            node.methods.add(setter);
        }

        MethodNode emit = new MethodNode(0, "emit", "(L" + VERTEX + ";I)V", null, null);
        emit.instructions.add(new VarInsnNode(ALOAD, 1));
        emit.instructions.add(new MethodInsnNode(INVOKEINTERFACE, VERTEX, "vertex",
                "(FFFFFFFFFIIFFF)V", true));
        emit.instructions.add(new InsnNode(RETURN));
        emit.maxLocals = 3;
        emit.maxStack = 15;
        node.methods.add(emit);
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
        MethodNode legacyPosition = find(node, "vertex", "(DDD)L" + VERTEX + ";");
        assertNotNull(legacyPosition);
        assertTrue(legacyPosition.instructions.toArray().length > 0);
        assertTrue(java.util.Arrays.stream(legacyPosition.instructions.toArray()).anyMatch(instruction ->
                instruction instanceof FieldInsnNode field && field.getOpcode() == PUTFIELD
                        && field.owner.equals(node.name) && field.name.equals("legacyPendingVertex")),
                "the old position setter must mark its stored vertex for legacy endVertex emission");
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

    @Test
    void plumberScanCollectorImplementsTheFullOneTwentyOneVertexSequence() {
        ClassNode node = plumberCollector();

        assertTrue(AlienEvoVertexConsumer1201.apply(node) > 0,
                "the Plumber collector should receive the 1.21.1 adapter");
        assertTrue(node.interfaces.contains(VERTEX));
        assertNotNull(find(node, "vertex", "(DDD)L" + VERTEX + ";"),
                "the legacy position setter must remain as the bridge target");
        assertNotNull(find(node, "endVertex", "()V"),
                "the old quad-accumulation terminal must remain intact");

        MethodNode addVertex = find(node, "addVertex", "(FFF)L" + VERTEX + ";");
        assertNotNull(addVertex);
        assertNotNull(findCall(addVertex, node.name, "vertex", "(DDD)L" + VERTEX + ";"));
        assertEquals(3, java.util.Arrays.stream(addVertex.instructions.toArray())
                .filter(instruction -> instruction.getOpcode() == F2D).count(),
                "each 1.21.1 float coordinate must be widened for the legacy position method");
        assertNull(findCall(addVertex, node.name, "endVertex", "()V"),
                "adding a position must not commit an incomplete quad vertex");

        for (String[] method : new String[][] {
                {"setColor", "(IIII)L" + VERTEX + ";"},
                {"setUv", "(FF)L" + VERTEX + ";"},
                {"setUv1", "(II)L" + VERTEX + ";"},
                {"setUv2", "(II)L" + VERTEX + ";"},
                {"setNormal", "(FFF)L" + VERTEX + ";"}
        }) {
            assertNotNull(find(node, method[0], method[1]), "missing 1.21.1 setter " + method[0]);
        }
        assertNotNull(find(node, "setNormal", "(FFF)L" + VERTEX + ";"));
        assertNull(find(node, "normal", "(FFF)L" + VERTEX + ";"),
                "the legacy normal name must be remapped to the 1.21.1 setter");
        assertNotNull(findCall(find(node, "setNormal", "(FFF)L" + VERTEX + ";"),
                node.name, "endVertex", "()V"),
                "setNormal must commit the old per-vertex buffer entry after writing its normal");

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        assertTrue(writer.toByteArray().length > 0);

        assertEquals(0, AlienEvoVertexConsumer1201.apply(node),
                "reapplying the transformer must not duplicate bridges or endVertex flushes");
    }

    @Test
    void plumberCollectorMigrationFailsClosedWhenTheLegacyShapeIsIncomplete() {
        ClassNode node = plumberCollector();
        node.methods.remove(find(node, "normal", "(FFF)L" + VERTEX + ";"));

        assertEquals(0, AlienEvoVertexConsumer1201.apply(node));
        assertNull(find(node, "addVertex", "(FFF)L" + VERTEX + ";"),
                "do not install a partial adapter when the old normal setter is missing");
    }

    @Test
    void plumberBadgePrintedPreservesBufferedQuadSemanticsOnOneTwentyOne() {
        ClassNode node = plumberPrinted();

        assertTrue(AlienEvoVertexConsumer1201.apply(node) > 0,
                "the Badge Printed consumer should receive the 1.21.1 adapter");
        assertTrue(node.interfaces.contains(VERTEX));

        MethodNode addVertex = find(node, "addVertex", "(FFF)L" + VERTEX + ";");
        assertNotNull(addVertex);
        assertNotNull(findCall(addVertex, node.name, "vertex", "(DDD)L" + VERTEX + ";"));
        assertEquals(3, java.util.Arrays.stream(addVertex.instructions.toArray())
                .filter(instruction -> instruction.getOpcode() == F2D).count());
        assertNull(findCall(addVertex, node.name, "endVertex", "()V"),
                "addVertex only stores the position; it must not prematurely commit a partial quad");

        for (String[] method : new String[][] {
                {"setColor", "(IIII)L" + VERTEX + ";"},
                {"setUv", "(FF)L" + VERTEX + ";"},
                {"setUv1", "(II)L" + VERTEX + ";"},
                {"setUv2", "(II)L" + VERTEX + ";"},
                {"setNormal", "(FFF)L" + VERTEX + ";"}
        }) {
            assertNotNull(find(node, method[0], method[1]), "missing 1.21.1 setter " + method[0]);
        }
        assertNull(find(node, "normal", "(FFF)L" + VERTEX + ";"));
        assertNotNull(findCall(find(node, "setNormal", "(FFF)L" + VERTEX + ";"),
                node.name, "endVertex", "()V"),
                "the old endVertex operation must run only after setNormal finishes the vertex");

        MethodNode emit = find(node, "emit", "(L" + VERTEX + ";I)V");
        assertNotNull(emit);
        assertNotNull(findCall(emit, AlienEvoVertexConsumer1201.HELPER, "vertex",
                "(L" + VERTEX + ";FFFFFFFFFIIFFF)V"));
        assertNull(findCall(emit, VERTEX, "vertex", "(FFFFFFFFFIIFFF)V"));

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        assertTrue(writer.toByteArray().length > 0);
        assertEquals(0, AlienEvoVertexConsumer1201.apply(node),
                "the Printed adapter must be idempotent");
    }

    @Test
    void plumberBadgePrintedMigrationFailsClosedWhenItsLegacySetterSetIsIncomplete() {
        ClassNode node = plumberPrinted();
        node.methods.remove(find(node, "normal", "(FFF)L" + VERTEX + ";"));

        assertEquals(0, AlienEvoVertexConsumer1201.apply(node));
        assertNull(find(node, "addVertex", "(FFF)L" + VERTEX + ";"),
                "do not install a partial Printed adapter");
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
