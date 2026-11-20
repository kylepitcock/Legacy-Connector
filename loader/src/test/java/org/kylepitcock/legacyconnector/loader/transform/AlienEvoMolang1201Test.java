package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Verifies the GeckoLib 4.8 MolangParser to GeckoLib 4.9 MathParser migration. */
class AlienEvoMolang1201Test implements Opcodes {
    private static final String CALLBACK = "org/spongepowered/asm/mixin/injection/callback/CallbackInfo";

    private static ClassNode bootstrap() {
        ClassNode node = node(AlienEvoMolang1201.BOOTSTRAP);
        node.fields.add(new FieldNode(ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
                "QUERIES", "[Ljava/lang/String;", null, null));
        MethodNode init = method(node, ACC_PUBLIC | ACC_STATIC, "init", "()V");
        init.instructions.add(new FieldInsnNode(GETSTATIC, AlienEvoMolang1201.OLD_PARSER, "INSTANCE",
                "L" + AlienEvoMolang1201.OLD_PARSER + ";"));
        init.instructions.add(new InsnNode(POP));
        init.instructions.add(new InsnNode(RETURN));
        return node;
    }

    private static ClassNode node(String name) {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.access = ACC_PUBLIC;
        node.name = name;
        node.superName = "java/lang/Object";
        return node;
    }

    private static MethodNode method(ClassNode node, int access, String name, String desc) {
        MethodNode method = new MethodNode(access, name, desc, null, null);
        node.methods.add(method);
        return method;
    }

    @Test
    void queryBootstrapPreRegistersAllNamesThroughGeckoLib49() {
        ClassNode node = bootstrap();

        assertEquals(1, AlienEvoMolang1201.apply(node));
        MethodNode init = find(node, "init", "()V");
        assertNotNull(init);
        assertTrue(hasCall(init, INVOKESTATIC, AlienEvoMolang1201.MATH_PARSER, "getVariableFor",
                "(Ljava/lang/String;)L" + AlienEvoMolang1201.VARIABLE + ";"));
        assertFalse(hasOwner(init, AlienEvoMolang1201.OLD_PARSER));
        assertTrue(hasOpcode(init, ARRAYLENGTH));
        assertTrue(hasOpcode(init, AALOAD));
        assertTrue(hasOpcode(init, IF_ICMPGE));
        assertTrue(hasOpcode(init, IINC));
        assertEquals(3, init.maxLocals);
        assertEquals(2, init.maxStack);

        assertEquals(0, AlienEvoMolang1201.apply(node));
        assertClassCanBeSerialized(node);
    }

    @Test
    void oldParserVariableAndSetterCallsUseTheCompatibilityFacade() {
        ClassNode node = node("net/habb/alienevo/client/renderer/HologramRenderer");
        MethodNode render = method(node, ACC_PRIVATE, "readQueries", "()V");
        render.instructions.add(new FieldInsnNode(GETSTATIC, AlienEvoMolang1201.OLD_PARSER, "INSTANCE",
                "L" + AlienEvoMolang1201.OLD_PARSER + ";"));
        render.instructions.add(new LdcInsnNode("query.anim_time"));
        render.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, AlienEvoMolang1201.OLD_PARSER, "getVariable",
                "(Ljava/lang/String;)L" + AlienEvoMolang1201.OLD_LAZY_VARIABLE + ";", false));
        render.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, AlienEvoMolang1201.OLD_LAZY_VARIABLE,
                "get", "()D", false));
        render.instructions.add(new InsnNode(POP2));
        render.instructions.add(new FieldInsnNode(GETSTATIC, AlienEvoMolang1201.OLD_PARSER, "INSTANCE",
                "L" + AlienEvoMolang1201.OLD_PARSER + ";"));
        render.instructions.add(new LdcInsnNode("query.is_crouching"));
        render.instructions.add(new InsnNode(ACONST_NULL));
        render.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, AlienEvoMolang1201.OLD_PARSER, "setValue",
                "(Ljava/lang/String;Ljava/util/function/DoubleSupplier;)V", false));
        render.instructions.add(new FieldInsnNode(GETSTATIC, AlienEvoMolang1201.OLD_PARSER, "INSTANCE",
                "L" + AlienEvoMolang1201.OLD_PARSER + ";"));
        render.instructions.add(new LdcInsnNode("query.rotation"));
        render.instructions.add(new InsnNode(ACONST_NULL));
        render.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, AlienEvoMolang1201.OLD_PARSER, "setMemoizedValue",
                "(Ljava/lang/String;Ljava/util/function/DoubleSupplier;)V", false));
        render.instructions.add(new InsnNode(RETURN));

        assertTrue(AlienEvoMolang1201.apply(node) >= 7);
        assertTrue(hasField(render, GETSTATIC, AlienEvoMolang1201.PARSER_COMPAT, "INSTANCE",
                "L" + AlienEvoMolang1201.PARSER_COMPAT + ";"));
        assertTrue(hasCall(render, INVOKEVIRTUAL, AlienEvoMolang1201.PARSER_COMPAT, "getVariable",
                "(Ljava/lang/String;)L" + AlienEvoMolang1201.VARIABLE_COMPAT + ";"));
        assertTrue(hasCall(render, INVOKEVIRTUAL, AlienEvoMolang1201.VARIABLE_COMPAT, "get", "()D"));
        assertTrue(hasCall(render, INVOKEVIRTUAL, AlienEvoMolang1201.PARSER_COMPAT, "setValue",
                "(Ljava/lang/String;Ljava/util/function/DoubleSupplier;)V"));
        assertTrue(hasCall(render, INVOKEVIRTUAL, AlienEvoMolang1201.PARSER_COMPAT, "setMemoizedValue",
                "(Ljava/lang/String;Ljava/util/function/DoubleSupplier;)V"));
        assertFalse(hasOwner(render, AlienEvoMolang1201.OLD_PARSER));
        assertFalse(hasOwner(render, AlienEvoMolang1201.OLD_LAZY_VARIABLE));
        assertEquals(0, AlienEvoMolang1201.apply(node));
        assertClassCanBeSerialized(node);
    }

    @Test
    void clientRegistrationMovesTheCustomRollSinceFunctionToMathParserFactory() {
        ClassNode node = node(AlienEvoMolang1201.CLIENT);
        MethodNode init = method(node, ACC_PUBLIC | ACC_STATIC, "init", "()V");
        init.instructions.add(new FieldInsnNode(GETSTATIC, AlienEvoMolang1201.OLD_PARSER, "INSTANCE",
                "L" + AlienEvoMolang1201.OLD_PARSER + ";"));
        init.instructions.add(new FieldInsnNode(GETFIELD, AlienEvoMolang1201.OLD_PARSER, "functions", "Ljava/util/Map;"));
        init.instructions.add(new LdcInsnNode("alienevo.roll_since"));
        init.instructions.add(new LdcInsnNode(Type.getObjectType(AlienEvoMolang1201.ROLL_FUNCTION)));
        init.instructions.add(new MethodInsnNode(INVOKEINTERFACE, "java/util/Map", "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true));
        init.instructions.add(new InsnNode(POP));
        init.instructions.add(new InsnNode(RETURN));

        assertEquals(1, AlienEvoMolang1201.apply(node));
        assertTrue(hasCall(init, INVOKESTATIC, AlienEvoMolang1201.PARSER_COMPAT, "registerRollSince", "()V"));
        assertFalse(hasOwner(init, AlienEvoMolang1201.OLD_PARSER));
        assertFalse(hasOwner(init, "java/util/Map"));
        assertEquals(0, AlienEvoMolang1201.apply(node));
        assertClassCanBeSerialized(node);
    }

    @Test
    void generatedFacadeFactoryAndMathFunctionExposeTheGeckoLib49Contracts() {
        Map<String, byte[]> generated = AlienEvoMolang1201.generateHelperClasses();
        assertEquals(4, generated.size());

        ClassNode parser = read(generated, AlienEvoMolang1201.PARSER_COMPAT);
        assertNotNull(parser.fields.stream().filter(f -> f.name.equals("INSTANCE")
                && f.desc.equals("L" + AlienEvoMolang1201.PARSER_COMPAT + ";")).findFirst().orElse(null));
        assertNotNull(find(parser, "getVariable",
                "(Ljava/lang/String;)L" + AlienEvoMolang1201.VARIABLE_COMPAT + ";"));
        assertNotNull(find(parser, "setValue", "(Ljava/lang/String;Ljava/util/function/DoubleSupplier;)V"));
        assertNotNull(find(parser, "setMemoizedValue", "(Ljava/lang/String;Ljava/util/function/DoubleSupplier;)V"));
        assertNotNull(find(parser, "registerRollSince", "()V"));

        ClassNode variable = read(generated, AlienEvoMolang1201.VARIABLE_COMPAT);
        assertNotNull(find(variable, "get", "()D"));

        ClassNode factory = read(generated, AlienEvoMolang1201.ROLL_FACTORY);
        assertTrue(factory.interfaces.contains(AlienEvoMolang1201.MATH_FACTORY));
        assertNotNull(find(factory, "create",
                "([L" + AlienEvoMolang1201.MATH_VALUE + ";)L" + AlienEvoMolang1201.MATH_FUNCTION + ";"));

        ClassNode function = read(generated, AlienEvoMolang1201.ROLL_MATH_FUNCTION);
        assertEquals(AlienEvoMolang1201.MATH_FUNCTION, function.superName);
        assertNotNull(find(function, "getName", "()Ljava/lang/String;"));
        assertNotNull(find(function, "getMinArgs", "()I"));
        assertNotNull(find(function, "getArgs", "()[L" + AlienEvoMolang1201.MATH_VALUE + ";"));
        MethodNode compute = find(function, "compute", "()D");
        assertNotNull(compute);
        assertTrue(hasCall(compute, INVOKESTATIC,
                "net/habb/alienevo/compat/geckolib/MolangEvalContext", "get",
                "()Lnet/minecraft/world/entity/LivingEntity;"));
        assertTrue(hasCall(compute, INVOKESTATIC,
                "net/habb/alienevo/client/model/animation/aliens/arburian_pelarota/ChestSpinAnimation",
                "rollSinceKeyframe", "(Lnet/minecraft/world/entity/LivingEntity;D)F"));
    }

    @Test
    void unrelatedClassesAreNotModified() {
        ClassNode node = node("example/UnrelatedBootstrap");
        assertEquals(0, AlienEvoMolang1201.apply(node));
    }

    private static ClassNode read(Map<String, byte[]> generated, String name) {
        ClassNode node = new ClassNode();
        new ClassReader(generated.get(name + ".class")).accept(node, 0);
        return node;
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
            if (instruction instanceof FieldInsnNode field && field.owner.equals(owner)) return true;
        }
        return false;
    }

    private static boolean hasField(MethodNode method, int opcode, String owner, String name, String desc) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field && field.getOpcode() == opcode
                    && field.owner.equals(owner) && field.name.equals(name) && field.desc.equals(desc)) return true;
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

    private static void assertClassCanBeSerialized(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        assertTrue(writer.toByteArray().length > 0);
    }
}
