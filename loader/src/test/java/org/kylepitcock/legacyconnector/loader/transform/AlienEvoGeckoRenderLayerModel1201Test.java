package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

class AlienEvoGeckoRenderLayerModel1201Test implements Opcodes {
    private static final String BGC = AlienEvoGeckoRenderLayerModel1201.BUFFERING_GLOW_CONSUMER;
    private static final String RENDER_TYPE = AlienEvoGeckoRenderLayerModel1201.RENDER_TYPE;
    private static final String MBS = AlienEvoGeckoRenderLayerModel1201.MULTI_BUFFER_SOURCE;
    private static final String VERTEX = AlienEvoGeckoRenderLayerModel1201.VERTEX_CONSUMER;
    private static final String TEXTURE = AlienEvoGeckoRenderLayerModel1201.RESOURCE_LOCATION;
    private static final String RECURSIVE_DESC = AlienEvoGeckoRenderLayerModel1201.RECURSIVE_DESC;
    private static final String MODEL_RENDER_DESC = AlienEvoGeckoRenderLayerModel1201.MODEL_BUFFER_RENDER_DESC;

    private static ClassNode node(String name, MethodNode animation) {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.name = name;
        node.superName = "java/lang/Object";
        node.methods.add(animation);
        if (AlienEvoGeckoRenderLayerModel1201.TARGET_CLASS.equals(name)) {
            node.methods.add(recursiveMethod(name));
            node.methods.add(modelRenderMethod());
        }
        return node;
    }

    private static MethodNode recursiveMethod(String owner) {
        MethodNode method = new MethodNode(ACC_PUBLIC, "renderRecursively", RECURSIVE_DESC, null, null);
        method.instructions.add(new VarInsnNode(ALOAD, 1));
        method.instructions.add(new VarInsnNode(ALOAD, 3));
        method.instructions.add(new MethodInsnNode(INVOKESTATIC,
                AlienEvoGeckoRenderLayerModel1201.RENDER_UTIL, "prepMatrixForBone",
                AlienEvoGeckoRenderLayerModel1201.PREP_MATRIX_FOR_BONE_DESC, false));
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new VarInsnNode(ALOAD, 1));
        method.instructions.add(new VarInsnNode(ALOAD, 2));
        method.instructions.add(new VarInsnNode(ALOAD, 3));
        method.instructions.add(new VarInsnNode(ALOAD, 4));
        method.instructions.add(new VarInsnNode(ALOAD, 5));
        method.instructions.add(new VarInsnNode(ALOAD, 6));
        method.instructions.add(new VarInsnNode(ILOAD, 7));
        method.instructions.add(new VarInsnNode(FLOAD, 8));
        method.instructions.add(new VarInsnNode(ILOAD, 9));
        method.instructions.add(new VarInsnNode(ILOAD, 10));
        method.instructions.add(new VarInsnNode(FLOAD, 11));
        method.instructions.add(new VarInsnNode(FLOAD, 12));
        method.instructions.add(new VarInsnNode(FLOAD, 13));
        method.instructions.add(new VarInsnNode(FLOAD, 14));
        method.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, owner, "renderRecursively", RECURSIVE_DESC, false));
        method.instructions.add(new InsnNode(RETURN));
        method.maxLocals = 15;
        method.maxStack = 15;
        return method;
    }

    private static MethodNode modelRenderMethod() {
        MethodNode method = new MethodNode(ACC_PUBLIC, "renderToBuffer", MODEL_RENDER_DESC, null, null);
        method.instructions.add(new InsnNode(ACONST_NULL));
        method.instructions.add(new VarInsnNode(ASTORE, 10));
        method.instructions.add(new InsnNode(ACONST_NULL));
        method.instructions.add(new VarInsnNode(ASTORE, 18));
        method.instructions.add(new InsnNode(ACONST_NULL));
        method.instructions.add(new VarInsnNode(ASTORE, 19));
        method.instructions.add(new TypeInsnNode(NEW, BGC));
        method.instructions.add(new InsnNode(DUP));
        method.instructions.add(new VarInsnNode(ALOAD, 10));
        method.instructions.add(new VarInsnNode(ALOAD, 18));
        method.instructions.add(new MethodInsnNode(INVOKESTATIC, RENDER_TYPE, "entityTranslucent",
                "(L" + TEXTURE + ";)L" + RENDER_TYPE + ";", false));
        method.instructions.add(new MethodInsnNode(INVOKEINTERFACE, MBS, "getBuffer",
                "(L" + RENDER_TYPE + ";)L" + VERTEX + ";", true));
        method.instructions.add(new VarInsnNode(ALOAD, 19));
        method.instructions.add(new MethodInsnNode(INVOKESPECIAL, BGC, "<init>",
                "(L" + VERTEX + ";L" + RENDER_TYPE + ";)V", false));
        method.instructions.add(new VarInsnNode(ASTORE, 2));
        method.instructions.add(new InsnNode(RETURN));
        method.maxLocals = 20;
        method.maxStack = 6;
        return method;
    }

    private static MethodNode animationMethod(String descriptor) {
        MethodNode method = new MethodNode(ACC_PUBLIC, "renderToBuffer", "()V", null, null);
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new VarInsnNode(ALOAD, 1));
        method.instructions.add(new VarInsnNode(LLOAD, 2));
        method.instructions.add(new VarInsnNode(ALOAD, 4));
        method.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                PalladiumGeckoRenderLayer1201.GEO_MODEL, "handleAnimations", descriptor, false));
        method.instructions.add(new InsnNode(RETURN));
        method.maxStack = 5;
        method.maxLocals = 5;
        return method;
    }

    @Test
    void adaptsAnimationAndBothBufferRefreshPaths() {
        ClassNode node = node(AlienEvoGeckoRenderLayerModel1201.TARGET_CLASS,
                animationMethod(PalladiumGeckoRenderLayer1201.OLD_HANDLE_ANIMATIONS_DESC));
        MethodNode animation = node.methods.get(0);
        assertEquals(3, AlienEvoGeckoRenderLayerModel1201.apply(node));
        AbstractInsnNode[] instructions = animation.instructions.toArray();
        assertEquals(DUP, instructions[4].getOpcode());
        assertTrue(instructions[5] instanceof MethodInsnNode call
                && "getPartialTick".equals(call.name) && "()F".equals(call.desc));
        assertEquals(6, animation.maxStack);

        MethodNode recursive = find(node, "renderRecursively", RECURSIVE_DESC);
        assertNotNull(recursive);
        MethodInsnNode prep = findCall(recursive,
                AlienEvoGeckoRenderLayerModel1201.RENDER_UTIL, "prepMatrixForBone");
        assertNotNull(prep);
        assertEquals(INVOKESTATIC, prep.getOpcode());
        assertEquals(AlienEvoGeckoRenderLayerModel1201.PREP_MATRIX_FOR_BONE_DESC, prep.desc);
        AbstractInsnNode guard = nextMeaningful(prep);
        assertTrue(guard instanceof VarInsnNode load && load.getOpcode() == ILOAD && load.var == 7);
        assertTrue(nextMeaningful(guard) instanceof JumpInsnNode jump && jump.getOpcode() == IFNE);
        assertNotNull(findCall(recursive, BGC, "legacyRefreshSolid"));
        assertNotNull(findCall(recursive, AlienEvoGeckoRenderLayerModel1201.BUFFER_REFRESH, "needsRefresh"));
        MethodInsnNode factory = findCall(recursive,
                "net/threetag/palladium/client/renderer/renderlayer/RenderTypeFunction", "createVertexConsumer");
        assertNotNull(factory);
        assertEquals(INVOKEINTERFACE, factory.getOpcode());
        assertEquals(1, countVar(recursive, ASTORE, 6));
        assertTrue(countVar(recursive, ALOAD, 6) >= 2);
        assertEquals(0, countCalls(recursive, "software/bernie/geckolib/renderer/GeoRenderer",
                "checkAndRefreshBuffer"));

        MethodNode modelRender = find(node, "renderToBuffer", MODEL_RENDER_DESC);
        assertNotNull(modelRender);
        assertNotNull(findCall(modelRender, BGC, "legacySetSolidType"));
        assertEquals(1, countCalls(modelRender, BGC, "<init>"));
        assertEquals(0, AlienEvoGeckoRenderLayerModel1201.apply(node));
    }

    @Test
    void leavesUnrelatedClassesUntouched() {
        MethodNode method = new MethodNode(ACC_PUBLIC, "renderToBuffer", "()V", null, null);
        method.instructions.add(new InsnNode(RETURN));
        ClassNode node = node("example/UnrelatedAlienRenderLayerModel", method);
        assertEquals(0, AlienEvoGeckoRenderLayerModel1201.apply(node));
    }

    @Test
    void failsClosedWhenTheAnimationCallDescriptorChanges() {
        ClassNode node = node(AlienEvoGeckoRenderLayerModel1201.TARGET_CLASS,
                animationMethod("(Lsoftware/bernie/geckolib/core/animatable/GeoAnimatable;J"
                        + "Lsoftware/bernie/geckolib/core/animation/AnimationState;I)V"));
        assertThrows(IllegalStateException.class, () -> AlienEvoGeckoRenderLayerModel1201.apply(node));
    }

    @Test
    void failsClosedWhenThePreRemapGeckoLib48AnchorReachesThePostRemapAdapter() {
        ClassNode node = node(AlienEvoGeckoRenderLayerModel1201.TARGET_CLASS,
                animationMethod(PalladiumGeckoRenderLayer1201.OLD_HANDLE_ANIMATIONS_DESC));
        MethodNode recursive = find(node, "renderRecursively", RECURSIVE_DESC);
        MethodInsnNode prep = findCall(recursive,
                AlienEvoGeckoRenderLayerModel1201.RENDER_UTIL, "prepMatrixForBone");
        assertNotNull(prep);
        prep.owner = "software/bernie/geckolib/util/RenderUtils";
        prep.desc = "(Lcom/mojang/blaze3d/vertex/PoseStack;"
                + "Lsoftware/bernie/geckolib/core/animatable/model/CoreGeoBone;)V";
        assertThrows(IllegalStateException.class, () -> AlienEvoGeckoRenderLayerModel1201.apply(node));
    }

    @Test
    void failsClosedWhenTheRecursiveMatrixAnchorChanges() {
        ClassNode node = node(AlienEvoGeckoRenderLayerModel1201.TARGET_CLASS,
                animationMethod(PalladiumGeckoRenderLayer1201.OLD_HANDLE_ANIMATIONS_DESC));
        MethodNode recursive = find(node, "renderRecursively", RECURSIVE_DESC);
        recursive.instructions.remove(findCall(recursive,
                AlienEvoGeckoRenderLayerModel1201.RENDER_UTIL, "prepMatrixForBone"));
        assertThrows(IllegalStateException.class, () -> AlienEvoGeckoRenderLayerModel1201.apply(node));
    }

    private static MethodNode find(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) if (method.name.equals(name) && method.desc.equals(desc)) return method;
        return null;
    }

    private static MethodInsnNode findCall(MethodNode method, String owner, String name) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(name)) return call;
        }
        return null;
    }

    private static int countCalls(MethodNode method, String owner, String name) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(name)) count++;
        }
        return count;
    }

    private static int countVar(MethodNode method, int opcode, int slot) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof VarInsnNode variable && variable.getOpcode() == opcode && variable.var == slot) count++;
        }
        return count;
    }

    private static AbstractInsnNode nextMeaningful(AbstractInsnNode instruction) {
        AbstractInsnNode value = instruction == null ? null : instruction.getNext();
        while (value != null && value.getOpcode() < 0) value = value.getNext();
        return value;
    }
}
