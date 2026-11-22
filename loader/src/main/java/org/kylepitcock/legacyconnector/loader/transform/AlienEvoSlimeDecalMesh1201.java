package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Prevents AlienEvo's slime-decal bake path from closing or uploading the null
 * MeshData returned by the 1.21.1 builder when a cell emitted no vertices.
 */
final class AlienEvoSlimeDecalMesh1201 implements Opcodes {
    static final String TARGET = "net/habb/alienevo/client/decal/SlimeDecalRenderer";
    static final String BUILDER = "org/kylepitcock/legacyconnector/v1_20_1/migrate/CompatBufferBuilder";
    static final String MESH_DATA = "com/mojang/blaze3d/vertex/MeshData";
    static final String BAKE_DESC =
            "(Lcom/mojang/blaze3d/vertex/VertexBuffer;"
                    + "Lnet/habb/alienevo/client/decal/ClientSlimeDecals$Cell;"
                    + "BDLnet/minecraft/world/phys/Vec3;)Z";
    static final String END_DESC = "()L" + MESH_DATA + ";";
    private static final int ANY_LOCAL = 7;
    private static final int RENDERED_LOCAL = 8;

    private AlienEvoSlimeDecalMesh1201() {}

    static int apply(ClassNode node) {
        if (!TARGET.equals(node.name)) return 0;

        MethodNode bake = null;
        for (MethodNode method : node.methods) {
            if (!"bake".equals(method.name) || !BAKE_DESC.equals(method.desc)) continue;
            if (bake != null) {
                throw new IllegalStateException("AlienEvo SlimeDecalRenderer has duplicate bake methods");
            }
            bake = method;
        }
        if (bake == null) {
            throw new IllegalStateException("AlienEvo SlimeDecalRenderer no longer declares the inspected bake method");
        }

        MethodInsnNode end = null;
        int endCalls = 0;
        for (AbstractInsnNode instruction : bake.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode call)
                    || !BUILDER.equals(call.owner) || !"end".equals(call.name)) continue;
            if (call.getOpcode() != INVOKEVIRTUAL || !END_DESC.equals(call.desc)) {
                throw new IllegalStateException("AlienEvo SlimeDecalRenderer bake builder end call has an unknown descriptor");
            }
            end = call;
            endCalls++;
        }
        if (endCalls != 1) {
            throw new IllegalStateException(
                    "AlienEvo SlimeDecalRenderer bake expected exactly one CompatBufferBuilder.end() call; found "
                            + endCalls);
        }

        AbstractInsnNode stored = nextOpcode(end);
        if (!(stored instanceof VarInsnNode store)
                || store.getOpcode() != ASTORE || store.var != RENDERED_LOCAL) {
            throw new IllegalStateException(
                    "AlienEvo SlimeDecalRenderer bake no longer stores the end() result in local 8");
        }

        if (hasNullGuard(bake, store)) return 0;

        AbstractInsnNode originalNext = nextOpcode(store);
        if (!(originalNext instanceof VarInsnNode any)
                || any.getOpcode() != ILOAD || any.var != ANY_LOCAL) {
            throw new IllegalStateException(
                    "AlienEvo SlimeDecalRenderer bake changed the inspected emitted-vertices branch");
        }

        LabelNode nonNull = new LabelNode();
        InsnList guard = new InsnList();
        guard.add(new VarInsnNode(ALOAD, RENDERED_LOCAL));
        guard.add(new JumpInsnNode(IFNONNULL, nonNull));
        guard.add(new InsnNode(ICONST_0));
        guard.add(new InsnNode(IRETURN));
        guard.add(nonNull);
        bake.instructions.insert(store, guard);
        return 1;
    }

    private static boolean hasNullGuard(MethodNode method, VarInsnNode store) {
        AbstractInsnNode loadNode = nextOpcode(store);
        if (!(loadNode instanceof VarInsnNode load)
                || load.getOpcode() != ALOAD || load.var != RENDERED_LOCAL) {
            return false;
        }

        AbstractInsnNode branchNode = nextOpcode(loadNode);
        if (!(branchNode instanceof JumpInsnNode branch) || branch.getOpcode() != IFNONNULL) {
            throw new IllegalStateException("AlienEvo SlimeDecalRenderer bake has a partial MeshData null guard");
        }
        AbstractInsnNode zero = nextOpcode(branchNode);
        AbstractInsnNode returnFalse = zero == null ? null : nextOpcode(zero);
        if (zero == null || zero.getOpcode() != ICONST_0
                || returnFalse == null || returnFalse.getOpcode() != IRETURN
                || branch.label == null || method.instructions.indexOf(branch.label) < 0) {
            throw new IllegalStateException("AlienEvo SlimeDecalRenderer bake has a malformed MeshData null guard");
        }

        AbstractInsnNode afterGuard = nextOpcode(branch.label);
        if (!(afterGuard instanceof VarInsnNode any)
                || any.getOpcode() != ILOAD || any.var != ANY_LOCAL) {
            throw new IllegalStateException("AlienEvo SlimeDecalRenderer MeshData null guard targets an unexpected block");
        }
        return true;
    }

    private static AbstractInsnNode nextOpcode(AbstractInsnNode instruction) {
        for (AbstractInsnNode next = instruction.getNext(); next != null; next = next.getNext()) {
            if (next.getOpcode() >= 0) return next;
        }
        return null;
    }
}
