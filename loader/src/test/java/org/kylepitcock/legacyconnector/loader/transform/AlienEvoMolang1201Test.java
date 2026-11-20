package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
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
        // Stack-map frame types are part of the class too: HologramRenderer's
        // loop stores the legacy parser in a local across a branch.
        render.instructions.add(new InsnNode(ACONST_NULL));
        render.instructions.add(new VarInsnNode(ASTORE, 1));
        render.instructions.add(new InsnNode(ACONST_NULL));
        render.instructions.add(new VarInsnNode(ASTORE, 2));
        render.instructions.add(new LabelNode());
        FrameNode oldFrame = new FrameNode(F_APPEND, 2,
                new Object[] {AlienEvoMolang1201.OLD_PARSER, AlienEvoMolang1201.OLD_LAZY_VARIABLE}, 0, null);
        render.instructions.add(oldFrame);
        render.instructions.add(new InsnNode(RETURN));
        render.maxLocals = 3;
        render.maxStack = 2;

        assertTrue(AlienEvoMolang1201.apply(node) >= 9);
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
        assertEquals(AlienEvoMolang1201.PARSER_COMPAT, oldFrame.local.get(0));
        assertEquals(AlienEvoMolang1201.VARIABLE_COMPAT, oldFrame.local.get(1));
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
        assertEquals(5, generated.size());

        ClassNode parser = read(generated, AlienEvoMolang1201.PARSER_COMPAT);
        assertNotNull(parser.fields.stream().filter(f -> f.name.equals("INSTANCE")
                && f.desc.equals("L" + AlienEvoMolang1201.PARSER_COMPAT + ";")).findFirst().orElse(null));
        assertNotNull(find(parser, "getVariable",
                "(Ljava/lang/String;)L" + AlienEvoMolang1201.VARIABLE_COMPAT + ";"));
        assertNotNull(find(parser, "setValue", "(Ljava/lang/String;Ljava/util/function/DoubleSupplier;)V"));
        MethodNode memoizedSetter = find(parser, "setMemoizedValue",
                "(Ljava/lang/String;Ljava/util/function/DoubleSupplier;)V");
        assertNotNull(memoizedSetter);
        assertTrue(hasCall(memoizedSetter, INVOKESPECIAL, AlienEvoMolang1201.MEMOIZED_SUPPLIER, "<init>",
                "(Ljava/util/function/DoubleSupplier;)V"));
        assertNotNull(find(parser, "registerRollSince", "()V"));

        ClassNode memoized = read(generated, AlienEvoMolang1201.MEMOIZED_SUPPLIER);
        assertTrue(memoized.interfaces.contains("java/util/function/DoubleSupplier"));
        assertNotNull(find(memoized, "getAsDouble", "()D"));

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
        MethodNode isMutable = find(function, "isMutable", "([L" + AlienEvoMolang1201.MATH_VALUE + ";)Z");
        assertNotNull(isMutable);
        assertEquals(ICONST_1, isMutable.instructions.getFirst().getOpcode(),
                "roll_since depends on the current render actor and must never be cached as immutable");
        assertEquals(IRETURN, isMutable.instructions.getLast().getOpcode());

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
    void mclibIValueKeyframeTypesAndNegatingLambdaMoveToMathValue() {
        ClassNode node = node("net/habb/alienevo/client/geo/MirroredGeoAnimations");
        node.signature = "Ljava/lang/Object;";
        node.fields.add(new FieldNode(ACC_PRIVATE, "frames", "Ljava/util/List;",
                "Ljava/util/List<Lsoftware/bernie/geckolib/core/keyframe/Keyframe<L"
                        + AlienEvoMolang1201.OLD_MCLIB_IVALUE + ";>;>;", null));

        MethodNode negate = method(node, ACC_PRIVATE | ACC_STATIC, "negate",
                "(L" + AlienEvoMolang1201.OLD_MCLIB_IVALUE + ";)L" + AlienEvoMolang1201.OLD_MCLIB_IVALUE + ";");
        negate.signature = "(L" + AlienEvoMolang1201.OLD_MCLIB_IVALUE + ";)L"
                + AlienEvoMolang1201.OLD_MCLIB_IVALUE + ";";
        negate.instructions.add(new VarInsnNode(ALOAD, 0));
        negate.instructions.add(new TypeInsnNode(CHECKCAST, AlienEvoMolang1201.OLD_MCLIB_CONSTANT));
        negate.instructions.add(new InsnNode(POP));
        negate.instructions.add(new VarInsnNode(ALOAD, 0));
        negate.instructions.add(new MethodInsnNode(INVOKEINTERFACE, AlienEvoMolang1201.OLD_MCLIB_IVALUE,
                "get", "()D", true));
        negate.instructions.add(new InsnNode(POP2));
        negate.instructions.add(new VarInsnNode(ALOAD, 0));
        Handle metafactory = new Handle(H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                        + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)"
                        + "Ljava/lang/invoke/CallSite;", false);
        Handle implementation = new Handle(H_INVOKESTATIC, node.name, "lambda$negate$2",
                "(L" + AlienEvoMolang1201.OLD_MCLIB_IVALUE + ";)D", false);
        negate.instructions.add(new InvokeDynamicInsnNode("get",
                "(L" + AlienEvoMolang1201.OLD_MCLIB_IVALUE + ";)L" + AlienEvoMolang1201.OLD_MCLIB_IVALUE + ";",
                metafactory, Type.getMethodType("()D"), implementation, Type.getMethodType("()D")));
        negate.instructions.add(new InsnNode(POP));
        negate.instructions.add(new InsnNode(ACONST_NULL));
        negate.instructions.add(new InsnNode(ARETURN));
        negate.maxLocals = 1;
        negate.maxStack = 2;

        MethodNode lambda = method(node, ACC_PRIVATE | ACC_STATIC, "lambda$negate$2",
                "(L" + AlienEvoMolang1201.OLD_MCLIB_IVALUE + ";)D");
        lambda.instructions.add(new VarInsnNode(ALOAD, 0));
        lambda.instructions.add(new MethodInsnNode(INVOKEINTERFACE, AlienEvoMolang1201.OLD_MCLIB_IVALUE,
                "get", "()D", true));
        lambda.instructions.add(new InsnNode(DNEG));
        lambda.instructions.add(new InsnNode(DRETURN));
        lambda.maxLocals = 1;
        lambda.maxStack = 2;

        assertTrue(AlienEvoMolang1201.apply(node) >= 8);
        assertTrue(node.fields.getFirst().signature.contains("L" + AlienEvoMolang1201.MATH_VALUE + ";"));
        assertEquals("(L" + AlienEvoMolang1201.MATH_VALUE + ";)L"
                + AlienEvoMolang1201.MATH_VALUE + ";", negate.desc);
        assertEquals("(L" + AlienEvoMolang1201.MATH_VALUE + ";)L"
                + AlienEvoMolang1201.MATH_VALUE + ";", negate.signature);
        assertTrue(hasType(negate, CHECKCAST, AlienEvoMolang1201.GECKO_CONSTANT));
        assertTrue(hasCall(negate, INVOKEINTERFACE, AlienEvoMolang1201.MATH_VALUE, "get", "()D"));
        InvokeDynamicInsnNode dynamic = null;
        for (var instruction : negate.instructions.toArray()) {
            if (instruction instanceof InvokeDynamicInsnNode indy) dynamic = indy;
        }
        assertNotNull(dynamic);
        assertEquals("(L" + AlienEvoMolang1201.MATH_VALUE + ";)L"
                + AlienEvoMolang1201.MATH_VALUE + ";", dynamic.desc);
        assertEquals("(L" + AlienEvoMolang1201.MATH_VALUE + ";)D",
                ((Handle) dynamic.bsmArgs[1]).getDesc());
        assertEquals("(L" + AlienEvoMolang1201.MATH_VALUE + ";)D", lambda.desc);
        assertTrue(hasCall(lambda, INVOKEINTERFACE, AlienEvoMolang1201.MATH_VALUE, "get", "()D"));
        assertFalse(hasOwner(negate, AlienEvoMolang1201.OLD_MCLIB_IVALUE));
        assertFalse(hasOwner(lambda, AlienEvoMolang1201.OLD_MCLIB_IVALUE));
        assertEquals(0, AlienEvoMolang1201.apply(node));
        assertClassCanBeSerialized(node);
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

    private static boolean hasType(MethodNode method, int opcode, String desc) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof org.objectweb.asm.tree.TypeInsnNode type
                    && type.getOpcode() == opcode && type.desc.equals(desc)) return true;
        }
        return false;
    }

    private static void assertClassCanBeSerialized(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        assertTrue(writer.toByteArray().length > 0);
    }
}
