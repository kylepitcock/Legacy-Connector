package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Adds the 1.21.1 VertexConsumer position entry point to AlienEvo's buffered glow consumer.
 *
 * The legacy consumer stores the position in vertex(double,double,double), then stores
 * color/UV/light/normal attributes. Its endVertex() method forwards the solid vertex and
 * records the complete 14-float vertex for later glow replay. The new API ends the
 * attribute chain at setNormal(), so this adapter flushes that same legacy path there.
 */
final class AlienEvoBufferingGlowConsumer1201 implements Opcodes {
    static final String TARGET = "net/habb/alienevo/client/renderer/BufferingGlowConsumer";
    static final String VERTEX = "com/mojang/blaze3d/vertex/VertexConsumer";
    static final String LEGACY_END =
            "org/kylepitcock/legacyconnector/v1_20_1/migrate/LegacyEndVertexConsumer";
    static final String RENDER_TYPE = "net/minecraft/client/renderer/RenderType";
    static final String MULTI_BUFFER_SOURCE = "net/minecraft/client/renderer/MultiBufferSource";
    static final String BUFFER_REFRESH =
            "org/kylepitcock/legacyconnector/v1_20_1/migrate/AlienEvoBufferRefresh1201";

    private static final String SOLID_TYPE_DESC = "(L" + RENDER_TYPE + ";)V";
    private static final String REFRESH_SOLID_DESC = "(L" + MULTI_BUFFER_SOURCE + ";)V";
    private static final String POSITION_DESC = "(DDD)L" + VERTEX + ";";
    private static final String NEW_POSITION_DESC = "(FFF)L" + VERTEX + ";";
    private static final String ATTRIBUTE_DESC = "(FFF)L" + VERTEX + ";";

    private AlienEvoBufferingGlowConsumer1201() {}

    /** Returns the number of class/bytecode changes. Fails closed on an unknown class shape. */
    static int apply(ClassNode node) {
        if (!TARGET.equals(node.name)) return 0;

        if (!"java/lang/Object".equals(node.superName) || !node.interfaces.contains(VERTEX)) {
            throw new IllegalStateException("Unexpected BufferingGlowConsumer class hierarchy");
        }

        FieldNode solidField = requireField(node, "solid", "L" + VERTEX + ";");
        requireField(node, "glowType", "L" + RENDER_TYPE + ";");
        requireField(node, "recordedVertices", "Ljava/util/List;");
        MethodNode constructor = requireMethod(node, "<init>",
                "(L" + VERTEX + ";L" + RENDER_TYPE + ";)V");
        requireFieldWrites(constructor, node.name, "solid", "glowType");
        requireField(node, "vx", "D");
        requireField(node, "vy", "D");
        requireField(node, "vz", "D");
        requireField(node, "vnx", "F");
        requireField(node, "vny", "F");
        requireField(node, "vnz", "F");

        MethodNode position = requireMethod(node, "vertex", POSITION_DESC);
        MethodNode endVertex = requireMethod(node, "endVertex", "()V");
        MethodNode normal = requireAliasedMethod(node, "normal", "setNormal", ATTRIBUTE_DESC);

        requireFieldWrites(position, node.name, "vx", "vy", "vz");
        requireFieldWrites(normal, node.name, "vnx", "vny", "vnz");
        if (!callsListAdd(endVertex)) {
            throw new IllegalStateException("BufferingGlowConsumer.endVertex no longer records glow vertices");
        }
        if (countOpcode(normal, ARETURN) != 1) {
            throw new IllegalStateException("Unexpected BufferingGlowConsumer normal return shape");
        }

        MethodNode addVertex = findUnique(node, "addVertex", NEW_POSITION_DESC);
        FieldNode pending = findField(node, "legacyPendingVertex", "Z");
        MethodNode flush = findUnique(node, "legacyFlushPending", "()V");
        MethodNode legacyEnd = findUnique(node, "legacyEndVertex", "()V");
        boolean markerInterface = node.interfaces.contains(LEGACY_END);
        boolean vertexPartAny = addVertex != null || pending != null || flush != null
                || legacyEnd != null || markerInterface;
        boolean vertexPartComplete = addVertex != null && pending != null && flush != null
                && legacyEnd != null && markerInterface
                && calls(normal, node.name, "legacyFlushPending", "()V");
        if (vertexPartAny && !vertexPartComplete) {
            throw new IllegalStateException("Partial BufferingGlowConsumer VertexConsumer adapter state detected");
        }

        FieldNode solidTypeField = findField(node, "legacySolidType", "L" + RENDER_TYPE + ";");
        MethodNode setSolidType = findUnique(node, "legacySetSolidType", SOLID_TYPE_DESC);
        MethodNode refreshSolid = findUnique(node, "legacyRefreshSolid", REFRESH_SOLID_DESC);
        boolean solidMutable = (solidField.access & ACC_FINAL) == 0;
        boolean refreshPartAny = solidTypeField != null || setSolidType != null || refreshSolid != null || solidMutable;
        boolean refreshPartComplete = solidTypeField != null && setSolidType != null
                && refreshSolid != null && solidMutable;
        if (refreshPartAny && !refreshPartComplete) {
            throw new IllegalStateException("Partial BufferingGlowConsumer solid-delegate refresh state detected");
        }
        if (refreshPartComplete
                && (!calls(refreshSolid, BUFFER_REFRESH, "needsRefresh", "(L" + VERTEX + ";)Z")
                || !calls(refreshSolid, MULTI_BUFFER_SOURCE, "getBuffer",
                        "(L" + RENDER_TYPE + ";)L" + VERTEX + ";")
                || !hasFieldWrite(refreshSolid, node.name, "solid"))) {
            throw new IllegalStateException("BufferingGlowConsumer solid refresh body no longer matches its contract");
        }

        int changes = 0;
        if (!vertexPartComplete) {
            if (!"setNormal".equals(normal.name)) {
                normal.name = "setNormal";
                changes++;
            }
            changes += renameIfPresent(node, "color", "(IIII)L" + VERTEX + ";", "setColor");
            changes += renameIfPresent(node, "uv", "(FF)L" + VERTEX + ";", "setUv");
            changes += renameIfPresent(node, "overlayCoords", "(II)L" + VERTEX + ";", "setUv1");
            changes += renameIfPresent(node, "uv2", "(II)L" + VERTEX + ";", "setUv2");

            if (!node.interfaces.contains(LEGACY_END)) {
                node.interfaces.add(LEGACY_END);
                changes++;
            }
            node.fields.add(new FieldNode(ACC_PRIVATE | ACC_SYNTHETIC, "legacyPendingVertex", "Z", null, null));
            changes++;

            node.methods.add(legacyFlushPending(node.name));
            changes++;
            node.methods.add(legacyEndVertex(node.name));
            changes++;
            node.methods.add(addVertexBridge(node.name));
            changes++;

            for (AbstractInsnNode instruction : normal.instructions.toArray()) {
                if (instruction.getOpcode() == ARETURN) {
                    InsnList flushAfterNormal = new InsnList();
                    flushAfterNormal.add(new VarInsnNode(ALOAD, 0));
                    flushAfterNormal.add(new MethodInsnNode(INVOKESPECIAL, node.name,
                            "legacyFlushPending", "()V", false));
                    normal.instructions.insertBefore(instruction, flushAfterNormal);
                    changes++;
                }
            }
        }

        if (!refreshPartComplete) {
            solidField.access &= ~ACC_FINAL;
            node.fields.add(new FieldNode(ACC_PRIVATE | ACC_SYNTHETIC,
                    "legacySolidType", "L" + RENDER_TYPE + ";", null, null));
            changes++;
            node.methods.add(legacySetSolidType(node.name));
            changes++;
            node.methods.add(legacyRefreshSolid(node.name));
            changes++;
        }

        return changes;
    }

    private static MethodNode legacySetSolidType(String owner) {
        MethodNode method = new MethodNode(ACC_PUBLIC | ACC_SYNTHETIC,
                "legacySetSolidType", SOLID_TYPE_DESC, null, null);
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new VarInsnNode(ALOAD, 1));
        method.instructions.add(new FieldInsnNode(PUTFIELD, owner,
                "legacySolidType", "L" + RENDER_TYPE + ";"));
        method.instructions.add(new InsnNode(RETURN));
        method.maxLocals = 2;
        method.maxStack = 2;
        return method;
    }

    private static MethodNode legacyRefreshSolid(String owner) {
        MethodNode method = new MethodNode(ACC_PUBLIC | ACC_SYNTHETIC,
                "legacyRefreshSolid", REFRESH_SOLID_DESC, null, null);
        LabelNode done = new LabelNode();
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new FieldInsnNode(GETFIELD, owner,
                "legacySolidType", "L" + RENDER_TYPE + ";"));
        method.instructions.add(new JumpInsnNode(IFNULL, done));
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new FieldInsnNode(GETFIELD, owner, "solid", "L" + VERTEX + ";"));
        method.instructions.add(new MethodInsnNode(INVOKESTATIC, BUFFER_REFRESH,
                "needsRefresh", "(L" + VERTEX + ";)Z", false));
        method.instructions.add(new JumpInsnNode(IFEQ, done));
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new VarInsnNode(ALOAD, 1));
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new FieldInsnNode(GETFIELD, owner,
                "legacySolidType", "L" + RENDER_TYPE + ";"));
        method.instructions.add(new MethodInsnNode(INVOKEINTERFACE, MULTI_BUFFER_SOURCE,
                "getBuffer", "(L" + RENDER_TYPE + ";)L" + VERTEX + ";", true));
        method.instructions.add(new FieldInsnNode(PUTFIELD, owner, "solid", "L" + VERTEX + ";"));
        method.instructions.add(done);
        method.instructions.add(new InsnNode(RETURN));
        method.maxLocals = 2;
        method.maxStack = 3;
        return method;
    }

    private static MethodNode addVertexBridge(String owner) {
        MethodNode method = new MethodNode(ACC_PUBLIC | ACC_SYNTHETIC,
                "addVertex", NEW_POSITION_DESC, null, null);
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new MethodInsnNode(INVOKESPECIAL, owner,
                "legacyFlushPending", "()V", false));

        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new VarInsnNode(FLOAD, 1));
        method.instructions.add(new InsnNode(F2D));
        method.instructions.add(new VarInsnNode(FLOAD, 2));
        method.instructions.add(new InsnNode(F2D));
        method.instructions.add(new VarInsnNode(FLOAD, 3));
        method.instructions.add(new InsnNode(F2D));
        method.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, owner,
                "vertex", POSITION_DESC, false));
        method.instructions.add(new InsnNode(POP));

        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new InsnNode(ICONST_1));
        method.instructions.add(new FieldInsnNode(PUTFIELD, owner, "legacyPendingVertex", "Z"));
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new InsnNode(ARETURN));
        method.maxLocals = 4;
        method.maxStack = 7;
        return method;
    }

    private static MethodNode legacyFlushPending(String owner) {
        MethodNode method = new MethodNode(ACC_PRIVATE | ACC_SYNTHETIC,
                "legacyFlushPending", "()V", null, null);
        LabelNode done = new LabelNode();
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new FieldInsnNode(GETFIELD, owner, "legacyPendingVertex", "Z"));
        method.instructions.add(new JumpInsnNode(IFEQ, done));
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

    private static MethodNode legacyEndVertex(String owner) {
        MethodNode method = new MethodNode(ACC_PUBLIC | ACC_SYNTHETIC,
                "legacyEndVertex", "()V", null, null);
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new MethodInsnNode(INVOKESPECIAL, owner, "legacyFlushPending", "()V", false));
        method.instructions.add(new InsnNode(RETURN));
        method.maxLocals = 1;
        method.maxStack = 1;
        return method;
    }

    private static int renameIfPresent(ClassNode node, String oldName, String desc, String newName) {
        MethodNode oldMethod = findUnique(node, oldName, desc);
        MethodNode newMethod = findUnique(node, newName, desc);
        if (oldMethod != null && newMethod != null) {
            throw new IllegalStateException("Both legacy and modern VertexConsumer methods exist: " + oldName);
        }
        if (oldMethod == null) {
            if (newMethod == null) throw new IllegalStateException("Missing VertexConsumer method " + oldName + desc);
            return 0;
        }
        oldMethod.name = newName;
        return 1;
    }

    private static MethodNode requireAliasedMethod(ClassNode node, String oldName, String newName, String desc) {
        MethodNode oldMethod = findUnique(node, oldName, desc);
        MethodNode newMethod = findUnique(node, newName, desc);
        if (oldMethod != null && newMethod != null) {
            throw new IllegalStateException("Duplicate VertexConsumer normal methods");
        }
        if (oldMethod == null && newMethod == null) {
            throw new IllegalStateException("Missing BufferingGlowConsumer normal method");
        }
        return oldMethod != null ? oldMethod : newMethod;
    }

    private static MethodNode requireMethod(ClassNode node, String name, String desc) {
        MethodNode method = findUnique(node, name, desc);
        if (method == null) throw new IllegalStateException("Missing BufferingGlowConsumer method " + name + desc);
        return method;
    }

    private static MethodNode findUnique(ClassNode node, String name, String desc) {
        MethodNode found = null;
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) {
                if (found != null) throw new IllegalStateException("Duplicate method " + name + desc);
                found = method;
            }
        }
        return found;
    }

    private static FieldNode findField(ClassNode node, String name, String desc) {
        FieldNode found = null;
        for (FieldNode field : node.fields) {
            if (field.name.equals(name) && field.desc.equals(desc)) {
                if (found != null) throw new IllegalStateException("Duplicate field " + name + desc);
                found = field;
            }
        }
        return found;
    }

    private static FieldNode requireField(ClassNode node, String name, String desc) {
        FieldNode field = findField(node, name, desc);
        if (field == null) {
            throw new IllegalStateException("Missing BufferingGlowConsumer field " + name + ":" + desc);
        }
        return field;
    }

    private static void requireFieldWrites(MethodNode method, String owner, String... fields) {
        for (String field : fields) {
            int writes = 0;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof FieldInsnNode fieldInsn
                        && fieldInsn.getOpcode() == PUTFIELD
                        && fieldInsn.owner.equals(owner) && fieldInsn.name.equals(field)) {
                    writes++;
                }
            }
            if (writes != 1) {
                throw new IllegalStateException("Unexpected " + method.name + " write count for " + field);
            }
        }
    }

    private static boolean hasFieldWrite(MethodNode method, String owner, String name) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field && field.getOpcode() == PUTFIELD
                    && field.owner.equals(owner) && field.name.equals(name)) return true;
        }
        return false;
    }

    private static boolean callsListAdd(MethodNode method) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals("java/util/List")
                    && call.name.equals("add") && call.desc.equals("(Ljava/lang/Object;)Z")) {
                return true;
            }
        }
        return false;
    }

    private static boolean calls(MethodNode method, String owner, String name, String desc) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner)
                    && call.name.equals(name) && call.desc.equals(desc)) {
                return true;
            }
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
