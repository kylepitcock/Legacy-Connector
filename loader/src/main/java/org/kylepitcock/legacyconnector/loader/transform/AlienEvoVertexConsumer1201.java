package org.kylepitcock.legacyconnector.loader.transform;

import java.util.LinkedHashSet;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Migrates AlienEvo's flat loot-effect consumer away from the removed
 * DefaultedVertexConsumer superclass onto the 1.21.1 VertexConsumer interface.
 */
final class AlienEvoVertexConsumer1201 implements Opcodes {
    static final String FLAT = "net/habb/alienevo/client/renderer/ModuleLootItemFx$Flat";
    static final String DEFAULTED = "com/mojang/blaze3d/vertex/DefaultedVertexConsumer";
    static final String VERTEX = "com/mojang/blaze3d/vertex/VertexConsumer";
    static final String HELPER = "org/kylepitcock/legacyconnector/v1_20_1/migrate/VertexCompat";
    static final String LEGACY_END = "org/kylepitcock/legacyconnector/v1_20_1/migrate/LegacyEndVertexConsumer";

    private static final String COLOR_DESC = "(IIII)V";

    private AlienEvoVertexConsumer1201() {}

    /** Returns the number of structural and bytecode changes. */
    static int apply(ClassNode node) {
        if (!FLAT.equals(node.name) || !DEFAULTED.equals(node.superName)) return 0;

        int changes = 0;
        node.superName = "java/lang/Object";
        if (!node.interfaces.contains(VERTEX)) node.interfaces.add(VERTEX);
        if (!node.interfaces.contains(LEGACY_END)) node.interfaces.add(LEGACY_END);
        changes += 3;
        if (node.fields.stream().noneMatch(field -> field.name.equals("legacyPendingVertex") && field.desc.equals("Z"))) {
            node.fields.add(new FieldNode(ACC_PRIVATE | ACC_SYNTHETIC, "legacyPendingVertex", "Z", null, null));
            changes++;
        }

        Set<String> inheritedColorFields = new LinkedHashSet<>();
        boolean needsDefaultInitializer = false;

        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof FieldInsnNode field
                        && field.owner.equals(DEFAULTED) && field.desc.equals("I")) {
                    inheritedColorFields.add(field.name);
                    field.owner = node.name;
                    changes++;
                } else if (instruction instanceof MethodInsnNode call) {
                    if (call.owner.equals(DEFAULTED) && call.name.equals("<init>") && call.desc.equals("()V")) {
                        call.owner = "java/lang/Object";
                        changes++;
                    } else if (call.owner.equals(DEFAULTED) && call.desc.equals(COLOR_DESC)) {
                        call.setOpcode(INVOKESPECIAL);
                        call.owner = node.name;
                        call.name = "alienevo$initializeDefaultColor";
                        call.itf = false;
                        needsDefaultInitializer = true;
                        changes++;
                    } else if (call.owner.equals(VERTEX)) {
                        changes += migrateDelegateCall(method, call);
                    }
                }
            }
        }

        for (String field : inheritedColorFields) {
            boolean present = false;
            for (FieldNode existing : node.fields) {
                if (existing.name.equals(field) && existing.desc.equals("I")) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                node.fields.add(new FieldNode(ACC_PRIVATE | ACC_SYNTHETIC, field, "I", null, null));
                changes++;
            }
        }

        // The old 1.20.1 class inherited defaultColor storage. Its constructor's
        // invokespecial call now targets this private replacement.
        if (needsDefaultInitializer && find(node, "alienevo$initializeDefaultColor", COLOR_DESC) == null) {
            node.methods.add(defaultColorInitializer(node.name, inheritedColorFields));
            changes++;
        }

        // The new interface's first abstract method is addVertex(float,float,float).
        // Keep the legacy double-precision implementation as an internal helper
        // and expose the 1.21.1 float descriptor through this bridge.
        if (find(node, "addVertex", "(FFF)L" + VERTEX + ";") == null) {
            node.methods.add(addVertexBridge(node.name));
            changes++;
        }
        if (find(node, "endVertex", "()V") != null) {
            if (find(node, "legacyFlushPending", "()V") == null) {
                node.methods.add(legacyFlushPendingBridge(node.name));
                changes++;
            }
            if (find(node, "legacyEndVertex", "()V") == null) {
                node.methods.add(legacyEndVertexBridge(node.name));
                changes++;
            }
        }

        // The old concrete overrides now have the 1.21.1 method names. Keep
        // descriptors and bodies intact: their operations already match.
        changes += rename(node, "color", "(IIII)L" + VERTEX + ";", "setColor");
        changes += rename(node, "uv", "(FF)L" + VERTEX + ";", "setUv");
        changes += rename(node, "overlayCoords", "(II)L" + VERTEX + ";", "setUv1");
        changes += rename(node, "uv2", "(II)L" + VERTEX + ";", "setUv2");
        changes += rename(node, "normal", "(FFF)L" + VERTEX + ";", "setNormal");
        if (flushAfterNormal(node)) changes++;

        return changes;
    }

    private static int migrateDelegateCall(MethodNode method, MethodInsnNode call) {
        if (call.name.equals("vertex") && call.desc.equals("(DDD)L" + VERTEX + ";")) {
            call.setOpcode(INVOKESTATIC);
            call.owner = HELPER;
            call.name = "vertex";
            call.desc = "(L" + VERTEX + ";DDD)L" + VERTEX + ";";
            call.itf = false;
            return 1;
        }
        if (call.name.equals("vertex") && call.desc.equals("(FFFFFFFFFIIFFF)V")) {
            call.setOpcode(INVOKESTATIC);
            call.owner = HELPER;
            call.name = "vertex";
            call.desc = "(L" + VERTEX + ";FFFFFFFFFIIFFF)V";
            call.itf = false;
            return 1;
        }
        if (call.name.equals("color") && call.desc.equals("(IIII)L" + VERTEX + ";")) {
            call.name = "setColor";
            return 1;
        }
        if (call.name.equals("uv") && call.desc.equals("(FF)L" + VERTEX + ";")) {
            call.name = "setUv";
            return 1;
        }
        if (call.name.equals("overlayCoords") && call.desc.equals("(II)L" + VERTEX + ";")) {
            call.name = "setUv1";
            return 1;
        }
        if (call.name.equals("uv2") && call.desc.equals("(II)L" + VERTEX + ";")) {
            call.name = "setUv2";
            return 1;
        }
        if (call.name.equals("normal") && call.desc.equals("(FFF)L" + VERTEX + ";")) {
            call.name = "setNormal";
            return 1;
        }
        if (call.name.equals("endVertex") && call.desc.equals("()V")) {
            call.setOpcode(INVOKESTATIC);
            call.owner = HELPER;
            call.name = "endVertex";
            call.desc = "(L" + VERTEX + ";)V";
            call.itf = false;
            return 1;
        }
        return 0;
    }

    private static int rename(ClassNode node, String oldName, String descriptor, String newName) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(oldName) && method.desc.equals(descriptor)) {
                method.name = newName;
                return 1;
            }
        }
        return 0;
    }

    private static MethodNode find(ClassNode node, String name, String descriptor) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(descriptor)) return method;
        }
        return null;
    }

    private static MethodNode defaultColorInitializer(String owner, Set<String> fields) {
        MethodNode method = new MethodNode(ACC_PRIVATE | ACC_SYNTHETIC,
                "alienevo$initializeDefaultColor", COLOR_DESC, null, null);
        int local = 1;
        for (String field : fields) {
            method.instructions.add(new VarInsnNode(ALOAD, 0));
            method.instructions.add(new VarInsnNode(ILOAD, local++));
            method.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(PUTFIELD, owner, field, "I"));
        }
        method.instructions.add(new InsnNode(RETURN));
        method.maxLocals = 5;
        method.maxStack = 2;
        return method;
    }

    private static MethodNode legacyEndVertexBridge(String owner) {
        MethodNode method = new MethodNode(ACC_PUBLIC | ACC_SYNTHETIC, "legacyEndVertex", "()V", null, null);
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, owner, "legacyFlushPending", "()V", false));
        method.instructions.add(new InsnNode(RETURN));
        method.maxLocals = 1;
        method.maxStack = 1;
        return method;
    }

    private static MethodNode legacyFlushPendingBridge(String owner) {
        MethodNode method = new MethodNode(ACC_PRIVATE | ACC_SYNTHETIC, "legacyFlushPending", "()V", null, null);
        org.objectweb.asm.tree.LabelNode done = new org.objectweb.asm.tree.LabelNode();
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new FieldInsnNode(GETFIELD, owner, "legacyPendingVertex", "Z"));
        method.instructions.add(new org.objectweb.asm.tree.JumpInsnNode(IFEQ, done));
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, owner, "endVertex", "()V", false));
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new InsnNode(ICONST_0));
        method.instructions.add(new FieldInsnNode(PUTFIELD, owner, "legacyPendingVertex", "Z"));
        method.instructions.add(done);
        method.instructions.add(new InsnNode(RETURN));
        method.maxLocals = 1;
        method.maxStack = 2;
        return method;
    }

    private static boolean flushAfterNormal(ClassNode node) {
        MethodNode normal = find(node, "setNormal", "(FFF)L" + VERTEX + ";");
        if (normal == null) return false;
        boolean changed = false;
        for (AbstractInsnNode instruction : normal.instructions.toArray()) {
            if (instruction.getOpcode() == ARETURN) {
                InsnList flush = new InsnList();
                flush.add(new VarInsnNode(ALOAD, 0));
                flush.add(new MethodInsnNode(INVOKESPECIAL, node.name, "legacyFlushPending", "()V", false));
                normal.instructions.insertBefore(instruction, flush);
                changed = true;
            }
        }
        return changed;
    }

    private static MethodNode addVertexBridge(String owner) {
        MethodNode method = new MethodNode(ACC_PUBLIC, "addVertex",
                "(FFF)L" + VERTEX + ";", null, null);
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new MethodInsnNode(INVOKESPECIAL, owner, "legacyFlushPending", "()V", false));
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new VarInsnNode(FLOAD, 1));
        method.instructions.add(new InsnNode(F2D));
        method.instructions.add(new VarInsnNode(FLOAD, 2));
        method.instructions.add(new InsnNode(F2D));
        method.instructions.add(new VarInsnNode(FLOAD, 3));
        method.instructions.add(new InsnNode(F2D));
        method.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, owner, "vertex",
                "(DDD)L" + VERTEX + ";", false));
        method.instructions.add(new InsnNode(DUP));
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new InsnNode(ICONST_1));
        method.instructions.add(new FieldInsnNode(PUTFIELD, owner, "legacyPendingVertex", "Z"));
        method.instructions.add(new InsnNode(ARETURN));
        method.maxLocals = 4;
        method.maxStack = 7;
        return method;
    }
}
