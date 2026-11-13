package org.kylepitcock.legacyconnector.loader.transform;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import static org.junit.jupiter.api.Assertions.*;

class PalladiumSuitStand1201Test implements Opcodes {
    private static final String SUIT_STAND = PalladiumSuitStand1201.SUIT_STAND;
    private static final String ARMOR_STAND = "net/minecraft/world/entity/decoration/ArmorStand";
    private static final String ENTITY_TYPE = "net/minecraft/world/entity/EntityType";
    private static final String LEVEL = "net/minecraft/world/level/Level";
    private static final String SYNCHED = "net/minecraft/network/syncher/SynchedEntityData";
    private static final String BUILDER = SYNCHED + "$Builder";
    private static final String ACCESSOR = "net/minecraft/network/syncher/EntityDataAccessor";
    private static final String ACCESSOR_DESC = "L" + ACCESSOR + ";";
    private static final String PRIMARY_CTOR = "(L" + ENTITY_TYPE + ";L" + LEVEL + ";)V";
    private static final String BUILDER_DESC = "(L" + BUILDER + ";)V";

    @Test
    void migratesEntityDataToBuilderAndMovesFlagsSetupAfterSuperConstructor() {
        ClassNode node = legacySuitStand();

        assertEquals(1, PalladiumSuitStand1201.apply(node));

        assertNull(find(node, "defineSynchedData", "()V"),
                "the old no-argument override must be replaced so Mc1201Migration does not generate the unsafe bridge");
        MethodNode define = find(node, "defineSynchedData", BUILDER_DESC);
        assertNotNull(define);
        assertEquals(ACC_PROTECTED, define.access & (ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED));
        assertTrue(hasCall(define, INVOKESPECIAL, ARMOR_STAND, "defineSynchedData", BUILDER_DESC));
        assertTrue(hasCall(define, INVOKEVIRTUAL, BUILDER, "define",
                "(" + ACCESSOR_DESC + "Ljava/lang/Object;)L" + BUILDER + ";"));
        assertTrue(hasField(define, GETSTATIC, SUIT_STAND, "DYE_COLOR", ACCESSOR_DESC));
        assertFalse(hasCallOwner(define, SYNCHED),
                "the builder override must not read/write this.entityData before Entity builds it");

        MethodNode constructor = find(node, "<init>", PRIMARY_CTOR);
        assertNotNull(constructor);
        int superCall = indexOfCall(constructor, INVOKESPECIAL, ARMOR_STAND, "<init>", PRIMARY_CTOR);
        int flagsUpdate = indexOfCall(constructor, INVOKEVIRTUAL, SUIT_STAND, "setBit", "(BIZ)B");
        assertTrue(superCall >= 0);
        assertTrue(flagsUpdate > superCall,
                "DATA_CLIENT_FLAGS bit 4 must be set only after ArmorStand's constructor returns");
        assertTrue(hasCall(constructor, INVOKEVIRTUAL, SYNCHED, "set",
                "(" + ACCESSOR_DESC + "Ljava/lang/Object;)V"));

        assertEquals(0, PalladiumSuitStand1201.apply(node), "the migration must be idempotent");
        assertEquals(1, count(node, "defineSynchedData", BUILDER_DESC));
        assertClassCanBeSerialized(node);
    }

    @Test
    void unrelatedClassesAreNotChanged() {
        ClassNode node = legacySuitStand();
        node.name = "example/OtherEntity";

        assertEquals(0, PalladiumSuitStand1201.apply(node));
        assertNotNull(find(node, "defineSynchedData", "()V"));
    }

    @Test
    void unexpectedLegacyFlagBlockIsRejectedBeforeMutation() {
        ClassNode node = legacySuitStand();
        MethodNode legacy = find(node, "defineSynchedData", "()V");
        FieldInsnNode flagRead = null;
        int seen = 0;
        for (AbstractInsnNode instruction : legacy.instructions) {
            if (instruction instanceof FieldInsnNode field && field.getOpcode() == GETSTATIC
                    && field.desc.equals(ACCESSOR_DESC)) {
                seen++;
                if (seen == 2) {
                    flagRead = field;
                    break;
                }
            }
        }
        assertNotNull(flagRead);
        legacy.instructions.remove(flagRead);

        assertThrows(IllegalStateException.class, () -> PalladiumSuitStand1201.apply(node));
        assertNotNull(find(node, "defineSynchedData", "()V"),
                "failed shape checks must leave the original class untouched");
    }

    private static ClassNode legacySuitStand() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = SUIT_STAND;
        node.superName = ARMOR_STAND;
        node.fields.add(new FieldNode(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "DYE_COLOR", ACCESSOR_DESC, null, null));

        MethodNode constructor = new MethodNode(ACC_PUBLIC, "<init>", PRIMARY_CTOR, null, null);
        constructor.instructions.add(new VarInsnNode(ALOAD, 0));
        constructor.instructions.add(new VarInsnNode(ALOAD, 1));
        constructor.instructions.add(new VarInsnNode(ALOAD, 2));
        constructor.instructions.add(new MethodInsnNode(INVOKESPECIAL, ARMOR_STAND, "<init>", PRIMARY_CTOR, false));
        constructor.instructions.add(new InsnNode(RETURN));
        constructor.maxLocals = 3;
        constructor.maxStack = 3;
        node.methods.add(constructor);

        MethodNode define = new MethodNode(ACC_PROTECTED, "defineSynchedData", "()V", null, null);
        define.instructions.add(new VarInsnNode(ALOAD, 0));
        define.instructions.add(new MethodInsnNode(INVOKESPECIAL, ARMOR_STAND, "defineSynchedData", "()V", false));

        // Legacy DYE_COLOR definition, executed during Entity construction in 1.20.1.
        define.instructions.add(new VarInsnNode(ALOAD, 0));
        define.instructions.add(new FieldInsnNode(GETFIELD, SUIT_STAND, "entityData", "L" + SYNCHED + ";"));
        define.instructions.add(new FieldInsnNode(GETSTATIC, SUIT_STAND, "DYE_COLOR", ACCESSOR_DESC));
        define.instructions.add(new InsnNode(ICONST_0));
        define.instructions.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false));
        define.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, SYNCHED, "define",
                "(" + ACCESSOR_DESC + "Ljava/lang/Object;)V", false));

        // Legacy DATA_CLIENT_FLAGS bit-4 update, which must be deferred until after super().
        define.instructions.add(new VarInsnNode(ALOAD, 0));
        define.instructions.add(new FieldInsnNode(GETFIELD, SUIT_STAND, "entityData", "L" + SYNCHED + ";"));
        define.instructions.add(new FieldInsnNode(GETSTATIC, SUIT_STAND, "DATA_CLIENT_FLAGS", ACCESSOR_DESC));
        define.instructions.add(new VarInsnNode(ALOAD, 0));
        define.instructions.add(new VarInsnNode(ALOAD, 0));
        define.instructions.add(new FieldInsnNode(GETFIELD, SUIT_STAND, "entityData", "L" + SYNCHED + ";"));
        define.instructions.add(new FieldInsnNode(GETSTATIC, SUIT_STAND, "DATA_CLIENT_FLAGS", ACCESSOR_DESC));
        define.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, SYNCHED, "get",
                "(" + ACCESSOR_DESC + ")Ljava/lang/Object;", false));
        define.instructions.add(new TypeInsnNode(CHECKCAST, "java/lang/Byte"));
        define.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false));
        define.instructions.add(new InsnNode(ICONST_4));
        define.instructions.add(new InsnNode(ICONST_1));
        define.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, SUIT_STAND, "setBit", "(BIZ)B", false));
        define.instructions.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false));
        define.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, SYNCHED, "set",
                "(" + ACCESSOR_DESC + "Ljava/lang/Object;)V", false));
        define.instructions.add(new InsnNode(RETURN));
        define.maxLocals = 1;
        define.maxStack = 6;
        node.methods.add(define);
        return node;
    }

    private static MethodNode find(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) if (method.name.equals(name) && method.desc.equals(desc)) return method;
        return null;
    }

    private static int count(ClassNode node, String name, String desc) {
        int count = 0;
        for (MethodNode method : node.methods) if (method.name.equals(name) && method.desc.equals(desc)) count++;
        return count;
    }

    private static int indexOfCall(MethodNode method, int opcode, String owner, String name, String desc) {
        int index = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && call.getOpcode() == opcode
                    && call.owner.equals(owner) && call.name.equals(name) && call.desc.equals(desc)) return index;
            index++;
        }
        return -1;
    }

    private static boolean hasCall(MethodNode method, int opcode, String owner, String name, String desc) {
        return indexOfCall(method, opcode, owner, name, desc) >= 0;
    }

    private static boolean hasCallOwner(MethodNode method, String owner) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner)) return true;
        }
        return false;
    }

    private static boolean hasField(MethodNode method, int opcode, String owner, String name, String desc) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode field && field.getOpcode() == opcode
                    && field.owner.equals(owner) && field.name.equals(name) && field.desc.equals(desc)) return true;
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
