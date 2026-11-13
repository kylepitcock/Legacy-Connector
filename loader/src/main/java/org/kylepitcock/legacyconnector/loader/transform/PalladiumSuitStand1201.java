package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Migrates Palladium 4.5.10's SuitStand from the 1.20.1 entity-data lifecycle
 * to the 1.21.1 builder lifecycle. Runs before Mc1201Migration so its generic
 * defineSynchedData bridge is not generated around the unsafe legacy override.
 */
final class PalladiumSuitStand1201 implements Opcodes {
    static final String SUIT_STAND = "net/threetag/palladium/entity/SuitStand";

    private static final String ARMOR_STAND = "net/minecraft/world/entity/decoration/ArmorStand";
    private static final String ENTITY_TYPE = "net/minecraft/world/entity/EntityType";
    private static final String LEVEL = "net/minecraft/world/level/Level";
    private static final String SYNCHED_DATA = "net/minecraft/network/syncher/SynchedEntityData";
    private static final String BUILDER = SYNCHED_DATA + "$Builder";
    private static final String ACCESSOR = "net/minecraft/network/syncher/EntityDataAccessor";
    private static final String ACCESSOR_DESC = "L" + ACCESSOR + ";";
    private static final String SYNCHED_DATA_DESC = "L" + SYNCHED_DATA + ";";
    private static final String LEGACY_DEFINE_DESC = "()V";
    private static final String BUILDER_DEFINE_DESC = "(L" + BUILDER + ";)V";
    private static final String CONSTRUCTOR_DESC = "(L" + ENTITY_TYPE + ";L" + LEVEL + ";)V";
    private static final String BUILDER_METHOD_DESC =
            "(L" + ACCESSOR + ";Ljava/lang/Object;)L" + BUILDER + ";";

    private PalladiumSuitStand1201() {}

    /** Returns one when Palladium's legacy override was migrated. */
    static int apply(ClassNode node) {
        if (!SUIT_STAND.equals(node.name)) return 0;

        MethodNode legacy = find(node, "defineSynchedData", LEGACY_DEFINE_DESC);
        MethodNode modern = find(node, "defineSynchedData", BUILDER_DEFINE_DESC);
        if (legacy == null) {
            // Already migrated, or a later Palladium build owns a different implementation.
            return 0;
        }
        if (modern != null) {
            throw new IllegalStateException("Palladium SuitStand has both legacy and builder defineSynchedData methods");
        }

        FieldNode dyeColor = null;
        for (FieldNode field : node.fields) {
            if (field.name.equals("DYE_COLOR") && field.desc.equals(ACCESSOR_DESC)) {
                dyeColor = field;
                break;
            }
        }
        if (dyeColor == null || (dyeColor.access & ACC_STATIC) == 0) {
            throw new IllegalStateException("Palladium SuitStand DYE_COLOR accessor was not found");
        }

        // Capture the old DATA_CLIENT_FLAGS mutation before replacing the method.
        // It must run only after ArmorStand's constructor has built entityData.
        InsnList flagSetup = extractFlagSetup(node, legacy);
        MethodNode constructor = find(node, "<init>", CONSTRUCTOR_DESC);
        if (constructor == null) {
            throw new IllegalStateException("Palladium SuitStand primary EntityType/Level constructor was not found");
        }
        MethodInsnNode armorStandConstructor = findArmorStandConstructor(constructor);
        if (armorStandConstructor == null) {
            throw new IllegalStateException("Palladium SuitStand constructor no longer calls ArmorStand(EntityType, Level)");
        }
        if (hasSetBitCall(constructor, node.name)) {
            throw new IllegalStateException("Palladium SuitStand constructor already contains the DATA_CLIENT_FLAGS setup");
        }

        // Recreate the modern override: Entity/ArmorStand data is defined through
        // the builder, and DYE_COLOR is registered before entityData is built.
        legacy.instructions.clear();
        legacy.tryCatchBlocks.clear();
        legacy.localVariables = null;
        legacy.parameters = null;
        legacy.visibleParameterAnnotations = null;
        legacy.invisibleParameterAnnotations = null;
        legacy.visibleAnnotableParameterCount = 0;
        legacy.invisibleAnnotableParameterCount = 0;
        legacy.signature = null;
        legacy.desc = BUILDER_DEFINE_DESC;
        legacy.access = (legacy.access & ~(ACC_PUBLIC | ACC_PRIVATE | ACC_STATIC | ACC_ABSTRACT | ACC_NATIVE)) | ACC_PROTECTED;
        legacy.instructions.add(new VarInsnNode(ALOAD, 0));
        legacy.instructions.add(new VarInsnNode(ALOAD, 1));
        legacy.instructions.add(new MethodInsnNode(INVOKESPECIAL, ARMOR_STAND, "defineSynchedData", BUILDER_DEFINE_DESC, false));
        legacy.instructions.add(new VarInsnNode(ALOAD, 1));
        legacy.instructions.add(new FieldInsnNode(GETSTATIC, node.name, dyeColor.name, dyeColor.desc));
        legacy.instructions.add(new InsnNode(ICONST_0));
        legacy.instructions.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false));
        legacy.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, BUILDER, "define", BUILDER_METHOD_DESC, false));
        legacy.instructions.add(new InsnNode(POP));
        legacy.instructions.add(new InsnNode(RETURN));
        legacy.maxStack = 4;
        legacy.maxLocals = 2;

        constructor.instructions.insert(armorStandConstructor, flagSetup);
        constructor.maxStack = Math.max(constructor.maxStack, 6);
        return 1;
    }

    private static InsnList extractFlagSetup(ClassNode node, MethodNode legacy) {
        List<FieldInsnNode> accessorReads = new ArrayList<>();
        for (AbstractInsnNode instruction : legacy.instructions) {
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == GETSTATIC
                    && field.desc.equals(ACCESSOR_DESC)) {
                accessorReads.add(field);
            }
        }
        // The original method reads DYE_COLOR once, then DATA_CLIENT_FLAGS twice.
        if (accessorReads.size() != 3 || !accessorReads.get(0).name.equals("DYE_COLOR")) {
            throw new IllegalStateException("Palladium SuitStand defineSynchedData no longer has the expected three accessor reads");
        }
        FieldInsnNode flagAccessor = accessorReads.get(1);
        FieldInsnNode repeatedFlagAccessor = accessorReads.get(2);
        if (!flagAccessor.name.equals(repeatedFlagAccessor.name)
                || !flagAccessor.owner.equals(repeatedFlagAccessor.owner)
                || !flagAccessor.desc.equals(repeatedFlagAccessor.desc)) {
            throw new IllegalStateException("Palladium SuitStand DATA_CLIENT_FLAGS accessor shape changed");
        }

        AbstractInsnNode entityDataRead = previousReal(flagAccessor);
        AbstractInsnNode thisLoad = entityDataRead == null ? null : previousReal(entityDataRead);
        if (!(entityDataRead instanceof FieldInsnNode entityDataField)
                || entityDataField.getOpcode() != GETFIELD
                || !entityDataField.desc.equals(SYNCHED_DATA_DESC)
                || !(thisLoad instanceof VarInsnNode loadThis)
                || loadThis.getOpcode() != ALOAD
                || loadThis.var != 0) {
            throw new IllegalStateException("Palladium SuitStand DATA_CLIENT_FLAGS mutation no longer begins with this.entityData");
        }

        AbstractInsnNode returnInstruction = null;
        int returns = 0;
        int getCalls = 0;
        int setCalls = 0;
        int setBitCalls = 0;
        for (AbstractInsnNode instruction : legacy.instructions) {
            if (instruction.getOpcode() == RETURN) {
                returnInstruction = instruction;
                returns++;
            }
        }
        if (returns != 1 || returnInstruction == null) {
            throw new IllegalStateException("Palladium SuitStand legacy entity-data method no longer has one return");
        }

        for (AbstractInsnNode instruction = thisLoad; instruction != null && instruction != returnInstruction; instruction = instruction.getNext()) {
            if (instruction instanceof MethodInsnNode call) {
                if (call.owner.equals(SYNCHED_DATA)
                        && call.desc.equals("(L" + ACCESSOR + ";)Ljava/lang/Object;")) {
                    getCalls++;
                } else if (call.owner.equals(SYNCHED_DATA)
                        && call.desc.equals("(L" + ACCESSOR + ";Ljava/lang/Object;)V")) {
                    setCalls++;
                } else if (call.owner.equals(node.name) && call.name.equals("setBit") && call.desc.equals("(BIZ)B")) {
                    setBitCalls++;
                }
            }
            if (instruction.getOpcode() == GOTO || instruction.getOpcode() == IFNULL || instruction.getOpcode() == IFNONNULL
                    || instruction.getOpcode() == IFEQ || instruction.getOpcode() == IFNE
                    || instruction.getOpcode() == IF_ICMPEQ || instruction.getOpcode() == IF_ICMPNE) {
                throw new IllegalStateException("Palladium SuitStand DATA_CLIENT_FLAGS setup unexpectedly contains control flow");
            }
        }
        if (getCalls != 1 || setCalls != 1 || setBitCalls != 1) {
            throw new IllegalStateException("Palladium SuitStand DATA_CLIENT_FLAGS setup no longer matches the verified get/setBit/set sequence");
        }

        InsnList copied = new InsnList();
        for (AbstractInsnNode instruction = thisLoad; instruction != null && instruction != returnInstruction; instruction = instruction.getNext()) {
            // The verified block has no branches. Copy executable instructions only,
            // so the constructor does not inherit stale line-number/frame metadata.
            if (instruction.getOpcode() >= 0) copied.add(instruction.clone(java.util.Map.of()));
        }
        if (copied.size() == 0) {
            throw new IllegalStateException("Palladium SuitStand DATA_CLIENT_FLAGS setup was empty");
        }
        return copied;
    }

    private static AbstractInsnNode previousReal(AbstractInsnNode instruction) {
        AbstractInsnNode previous = instruction == null ? null : instruction.getPrevious();
        while (previous != null && previous.getOpcode() < 0) previous = previous.getPrevious();
        return previous;
    }

    private static MethodInsnNode findArmorStandConstructor(MethodNode constructor) {
        MethodInsnNode found = null;
        for (AbstractInsnNode instruction : constructor.instructions) {
            if (!(instruction instanceof MethodInsnNode call)
                    || call.getOpcode() != INVOKESPECIAL
                    || !call.owner.equals(ARMOR_STAND)
                    || !call.name.equals("<init>")
                    || !call.desc.equals(CONSTRUCTOR_DESC)) {
                continue;
            }
            if (found != null) throw new IllegalStateException("Palladium SuitStand constructor calls ArmorStand more than once");
            found = call;
        }
        return found;
    }

    private static boolean hasSetBitCall(MethodNode method, String owner) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(owner)
                    && call.name.equals("setBit")
                    && call.desc.equals("(BIZ)B")) return true;
        }
        return false;
    }

    private static MethodNode find(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) return method;
        }
        return null;
    }
}
