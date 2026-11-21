package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

/** Verifies the exact catch-table and verifier-metadata migration in AlienEvo's layer renderer. */
class AlienEvoGeckoException1201Test implements Opcodes {
    private static final String TARGET = AlienEvoGeckoException1201.TARGET;
    private static final String METHOD_DESC =
            "(Lnet/habb/alienevo/compat/geckolib/renderlayer/AlienLayerState;)"
                    + "Lsoftware/bernie/geckolib/cache/object/BakedGeoModel;";
    private static final String OLD_INTERNAL = "software/bernie/geckolib/GeckoLibException";
    private static final String NEW_INTERNAL = "java/lang/RuntimeException";
    private static final String OLD_DESC = "L" + OLD_INTERNAL + ";";
    private static final String NEW_DESC = "L" + NEW_INTERNAL + ";";

    private static ClassNode fixture() {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.access = ACC_PUBLIC;
        node.name = TARGET;
        node.superName = "java/lang/Object";

        MethodNode method = new MethodNode(ACC_PRIVATE, "safelyGetBakedModel", METHOD_DESC, null, null);
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        LabelNode handler = new LabelNode();
        LabelNode localEnd = new LabelNode();
        method.instructions.add(start);
        method.instructions.add(new InsnNode(NOP));
        method.instructions.add(end);
        method.instructions.add(handler);
        FrameNode appendedLocal = new FrameNode(F_APPEND, 1, new Object[] {OLD_INTERNAL}, 0, null);
        FrameNode caughtException = new FrameNode(F_SAME1, 0, null, 1, new Object[] {OLD_INTERNAL});
        method.instructions.add(appendedLocal);
        method.instructions.add(caughtException);
        method.instructions.add(new InsnNode(POP));
        method.instructions.add(new InsnNode(ACONST_NULL));
        method.instructions.add(new InsnNode(ARETURN));
        method.instructions.add(localEnd);
        method.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, OLD_INTERNAL));
        method.localVariables = new ArrayList<>();
        method.localVariables.add(new LocalVariableNode("e", OLD_DESC, null, handler, localEnd, 4));
        method.maxLocals = 5;
        method.maxStack = 1;
        node.methods.add(method);
        return node;
    }

    @Test
    void migratesCatchTableStackMapFramesAndLocalVariableTogether() {
        ClassNode node = fixture();
        MethodNode method = node.methods.get(0);

        assertEquals(1, AlienEvoGeckoException1201.apply(node));
        assertEquals(NEW_INTERNAL, method.tryCatchBlocks.get(0).type);

        FrameNode appendedLocal = (FrameNode) method.instructions.get(4);
        FrameNode caughtException = (FrameNode) method.instructions.get(5);
        assertEquals(NEW_INTERNAL, appendedLocal.local.get(0));
        assertEquals(NEW_INTERNAL, caughtException.stack.get(0));
        assertEquals(NEW_DESC, method.localVariables.get(0).desc);
        assertEquals(0, AlienEvoGeckoException1201.apply(node), "migration must be idempotent");
    }

    @Test
    void failsClosedWhenTheInspectedCatchShapeIsMissing() {
        ClassNode node = fixture();
        node.methods.get(0).tryCatchBlocks.clear();

        assertThrows(IllegalStateException.class, () -> AlienEvoGeckoException1201.apply(node));
    }

    @Test
    void ignoresUnrelatedClasses() {
        ClassNode node = fixture();
        node.name = "net/habb/alienevo/compat/geckolib/renderlayer/UnrelatedLayer";

        assertEquals(0, AlienEvoGeckoException1201.apply(node));
    }
}
