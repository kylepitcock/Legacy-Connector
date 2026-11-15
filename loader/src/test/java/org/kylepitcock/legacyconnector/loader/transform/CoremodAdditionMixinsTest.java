package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class CoremodAdditionMixinsTest {
    private static final String CHUNK = "net/minecraft/world/chunk/Chunk";

    private static ClassNode chunk() {
        ClassNode c = new ClassNode();
        c.name = CHUNK;
        c.methods.add(new MethodNode(Opcodes.ACC_PUBLIC, "func_76588_a", "()V", null, null));
        return c;
    }

    private static ClassNode extended() {
        ClassNode after = chunk();
        after.interfaces.add("mod/IExtraChunkData");
        after.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "extra", "[Ljava/lang/Object;", null, null));
        MethodNode get = new MethodNode(Opcodes.ACC_PUBLIC, "getExtra", "()[Ljava/lang/Object;", null, null);
        get.visitVarInsn(Opcodes.ALOAD, 0);
        get.visitFieldInsn(Opcodes.GETFIELD, CHUNK, "extra", "[Ljava/lang/Object;");
        get.visitInsn(Opcodes.ARETURN);
        get.visitMaxs(1, 1);
        after.methods.add(get);
        MethodNode set = new MethodNode(Opcodes.ACC_PUBLIC, "setExtra", "([Ljava/lang/Object;)V", null, null);
        set.visitVarInsn(Opcodes.ALOAD, 0);
        set.visitVarInsn(Opcodes.ALOAD, 1);
        set.visitFieldInsn(Opcodes.PUTFIELD, CHUNK, "extra", "[Ljava/lang/Object;");
        set.visitInsn(Opcodes.RETURN);
        set.visitMaxs(2, 2);
        after.methods.add(set);
        return after;
    }

    @Test
    void addedInterfaceAndAccessorsBecomeAMixinWithStateCalls() {
        ClassNode mixin = CoremodAdditionMixins.mixin(chunk(), extended(), "mcc_coremod/j1/", Set.of("mod/IExtraChunkData"), new HashSet<>());
        assertNotNull(mixin);
        assertEquals("mcc_coremod/j1/ChunkAdditions", mixin.name);
        assertEquals(List.of("mod/IExtraChunkData"), mixin.interfaces);
        assertEquals("Lorg/spongepowered/asm/mixin/Mixin;", mixin.invisibleAnnotations.get(0).desc);
        assertEquals(2, mixin.methods.size());
        assertTrue(mixin.fields.isEmpty());
        int calls = 0;
        for (MethodNode m : mixin.methods) {
            for (AbstractInsnNode insn : m.instructions) {
                assertTrue(!(insn instanceof FieldInsnNode), "field access left in " + m.name);
                if (insn instanceof MethodInsnNode call && call.owner.equals(CoremodAdditionMixins.STATE)) calls++;
            }
        }
        assertEquals(2, calls);
    }

    @Test
    void interfaceOfNoJarClassIsLeftAlone() {
        assertNull(CoremodAdditionMixins.mixin(chunk(), extended(), "mcc_coremod/j1/", Set.of(), new HashSet<>()));
    }

    /** A coremod that adds a plain method (no interface) to a class: applied only for the coremod's own jar, and not where the shim has the method. */
    private static ClassNode withHelper() {
        ClassNode after = chunk();
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, "cofh_helper", "()Z", null, null);
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CHUNK, "func_76588_a", "()V", false);
        m.visitInsn(Opcodes.ICONST_1);
        m.visitInsn(Opcodes.IRETURN);
        m.visitMaxs(1, 1);
        after.methods.add(m);
        return after;
    }

    @Test
    void methodWithoutInterfaceIsAppliedForTheTransformersOwnJar() {
        ClassNode mixin = CoremodAdditionMixins.mixin(chunk(), withHelper(), "mcc_coremod/j1/", Set.of(), new HashSet<>(), true, true, n -> false);
        assertNotNull(mixin);
        assertTrue(mixin.interfaces.isEmpty());
        assertEquals(1, mixin.methods.size());
        assertEquals("cofh_helper", mixin.methods.get(0).name);
        assertNull(CoremodAdditionMixins.mixin(chunk(), withHelper(), "mcc_coremod/j1/", Set.of(), new HashSet<>(), false, true, n -> false));
        assertNull(CoremodAdditionMixins.mixin(chunk(), withHelper(), "mcc_coremod/j1/", Set.of(), new HashSet<>(), true, false, n -> false));
        assertNull(CoremodAdditionMixins.mixin(chunk(), withHelper(), "mcc_coremod/j1/", Set.of(), new HashSet<>(), true, true, n -> n.equals("cofh_helper")));
    }

    @Test
    void secondPassOverTheSameClassAddsNothing() {
        Set<String> done = new HashSet<>();
        assertNotNull(CoremodAdditionMixins.mixin(chunk(), extended(), "mcc_coremod/j1/", Set.of("mod/IExtraChunkData"), done));
        assertNull(CoremodAdditionMixins.mixin(chunk(), extended(), "mcc_coremod/j1/", Set.of("mod/IExtraChunkData"), done));
    }
}
