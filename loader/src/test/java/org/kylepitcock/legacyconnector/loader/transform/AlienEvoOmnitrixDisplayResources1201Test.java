package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Verifies the narrowly scoped Omnitrix preview Gson field-owner migration. */
class AlienEvoOmnitrixDisplayResources1201Test implements Opcodes {
    private static ClassNode displayResources() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = AlienEvoOmnitrixDisplayResources1201.TARGET;
        node.superName = "java/lang/Object";

        MethodNode loadModel = new MethodNode(
                ACC_PRIVATE | ACC_SYNTHETIC,
                AlienEvoOmnitrixDisplayResources1201.MODEL_METHOD,
                AlienEvoOmnitrixDisplayResources1201.MODEL_METHOD_DESC,
                null,
                null);
        loadModel.instructions.add(new FieldInsnNode(
                GETSTATIC,
                AlienEvoOmnitrixDisplayResources1201.LEGACY_OWNER,
                AlienEvoOmnitrixDisplayResources1201.FIELD_NAME,
                AlienEvoOmnitrixDisplayResources1201.FIELD_DESC));
        loadModel.instructions.add(new InsnNode(POP));
        loadModel.instructions.add(new InsnNode(ACONST_NULL));
        loadModel.instructions.add(new InsnNode(ARETURN));
        loadModel.maxLocals = 2;
        loadModel.maxStack = 1;
        node.methods.add(loadModel);
        return node;
    }

    private static FieldInsnNode gsonField(ClassNode node) {
        MethodNode method = node.methods.getFirst();
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field
                    && field.name.equals(AlienEvoOmnitrixDisplayResources1201.FIELD_NAME)) {
                return field;
            }
        }
        throw new AssertionError("GEO_GSON field access not found");
    }

    @Test
    void rewritesOnlyTheOmnitrixPreviewGsonOwnerAndIsIdempotent() {
        ClassNode node = displayResources();

        assertEquals(1, AlienEvoOmnitrixDisplayResources1201.apply(node));
        FieldInsnNode field = gsonField(node);
        assertEquals(GETSTATIC, field.getOpcode());
        assertEquals(AlienEvoOmnitrixDisplayResources1201.GECKO_49_OWNER, field.owner);
        assertEquals("GEO_GSON", field.name);
        assertEquals("Lcom/google/gson/Gson;", field.desc);

        assertEquals(0, AlienEvoOmnitrixDisplayResources1201.apply(node));
        assertClassCanBeSerialized(node);
    }

    @Test
    void unrelatedClassesAreNotModified() {
        ClassNode node = displayResources();
        node.name = "example/UnrelatedRenderer";

        assertEquals(0, AlienEvoOmnitrixDisplayResources1201.apply(node));
        assertEquals(AlienEvoOmnitrixDisplayResources1201.LEGACY_OWNER, gsonField(node).owner);
    }

    @Test
    void rejectsMissingOrUnexpectedFieldOwnerInsteadOfGuessing() {
        ClassNode missing = displayResources();
        missing.methods.getFirst().instructions.clear();
        assertThrows(IllegalStateException.class,
                () -> AlienEvoOmnitrixDisplayResources1201.apply(missing));

        ClassNode unexpected = displayResources();
        gsonField(unexpected).owner = "software/bernie/geckolib/util/UnknownJsonUtil";
        assertThrows(IllegalStateException.class,
                () -> AlienEvoOmnitrixDisplayResources1201.apply(unexpected));
    }

    @Test
    void rejectsDuplicateOrMisplacedGsonReferences() {
        ClassNode duplicate = displayResources();
        MethodNode method = duplicate.methods.getFirst();
        method.instructions.insertBefore(method.instructions.getFirst(), new FieldInsnNode(
                GETSTATIC,
                AlienEvoOmnitrixDisplayResources1201.LEGACY_OWNER,
                AlienEvoOmnitrixDisplayResources1201.FIELD_NAME,
                AlienEvoOmnitrixDisplayResources1201.FIELD_DESC));
        assertThrows(IllegalStateException.class,
                () -> AlienEvoOmnitrixDisplayResources1201.apply(duplicate));

        ClassNode misplaced = displayResources();
        MethodNode unrelatedMethod = new MethodNode(ACC_PRIVATE, "other", "()V", null, null);
        unrelatedMethod.instructions.add(new FieldInsnNode(
                GETSTATIC,
                AlienEvoOmnitrixDisplayResources1201.LEGACY_OWNER,
                AlienEvoOmnitrixDisplayResources1201.FIELD_NAME,
                AlienEvoOmnitrixDisplayResources1201.FIELD_DESC));
        unrelatedMethod.instructions.add(new InsnNode(POP));
        unrelatedMethod.instructions.add(new InsnNode(RETURN));
        misplaced.methods.add(unrelatedMethod);
        assertThrows(IllegalStateException.class,
                () -> AlienEvoOmnitrixDisplayResources1201.apply(misplaced));
    }

    private static void assertClassCanBeSerialized(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        assertTrue(writer.toByteArray().length > 0);
        assertFalse(writer.toByteArray().length == 0);
    }
}
