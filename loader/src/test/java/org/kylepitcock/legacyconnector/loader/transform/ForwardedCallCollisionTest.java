package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.mapping.McpMappings;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * A mod class that declares a method and calls a DIFFERENT inherited method of itself, where both end up with one name after the renames, would
 * call itself (The Lion King's quiver inventory: StackOverflowError). The declaration is renamed locally, the inherited call stays.
 */
class ForwardedCallCollisionTest {
    private static ClassNode quiver(String declared, String called) {
        ClassNode cn = new ClassNode();
        cn.version = Opcodes.V1_6;
        cn.name = "mod/Quiver";
        cn.superName = "java/lang/Object";
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, declared, "()V", null, null);
        InsnList il = m.instructions;
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "mod/Quiver", called, "()V", false));
        il.add(new InsnNode(Opcodes.RETURN));
        cn.methods.add(m);
        return cn;
    }

    private static void run(LegacyRemapper r, ClassNode cn) throws Exception {
        Method m = LegacyRemapper.class.getDeclaredMethod("preventForwardedCallCollisions", ClassNode.class);
        m.setAccessible(true);
        m.invoke(r, cn);
    }

    /** Two raw names that map to the same readable name (declaration + call on this): the declaration is renamed locally. */
    @Test
    void declarationWithTheSameFinalNameAsTheInheritedCallIsRenamed() throws Exception {
        LegacyRemapper r = new LegacyRemapper(LegacyVersion.V1_7_10, McpMappings.forVersion(LegacyVersion.V1_7_10), null, null);
        // SRG func_70296_d and the readable markDirty both end up as markDirty
        ClassNode cn = quiver("func_70296_d", "markDirty");
        r.localMethods(Map.of("mod/Quiver", Set.of("func_70296_d()V")));
        run(r, cn);
        assertEquals("markDirty$mod", r.mapMethodName("mod/Quiver", "func_70296_d", "()V"));
    }

    @Test
    void differentFinalNamesAreLeftAlone() throws Exception {
        LegacyRemapper r = new LegacyRemapper(LegacyVersion.V1_7_10, McpMappings.forVersion(LegacyVersion.V1_7_10), null, null);
        ClassNode cn = quiver("zzOwn", "markDirty");
        r.localMethods(Map.of("mod/Quiver", Set.of("zzOwn()V")));
        run(r, cn);
        assertNotEquals("zzOwn$mod", r.mapMethodName("mod/Quiver", "zzOwn", "()V"));
    }

    @Test
    void callToTheMethodTheClassDeclaresItselfIsLeftAlone() throws Exception {
        LegacyRemapper r = new LegacyRemapper(LegacyVersion.V1_7_10, McpMappings.forVersion(LegacyVersion.V1_7_10), null, null);
        ClassNode cn = quiver("markDirty", "markDirty");
        r.localMethods(Map.of("mod/Quiver", Set.of("markDirty()V")));
        run(r, cn);
        assertEquals("markDirty", r.mapMethodName("mod/Quiver", "markDirty", "()V"));
    }
}
