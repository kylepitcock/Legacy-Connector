package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/** The soft link on synthetic classes against a tiny made-up 1.21.1 class tree. */
class SoftLink1201Test implements Opcodes {
    private static final String BASE = "net/minecraft/world/Base";
    private static final String SUB = "net/minecraft/world/Sub";
    private static final String GONE = "net/minecraft/world/Removed";
    private static final String OTHER = "othermod/api/Thing";

    @BeforeAll
    static void index() {
        Mc1201Hierarchy.setMinecraftForTests(Map.of(
                BASE, new String[] {"java/lang/Object"},
                SUB, new String[] {BASE}));
        McMembers1201.setForTests(Map.of(
                BASE, new McMembers1201.Members(Set.of("<init>()V", "present()I", "tick()V"), Set.of("count:I")),
                SUB, new McMembers1201.Members(Set.of("<init>()V"), Set.of())));
    }

    @AfterAll
    static void restore() {
        McMembers1201.setForTests(null);
        Mc1201Hierarchy.setMinecraftForTests(Map.of());
    }

    private static ClassNode cls(String name, String sup) {
        ClassNode n = new ClassNode();
        n.version = V17;
        n.access = ACC_PUBLIC;
        n.name = name;
        n.superName = sup;
        return n;
    }

    private static MethodNode method(ClassNode n) {
        MethodNode m = new MethodNode(ACC_PUBLIC | ACC_STATIC, "run", "(L" + SUB + ";)V", null, null);
        n.methods.add(m);
        return m;
    }

    private static SoftLink1201 link(ClassNode... nodes) {
        Mc1201Hierarchy h = new Mc1201Hierarchy();
        for (ClassNode n : nodes) h.add(n);
        return new SoftLink1201(List.of(nodes), h);
    }

    private static boolean has(MethodNode m, int opcode, String name) {
        for (AbstractInsnNode in : m.instructions) {
            if (in.getOpcode() == opcode && (name == null || (in instanceof MethodInsnNode mi && mi.name.equals(name)))) return true;
        }
        return false;
    }

    @Test
    void resolvableSymbolsAreNotTouched() {
        ClassNode c = cls("mod/A", "java/lang/Object");
        MethodNode m = method(c);
        m.visitVarInsn(ALOAD, 0);
        m.visitMethodInsn(INVOKEVIRTUAL, SUB, "present", "()I", false); // inherited from Base
        m.visitInsn(POP);
        m.visitVarInsn(ALOAD, 0);
        m.visitFieldInsn(GETFIELD, SUB, "count", "I"); // inherited field
        m.visitInsn(POP);
        m.visitVarInsn(ALOAD, 0);
        m.visitMethodInsn(INVOKEVIRTUAL, SUB, "hashCode", "()I", false); // Object's
        m.visitInsn(POP);
        m.visitInsn(RETURN);
        SoftLink1201 s = link(c);
        assertEquals(0, s.apply(c));
        assertEquals(0, s.stats.symbols());
        assertFalse(has(m, ATHROW, null));
    }

    @Test
    void missingMethodThrowsOnlyWhenExecuted() {
        ClassNode c = cls("mod/B", "java/lang/Object");
        MethodNode m = method(c);
        m.visitVarInsn(ALOAD, 0);
        m.visitMethodInsn(INVOKEVIRTUAL, SUB, "removed", "(I)V", false);
        m.visitInsn(RETURN);
        SoftLink1201 s = link(c);
        assertEquals(1, s.apply(c));
        assertEquals(Set.of(SUB + ".removed(I)V"), s.stats.missingMethods);
        assertFalse(has(m, INVOKEVIRTUAL, "removed"));
        assertTrue(has(m, INVOKESTATIC, "unresolved"));
        assertTrue(has(m, ATHROW, null));
        assertTrue(s.stats.lines().get(0).contains("Soft link: method"));
    }

    @Test
    void missingFieldReadsTheDefault() {
        ClassNode c = cls("mod/C", "java/lang/Object");
        MethodNode m = method(c);
        m.visitVarInsn(ALOAD, 0);
        m.visitFieldInsn(GETFIELD, SUB, "gone", "J");
        m.visitInsn(POP2);
        m.visitVarInsn(ALOAD, 0);
        m.visitInsn(LCONST_1);
        m.visitFieldInsn(PUTFIELD, SUB, "gone", "J");
        m.visitInsn(RETURN);
        SoftLink1201 s = link(c);
        assertEquals(2, s.apply(c));
        assertEquals(Set.of(SUB + ".gone:J"), s.stats.missingFields);
        boolean lconst = false;
        for (AbstractInsnNode in : m.instructions) {
            assertFalse(in instanceof FieldInsnNode);
            if (in.getOpcode() == LCONST_0) lconst = true;
        }
        assertTrue(lconst);
        assertFalse(has(m, ATHROW, null));
    }

    @Test
    void missingClassIsStubbed() {
        ClassNode c = cls("mod/D", GONE);
        c.interfaces.add("net/minecraft/world/RemovedIface");
        MethodNode m = method(c);
        m.visitTypeInsn(NEW, GONE);
        m.visitInsn(DUP);
        m.visitMethodInsn(INVOKESPECIAL, GONE, "<init>", "()V", false);
        m.visitInsn(POP);
        m.visitVarInsn(ALOAD, 0);
        m.visitTypeInsn(INSTANCEOF, GONE);
        m.visitInsn(POP);
        m.visitInsn(RETURN);
        SoftLink1201 s = link(c);
        assertTrue(s.apply(c) > 0);
        assertEquals("java/lang/Object", c.superName);
        assertTrue(c.interfaces.isEmpty());
        assertTrue(s.stats.missingClasses.containsAll(Set.of(GONE, "net/minecraft/world/RemovedIface")));
        for (AbstractInsnNode in : m.instructions) assertFalse(in instanceof TypeInsnNode t && t.getOpcode() == INSTANCEOF);
        assertTrue(has(m, ATHROW, null));
    }

    @Test
    void referencesOutsideTheHostAreLeftAlone() {
        ClassNode c = cls("mod/E", "java/lang/Object");
        MethodNode m = method(c);
        m.visitTypeInsn(NEW, OTHER); // another mod: maybe present, never ours to judge
        m.visitInsn(DUP);
        m.visitMethodInsn(INVOKESPECIAL, OTHER, "<init>", "()V", false);
        m.visitInsn(POP);
        m.visitVarInsn(ALOAD, 0);
        m.visitMethodInsn(INVOKEVIRTUAL, "java/util/List", "size", "()I", false);
        m.visitInsn(POP);
        m.visitInsn(RETURN);
        SoftLink1201 s = link(c);
        assertEquals(0, s.apply(c));
    }

    @Test
    void modClassWithForeignSuperIsNotJudged() {
        ClassNode c = cls("mod/F", OTHER);
        MethodNode m = method(c);
        m.access = ACC_PUBLIC;
        m.visitVarInsn(ALOAD, 0);
        m.visitMethodInsn(INVOKEVIRTUAL, "mod/F", "fromOtherMod", "()V", false);
        m.visitInsn(RETURN);
        SoftLink1201 s = link(c);
        assertEquals(0, s.apply(c));
    }

    @Test
    void catchOfMissingClassIsDropped() {
        ClassNode c = cls("mod/G", "java/lang/Object");
        MethodNode m = method(c);
        var start = new org.objectweb.asm.Label();
        var end = new org.objectweb.asm.Label();
        var handler = new org.objectweb.asm.Label();
        m.visitTryCatchBlock(start, end, handler, GONE);
        m.visitLabel(start);
        m.visitInsn(NOP);
        m.visitLabel(end);
        m.visitInsn(RETURN);
        m.visitLabel(handler);
        m.visitInsn(POP);
        m.visitInsn(RETURN);
        SoftLink1201 s = link(c);
        assertEquals(1, s.apply(c));
        assertTrue(m.tryCatchBlocks.isEmpty());
    }

    @Test
    void ldcOfMissingClassThrows() {
        ClassNode c = cls("mod/H", "java/lang/Object");
        MethodNode m = method(c);
        m.visitLdcInsn(Type.getObjectType(GONE));
        m.visitInsn(POP);
        m.visitInsn(RETURN);
        SoftLink1201 s = link(c);
        assertEquals(1, s.apply(c));
        for (AbstractInsnNode in : m.instructions) assertFalse(in instanceof LdcInsnNode l && l.cst instanceof Type);
    }
}
