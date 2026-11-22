package org.kylepitcock.legacyconnector.loader.transform;

import org.kylepitcock.legacyconnector.loader.transform.compat.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Regression tests for AlienEvo's nullable 1.21.1 slime-decal mesh result. */
class AlienEvoSlimeDecalMesh1201Test implements Opcodes {

    private static ClassNode renderer() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.name = AlienEvoSlimeDecalMesh1201.TARGET;
        node.superName = "java/lang/Object";

        MethodNode bake = new MethodNode(ACC_PRIVATE | ACC_STATIC, "bake",
                AlienEvoSlimeDecalMesh1201.BAKE_DESC, null, null);
        LabelNode verticesEmitted = new LabelNode();
        bake.instructions.add(new InsnNode(ACONST_NULL)); // CompatBufferBuilder receiver
        bake.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                AlienEvoSlimeDecalMesh1201.BUILDER, "end",
                AlienEvoSlimeDecalMesh1201.END_DESC, false));
        bake.instructions.add(new VarInsnNode(ASTORE, 8)); // rendered
        bake.instructions.add(new VarInsnNode(ILOAD, 7)); // any
        bake.instructions.add(new JumpInsnNode(IFNE, verticesEmitted));
        bake.instructions.add(new VarInsnNode(ALOAD, 8));
        bake.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                AlienEvoSlimeDecalMesh1201.MESH_DATA, "close", "()V", false));
        bake.instructions.add(new InsnNode(ICONST_0));
        bake.instructions.add(new InsnNode(IRETURN));
        bake.instructions.add(verticesEmitted);
        bake.instructions.add(new InsnNode(ICONST_1));
        bake.instructions.add(new InsnNode(IRETURN));
        bake.maxStack = 1;
        bake.maxLocals = 9;
        node.methods.add(bake);
        return node;
    }

    @Test
    void returnsFalseBeforeClosingOrUploadingAnEmptyMeshResult() {
        ClassNode node = renderer();

        assertEquals(1, AlienEvoSlimeDecalMesh1201.apply(node));

        MethodNode bake = node.methods.get(0);
        VarInsnNode store = null;
        for (AbstractInsnNode instruction : bake.instructions.toArray()) {
            if (instruction instanceof VarInsnNode variable
                    && variable.getOpcode() == ASTORE && variable.var == 8) {
                store = variable;
                break;
            }
        }
        assertInstanceOf(VarInsnNode.class, nextOpcode(store));
        VarInsnNode load = (VarInsnNode) nextOpcode(store);
        assertEquals(ALOAD, load.getOpcode());
        assertEquals(8, load.var);

        JumpInsnNode nonNull = (JumpInsnNode) nextOpcode(load);
        assertEquals(IFNONNULL, nonNull.getOpcode());
        assertEquals(ICONST_0, nextOpcode(nonNull).getOpcode());
        assertEquals(IRETURN, nextOpcode(nextOpcode(nonNull)).getOpcode());
        assertTrue(bake.instructions.indexOf(nonNull.label) >= 0);
        AbstractInsnNode originalBranch = nextOpcode(nonNull.label);
        assertInstanceOf(VarInsnNode.class, originalBranch);
        assertEquals(ILOAD, originalBranch.getOpcode());
        assertEquals(7, ((VarInsnNode) originalBranch).var);

        long closeCalls = java.util.Arrays.stream(bake.instructions.toArray())
                .filter(instruction -> instruction instanceof MethodInsnNode call
                        && call.owner.equals(AlienEvoSlimeDecalMesh1201.MESH_DATA)
                        && call.name.equals("close"))
                .count();
        assertEquals(1, closeCalls);
        assertEquals(0, AlienEvoSlimeDecalMesh1201.apply(node));
    }

    @Test
    void failsClosedWhenTheInspectedBuilderEndDescriptorChanges() {
        ClassNode node = renderer();
        for (AbstractInsnNode instruction : node.methods.get(0).instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.name.equals("end")) {
                call.desc = "()Ljava/lang/Object;";
            }
        }
        assertThrows(IllegalStateException.class, () -> AlienEvoSlimeDecalMesh1201.apply(node));
    }

    @Test
    void failsClosedWhenTheRenderedLocalChanges() {
        ClassNode node = renderer();
        for (AbstractInsnNode instruction : node.methods.get(0).instructions.toArray()) {
            if (instruction instanceof VarInsnNode variable
                    && variable.getOpcode() == ASTORE && variable.var == 8) {
                variable.var = 6;
            }
        }
        assertThrows(IllegalStateException.class, () -> AlienEvoSlimeDecalMesh1201.apply(node));
    }

    @Test
    void failsClosedWhenTheInspectedBuilderEndCallIsMissing() {
        ClassNode node = renderer();
        for (AbstractInsnNode instruction : node.methods.get(0).instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.name.equals("end")) {
                node.methods.get(0).instructions.remove(call);
            }
        }
        assertThrows(IllegalStateException.class, () -> AlienEvoSlimeDecalMesh1201.apply(node));
    }

    @Test
    void leavesOtherClassesUntouched() {
        ClassNode node = renderer();
        node.name = "example/OtherRenderer";
        assertEquals(0, AlienEvoSlimeDecalMesh1201.apply(node));
    }

    private static AbstractInsnNode nextOpcode(AbstractInsnNode instruction) {
        if (instruction == null) return null;
        for (AbstractInsnNode next = instruction.getNext(); next != null; next = next.getNext()) {
            if (next.getOpcode() >= 0) return next;
        }
        return null;
    }
}
