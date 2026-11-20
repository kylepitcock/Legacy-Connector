package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Verifies AlienEvo's old Molang pre-registration loop uses GeckoLib 4.9's registry API. */
class AlienEvoMolang1201Test implements Opcodes {
    private static ClassNode bootstrap() {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.access = ACC_PUBLIC | ACC_FINAL;
        node.name = AlienEvoMolang1201.BOOTSTRAP;
        node.superName = "java/lang/Object";
        node.fields.add(new FieldNode(ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
                "QUERIES", "[Ljava/lang/String;", null, null));
        MethodNode init = new MethodNode(ACC_PUBLIC | ACC_STATIC, "init", "()V", null, null);
        // Old 1.20.1 implementation: MolangParser.INSTANCE.getVariable(query).
        init.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(GETSTATIC,
                AlienEvoMolang1201.OLD_PARSER, "INSTANCE",
                "L" + AlienEvoMolang1201.OLD_PARSER + ";"));
        init.instructions.add(new org.objectweb.asm.tree.InsnNode(POP));
        init.instructions.add(new org.objectweb.asm.tree.InsnNode(RETURN));
        init.maxLocals = 0;
        init.maxStack = 1;
        node.methods.add(init);
        return node;
    }

    @Test
    void preRegistrationLoopUsesGeckoLib49MathParser() {
        ClassNode node = bootstrap();

        assertEquals(1, AlienEvoMolang1201.apply(node));
        MethodNode init = find(node, "init", "()V");
        assertNotNull(init);
        assertTrue(hasCall(init, INVOKESTATIC, AlienEvoMolang1201.MATH_PARSER, "getVariableFor",
                "(Ljava/lang/String;)L" + AlienEvoMolang1201.VARIABLE + ";"));
        assertFalse(hasOwner(init, AlienEvoMolang1201.OLD_PARSER),
                "no constant-pool reference is emitted from the rewritten initializer");
        assertTrue(hasOpcode(init, ARRAYLENGTH));
        assertTrue(hasOpcode(init, AALOAD));
        assertTrue(hasOpcode(init, IF_ICMPGE));
        assertTrue(hasOpcode(init, IINC));
        assertEquals(3, init.maxLocals);
        assertEquals(2, init.maxStack);

        assertEquals(0, AlienEvoMolang1201.apply(node), "the adapter must be idempotent");
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        assertTrue(writer.toByteArray().length > 0);
    }

    @Test
    void unrelatedClassesAreNotModified() {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.name = "example/UnrelatedBootstrap";
        node.superName = "java/lang/Object";
        assertEquals(0, AlienEvoMolang1201.apply(node));
    }

    private static MethodNode find(ClassNode node, String name, String descriptor) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(descriptor)) return method;
        }
        return null;
    }

    private static boolean hasOwner(MethodNode method, String owner) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner)) return true;
            if (instruction instanceof org.objectweb.asm.tree.FieldInsnNode field && field.owner.equals(owner)) return true;
        }
        return false;
    }

    private static boolean hasCall(MethodNode method, int opcode, String owner, String name, String desc) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.getOpcode() == opcode
                    && call.owner.equals(owner) && call.name.equals(name) && call.desc.equals(desc)) return true;
        }
        return false;
    }

    private static boolean hasOpcode(MethodNode method, int opcode) {
        for (var instruction : method.instructions.toArray()) if (instruction.getOpcode() == opcode) return true;
        return false;
    }
}
