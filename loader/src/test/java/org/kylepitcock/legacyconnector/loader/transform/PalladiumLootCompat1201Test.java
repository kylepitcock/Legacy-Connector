package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

class PalladiumLootCompat1201Test implements Opcodes {
    private static final String MANAGER = PalladiumLootCompat1201.MANAGER;
    private static final String DESERIALIZERS = "net/minecraft/world/level/storage/loot/Deserializers";
    private static final String GSON_BUILDER = "com/google/gson/GsonBuilder";
    private static final String GSON = "com/google/gson/Gson";
    private static final String JSON_ELEMENT = "com/google/gson/JsonElement";
    private static final String LOOT_POOL = "net/minecraft/world/level/storage/loot/LootPool";
    private static final String MODIFICATION = MANAGER + "$Modification";
    private static final String HELPER = "legacyconnector/v1_20_1/PalladiumLootCompat";

    @Test
    void rewritesOnlyPalladiumLootGsonFactoryAndPoolDecode() {
        ClassNode node = managerNode();

        assertEquals(1, PalladiumLootCompat1201.apply(node));

        MethodNode initializer = find(node, "<clinit>", "()V");
        assertFalse(hasCall(initializer, INVOKESTATIC, DESERIALIZERS, "m_78800_"));
        assertTrue(hasTypeInsn(initializer, NEW, GSON_BUILDER));
        assertTrue(hasCall(initializer, INVOKESPECIAL, GSON_BUILDER, "<init>", "()V"));
        assertTrue(hasCall(initializer, INVOKEVIRTUAL, GSON_BUILDER, "create", "()Lcom/google/gson/Gson;"));

        MethodNode consumer = find(node, "lambda$apply$0",
                "(Ljava/util/concurrent/atomic/AtomicInteger;Lnet/minecraft/resources/ResourceLocation;"
                        + "L" + MODIFICATION + ";L" + JSON_ELEMENT + ";)V");
        assertTrue(hasCall(consumer, INVOKESTATIC, HELPER, "decodeLootPool",
                "(L" + GSON + ";L" + JSON_ELEMENT + ";Ljava/lang/Class;)L" + LOOT_POOL + ";"));
        assertFalse(hasCall(consumer, INVOKEVIRTUAL, GSON, "fromJson",
                "(L" + JSON_ELEMENT + ";Ljava/lang/Class;)Ljava/lang/Object;"));
        assertTrue(hasCall(consumer, INVOKEVIRTUAL, MODIFICATION, "addPool",
                "(L" + LOOT_POOL + ";)L" + MODIFICATION + ";"));
        assertTrue(hasOpcode(consumer, IFNONNULL),
                "a condition-filtered pool must not be added as null");
        assertTrue(hasOpcode(consumer, GOTO));
        assertClassCanBeSerialized(node);
    }

    @Test
    void leavesUnrelatedClassesUntouched() {
        ClassNode node = managerNode();
        node.name = "example/OtherLootManager";

        assertEquals(0, PalladiumLootCompat1201.apply(node));
        MethodNode initializer = find(node, "<clinit>", "()V");
        assertTrue(hasCall(initializer, INVOKESTATIC, DESERIALIZERS, "m_78800_"));
        MethodNode consumer = find(node, "lambda$apply$0",
                "(Ljava/util/concurrent/atomic/AtomicInteger;Lnet/minecraft/resources/ResourceLocation;"
                        + "L" + MODIFICATION + ";L" + JSON_ELEMENT + ";)V");
        assertTrue(hasCall(consumer, INVOKEVIRTUAL, GSON, "fromJson",
                "(L" + JSON_ELEMENT + ";Ljava/lang/Class;)Ljava/lang/Object;"));
    }

    @Test
    void rejectsAChangedPalladiumShapeInsteadOfPartiallyRewritingIt() {
        ClassNode node = managerNode();
        node.methods.remove(find(node, "lambda$apply$0",
                "(Ljava/util/concurrent/atomic/AtomicInteger;Lnet/minecraft/resources/ResourceLocation;"
                        + "L" + MODIFICATION + ";L" + JSON_ELEMENT + ";)V"));

        assertThrows(IllegalStateException.class, () -> PalladiumLootCompat1201.apply(node));
    }

    private static ClassNode managerNode() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = MANAGER;
        node.superName = "java/lang/Object";

        MethodNode initializer = new MethodNode(ACC_STATIC, "<clinit>", "()V", null, null);
        initializer.instructions.add(new InsnNode(ICONST_0));
        initializer.instructions.add(new FieldInsnNode(PUTSTATIC, MANAGER, "OVERRIDE_FORGE_NAME_LOGIC", "Z"));
        initializer.instructions.add(new MethodInsnNode(INVOKESTATIC, DESERIALIZERS,
                "m_78800_", "()L" + GSON_BUILDER + ";", false));
        initializer.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, GSON_BUILDER,
                "create", "()Lcom/google/gson/Gson;", false));
        initializer.instructions.add(new FieldInsnNode(PUTSTATIC, MANAGER, "GSON", "L" + GSON + ";"));
        initializer.instructions.add(new InsnNode(RETURN));
        initializer.maxStack = 1;
        initializer.maxLocals = 0;
        node.methods.add(initializer);

        String lambdaDesc = "(Ljava/util/concurrent/atomic/AtomicInteger;Lnet/minecraft/resources/ResourceLocation;"
                + "L" + MODIFICATION + ";L" + JSON_ELEMENT + ";)V";
        MethodNode consumer = new MethodNode(ACC_PRIVATE | ACC_STATIC, "lambda$apply$0", lambdaDesc, null, null);
        consumer.instructions.add(new InsnNode(ICONST_1));
        consumer.instructions.add(new FieldInsnNode(PUTSTATIC, MANAGER, "OVERRIDE_FORGE_NAME_LOGIC", "Z"));
        consumer.instructions.add(new VarInsnNode(ALOAD, 2));
        consumer.instructions.add(new FieldInsnNode(GETSTATIC, MANAGER, "GSON", "L" + GSON + ";"));
        consumer.instructions.add(new VarInsnNode(ALOAD, 3));
        consumer.instructions.add(new LdcInsnNode(Type.getObjectType(LOOT_POOL)));
        consumer.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, GSON, "fromJson",
                "(L" + JSON_ELEMENT + ";Ljava/lang/Class;)Ljava/lang/Object;", false));
        consumer.instructions.add(new TypeInsnNode(CHECKCAST, LOOT_POOL));
        consumer.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, MODIFICATION, "addPool",
                "(L" + LOOT_POOL + ";)L" + MODIFICATION + ";", false));
        consumer.instructions.add(new InsnNode(POP));
        consumer.instructions.add(new InsnNode(ICONST_0));
        consumer.instructions.add(new FieldInsnNode(PUTSTATIC, MANAGER, "OVERRIDE_FORGE_NAME_LOGIC", "Z"));
        consumer.instructions.add(new InsnNode(RETURN));
        consumer.maxStack = 3;
        consumer.maxLocals = 4;
        node.methods.add(consumer);
        return node;
    }

    private static MethodNode find(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) return method;
        }
        fail("Missing method " + node.name + "." + name + desc);
        return null;
    }

    private static boolean hasCall(MethodNode method, int opcode, String owner, String name) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == opcode
                    && owner.equals(call.owner)
                    && name.equals(call.name)) return true;
        }
        return false;
    }

    private static boolean hasCall(MethodNode method, int opcode, String owner, String name, String desc) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == opcode
                    && owner.equals(call.owner)
                    && name.equals(call.name)
                    && desc.equals(call.desc)) return true;
        }
        return false;
    }

    private static boolean hasTypeInsn(MethodNode method, int opcode, String type) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof TypeInsnNode node
                    && node.getOpcode() == opcode && type.equals(node.desc)) return true;
        }
        return false;
    }

    private static boolean hasOpcode(MethodNode method, int opcode) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction.getOpcode() == opcode) return true;
        }
        return false;
    }

    private static void assertClassCanBeSerialized(ClassNode node) {
        assertDoesNotThrow(() -> {
            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            writer.toByteArray();
        });
    }
}
