package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** The view the coremod class loader gives untranslated legacy mod code: ASM node subclasses and the forward layer's class renames. */
class CoremodLoaderViewTest {
    private static byte[] write(ClassNode node) {
        ClassWriter w = new ClassWriter(0);
        node.accept(w);
        return w.toByteArray();
    }

    private static ClassNode read(byte[] bytes) {
        ClassNode n = new ClassNode();
        new ClassReader(bytes).accept(n, 0);
        return n;
    }

    /** {@code new MethodNode(access, name, desc, sig, ex) { }} as an ASM 4 era coremod compiled it. */
    private static byte[] anonymousMethodNode() {
        ClassNode c = new ClassNode();
        c.version = Opcodes.V1_6;
        c.name = "mod/Transformer$3";
        c.superName = "org/objectweb/asm/tree/MethodNode";
        c.access = Opcodes.ACC_SUPER;
        MethodNode init = new MethodNode(0, "<init>", "()V", null, null);
        init.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        init.instructions.add(new org.objectweb.asm.tree.IntInsnNode(Opcodes.SIPUSH, 1));
        init.instructions.add(new org.objectweb.asm.tree.LdcInsnNode("n"));
        init.instructions.add(new org.objectweb.asm.tree.LdcInsnNode("()V"));
        init.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        init.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        init.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "org/objectweb/asm/tree/MethodNode", "<init>", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)V", false));
        init.instructions.add(new InsnNode(Opcodes.RETURN));
        c.methods.add(init);
        return write(c);
    }

    @Test
    void asmNodeSubclassGetsTheApiLevelSuperclass() {
        ClassNode out = read(AsmNodeCompat.apply(anonymousMethodNode()));
        assertEquals("org/kylepitcock/legacyconnector/loader/transform/CompatMethodNode", out.superName);
        MethodInsnNode call = null;
        for (var insn : out.methods.get(0).instructions) if (insn instanceof MethodInsnNode m) call = m;
        assertEquals("org/kylepitcock/legacyconnector/loader/transform/CompatMethodNode", call.owner);
    }

    @Test
    void compatNodesAcceptTheOldConstructors() {
        // ASM 9 throws IllegalStateException for a plain MethodNode subclass without an API level; the stand-ins must not
        new CompatMethodNode(1, "n", "()V", null, null) {};
        new CompatMethodNode() {};
        new CompatClassNode() {};
    }

    @Test
    void classesWithoutNodeSuperclassAreUntouched() {
        ClassNode c = new ClassNode();
        c.version = Opcodes.V1_6;
        c.name = "mod/Plain";
        c.superName = "java/lang/Object";
        c.fields.add(new FieldNode(0, "f", "I", null, null));
        byte[] bytes = write(c);
        assertSame(bytes, AsmNodeCompat.apply(bytes));
    }

    @Test
    void forwardRenamesMapTheOldClassNames() {
        ClassNode c = new ClassNode();
        c.version = Opcodes.V1_6;
        c.name = "mod/UsesConfig";
        c.superName = "java/lang/Object";
        c.fields.add(new FieldNode(Opcodes.ACC_STATIC, "cfg", "Lnet/minecraftforge/common/Configuration;", null, null));
        ClassNode out = read(CoremodLoaderViewTestAccess.forwardRenames(write(c), LegacyVersion.V1_6_4));
        assertEquals("Lnet/minecraftforge/common/config/Configuration;", out.fields.get(0).desc);
    }

    @Test
    void noForwardVersionLeavesTheClass() {
        ClassNode c = new ClassNode();
        c.version = Opcodes.V1_6;
        c.name = "mod/X";
        c.superName = "java/lang/Object";
        byte[] bytes = write(c);
        assertSame(bytes, LegacyCoreModTransformers.forwardRenames(bytes, null));
    }

    /** forwardRenames is package-private in the same package: this indirection only documents the access. */
    private static final class CoremodLoaderViewTestAccess {
        static byte[] forwardRenames(byte[] b, LegacyVersion v) {
            return LegacyCoreModTransformers.forwardRenames(b, v);
        }
    }
}
