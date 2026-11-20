package org.kylepitcock.legacyconnector.loader.transform;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.IincInsnNode;

/**
 * Migrates AlienEvo's GeckoLib 4.8 MolangParser calls to the GeckoLib 4.9
 * MathParser/Variable API. A small generated facade preserves the old
 * instance-shaped getVariable/setValue calls and registers AlienEvo's
 * roll_since function through the new MathFunction.Factory contract.
 */
final class AlienEvoMolang1201 implements Opcodes {
    static final String BOOTSTRAP = "net/habb/alienevo/compat/geckolib/MolangQueryBootstrap";
    static final String CLIENT = "net/habb/alienevo/AlienEvoClient";
    static final String ROLL_FUNCTION = "net/habb/alienevo/compat/geckolib/RollSinceFunction";
    static final String MATH_PARSER = "software/bernie/geckolib/loading/math/MathParser";
    static final String MATH_VALUE = "software/bernie/geckolib/loading/math/MathValue";
    static final String MATH_FUNCTION = "software/bernie/geckolib/loading/math/function/MathFunction";
    static final String MATH_FACTORY = MATH_FUNCTION + "$Factory";
    static final String VARIABLE = "software/bernie/geckolib/loading/math/value/Variable";
    static final String OLD_PARSER = "software/bernie/geckolib/core/molang/MolangParser";
    static final String OLD_LAZY_VARIABLE = "software/bernie/geckolib/core/molang/LazyVariable";

    static final String HELPER_PACKAGE = "org/kylepitcock/legacyconnector/v1_20_1/migrate/";
    static final String PARSER_COMPAT = HELPER_PACKAGE + "MolangParserCompat";
    static final String VARIABLE_COMPAT = HELPER_PACKAGE + "MolangVariableCompat";
    static final String ROLL_FACTORY = HELPER_PACKAGE + "MolangRollSinceFactory";
    static final String ROLL_MATH_FUNCTION = HELPER_PACKAGE + "MolangRollSinceFunction";

    private static final String DOUBLE_SUPPLIER = "Ljava/util/function/DoubleSupplier;";
    private static final String VARIABLE_DESC = "L" + VARIABLE + ";";
    private static final String OLD_PARSER_DESC = "L" + OLD_PARSER + ";";
    private static final String PARSER_COMPAT_DESC = "L" + PARSER_COMPAT + ";";
    private static final String OLD_LAZY_DESC = "L" + OLD_LAZY_VARIABLE + ";";
    private static final String VARIABLE_COMPAT_DESC = "L" + VARIABLE_COMPAT + ";";

    private AlienEvoMolang1201() {}

    /** Returns the number of class/method/call-site changes applied to this class. */
    static int apply(ClassNode node) {
        Objects.requireNonNull(node, "node");
        int changes = 0;
        if (BOOTSTRAP.equals(node.name)) {
            changes += migrateBootstrap(node);
        }
        if (CLIENT.equals(node.name)) {
            changes += migrateRollSinceRegistration(node);
        }
        changes += migrateLegacyParserCalls(node);
        return changes;
    }

    private static int migrateBootstrap(ClassNode node) {
        FieldNode queries = null;
        for (FieldNode field : node.fields) {
            if (field.name.equals("QUERIES") && field.desc.equals("[Ljava/lang/String;")) {
                queries = field;
                break;
            }
        }
        if (queries == null) {
            throw new IllegalStateException("AlienEvo MolangQueryBootstrap no longer declares its inspected QUERIES string array");
        }

        MethodNode init = find(node, "init", "()V");
        if (init == null || (init.access & ACC_STATIC) == 0) {
            throw new IllegalStateException("AlienEvo MolangQueryBootstrap no longer has its inspected static init() method");
        }

        // Be idempotent: the new API call is our marker for a migrated method.
        for (var instruction : init.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKESTATIC
                    && call.owner.equals(MATH_PARSER)
                    && call.name.equals("getVariableFor")
                    && call.desc.equals("(Ljava/lang/String;)" + VARIABLE_DESC)) {
                return 0;
            }
        }

        InsnList code = new InsnList();
        LabelNode loop = new LabelNode();
        LabelNode done = new LabelNode();
        code.add(new FieldInsnNode(GETSTATIC, node.name, "QUERIES", "[Ljava/lang/String;"));
        code.add(new VarInsnNode(ASTORE, 0));
        code.add(new VarInsnNode(ALOAD, 0));
        code.add(new InsnNode(ARRAYLENGTH));
        code.add(new VarInsnNode(ISTORE, 1));
        code.add(new InsnNode(ICONST_0));
        code.add(new VarInsnNode(ISTORE, 2));
        code.add(loop);
        code.add(new VarInsnNode(ILOAD, 2));
        code.add(new VarInsnNode(ILOAD, 1));
        code.add(new JumpInsnNode(IF_ICMPGE, done));
        code.add(new VarInsnNode(ALOAD, 0));
        code.add(new VarInsnNode(ILOAD, 2));
        code.add(new InsnNode(AALOAD));
        code.add(new MethodInsnNode(INVOKESTATIC, MATH_PARSER, "getVariableFor",
                "(Ljava/lang/String;)" + VARIABLE_DESC, false));
        code.add(new InsnNode(POP));
        code.add(new IincInsnNode(2, 1));
        code.add(new JumpInsnNode(GOTO, loop));
        code.add(done);
        code.add(new InsnNode(RETURN));

        init.instructions.clear();
        init.instructions.add(code);
        init.tryCatchBlocks.clear();
        init.localVariables = null;
        init.parameters = null;
        init.signature = null;
        init.maxLocals = 3;
        init.maxStack = 2;
        return 1;
    }

    /**
     * AlienEvoClient registered a legacy McLib Function class directly in the
     * removed parser's mutable functions map. GeckoLib 4.9 instead accepts a
     * MathFunction.Factory, generated below. Replace only that exact six-node
     * registration sequence; leave the remainder of client initialization intact.
     */
    private static int migrateRollSinceRegistration(ClassNode node) {
        MethodNode init = find(node, "init", "()V");
        if (init == null || (init.access & ACC_STATIC) == 0) {
            throw new IllegalStateException("AlienEvoClient no longer declares its inspected static init() method");
        }
        for (var instruction : init.instructions.toArray()) {
            if (!(instruction instanceof FieldInsnNode parser)
                    || parser.getOpcode() != GETSTATIC
                    || !parser.owner.equals(OLD_PARSER)
                    || !parser.name.equals("INSTANCE")
                    || !parser.desc.equals(OLD_PARSER_DESC)) continue;

            var getFunctions = parser.getNext();
            var name = getFunctions == null ? null : getFunctions.getNext();
            var functionClass = name == null ? null : name.getNext();
            var put = functionClass == null ? null : functionClass.getNext();
            var pop = put == null ? null : put.getNext();
            if (!(getFunctions instanceof FieldInsnNode mapField)
                    || mapField.getOpcode() != GETFIELD
                    || !mapField.owner.equals(OLD_PARSER)
                    || !mapField.name.equals("functions")
                    || !mapField.desc.equals("Ljava/util/Map;")
                    || !(name instanceof org.objectweb.asm.tree.LdcInsnNode nameConstant)
                    || !"alienevo.roll_since".equals(nameConstant.cst)
                    || !(functionClass instanceof org.objectweb.asm.tree.LdcInsnNode classConstant)
                    || !(classConstant.cst instanceof org.objectweb.asm.Type functionType)
                    || !functionType.getInternalName().equals(ROLL_FUNCTION)
                    || !(put instanceof MethodInsnNode mapPut)
                    || mapPut.getOpcode() != INVOKEINTERFACE
                    || !mapPut.owner.equals("java/util/Map")
                    || !mapPut.name.equals("put")
                    || !mapPut.desc.equals("(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
                    || pop == null || pop.getOpcode() != POP) {
                throw new IllegalStateException("AlienEvoClient Molang roll_since registration no longer matches the inspected parser map sequence");
            }

            InsnList replacement = new InsnList();
            replacement.add(new MethodInsnNode(INVOKESTATIC, PARSER_COMPAT, "registerRollSince", "()V", false));
            init.instructions.insertBefore(parser, replacement);
            init.instructions.remove(parser);
            init.instructions.remove(mapField);
            init.instructions.remove(nameConstant);
            init.instructions.remove(classConstant);
            init.instructions.remove(mapPut);
            init.instructions.remove(pop);
            init.maxStack = Math.max(init.maxStack, 2);
            return 1;
        }
        // A second pass is allowed; once the generated registration call exists
        // the original parser/map sequence must no longer be present.
        for (var instruction : init.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKESTATIC
                    && call.owner.equals(PARSER_COMPAT)
                    && call.name.equals("registerRollSince")) return 0;
        }
        throw new IllegalStateException("AlienEvoClient no longer contains its inspected roll_since registration");
    }

    /** Redirects old parser/variable references through the generated compatibility facade. */
    private static int migrateLegacyParserCalls(ClassNode node) {
        if (BOOTSTRAP.equals(node.name)) return 0; // init() was fully replaced above
        int changes = 0;
        for (FieldNode field : node.fields) {
            String desc = replaceType(field.desc);
            if (!desc.equals(field.desc)) {
                field.desc = desc;
                changes++;
            }
        }
        for (MethodNode method : node.methods) {
            method.desc = replaceType(method.desc);
            if (method.signature != null) method.signature = replaceType(method.signature);
            if (method.localVariables != null) {
                for (var local : method.localVariables) {
                    String desc = replaceType(local.desc);
                    if (!desc.equals(local.desc)) {
                        local.desc = desc;
                        changes++;
                    }
                }
            }
            if (method.instructions == null) continue;
            for (var instruction : method.instructions.toArray()) {
                if (instruction instanceof FieldInsnNode field && field.owner.equals(OLD_PARSER)) {
                    if (field.getOpcode() == GETSTATIC && field.name.equals("INSTANCE")
                            && field.desc.equals(OLD_PARSER_DESC)) {
                        field.owner = PARSER_COMPAT;
                        field.desc = PARSER_COMPAT_DESC;
                        changes++;
                    } else if (field.name.equals("functions")) {
                        throw new IllegalStateException("Unadapted direct access to GeckoLib's removed MolangParser.functions map in " + node.name);
                    }
                } else if (instruction instanceof TypeInsnNode type && type.desc.equals(OLD_PARSER)) {
                    type.desc = PARSER_COMPAT;
                    changes++;
                } else if (instruction instanceof MethodInsnNode call) {
                    if (call.owner.equals(OLD_PARSER)) {
                        if (call.name.equals("getVariable")
                                && call.desc.equals("(Ljava/lang/String;)" + OLD_LAZY_DESC)) {
                            call.owner = PARSER_COMPAT;
                            call.desc = "(Ljava/lang/String;)" + VARIABLE_COMPAT_DESC;
                            changes++;
                        } else if ((call.name.equals("setValue") || call.name.equals("setMemoizedValue"))
                                && call.desc.equals("(Ljava/lang/String;" + DOUBLE_SUPPLIER + ")V")) {
                            call.owner = PARSER_COMPAT;
                            changes++;
                        }
                    } else if (call.owner.equals(OLD_LAZY_VARIABLE)
                            && call.name.equals("get") && call.desc.equals("()D")) {
                        call.owner = VARIABLE_COMPAT;
                        changes++;
                    }
                }
            }
            if (method.localVariables == null) continue;
        }
        return changes;
    }

    private static String replaceType(String value) {
        return value == null ? null : value.replace(OLD_PARSER, PARSER_COMPAT)
                .replace(OLD_LAZY_VARIABLE, VARIABLE_COMPAT);
    }

    /** Generated helper bytecode: no GeckoLib compile-time dependency is added to the loader. */
    static Map<String, byte[]> generateHelperClasses() {
        Map<String, byte[]> result = new LinkedHashMap<>();
        result.put(PARSER_COMPAT + ".class", write(parserCompatClass()));
        result.put(VARIABLE_COMPAT + ".class", write(variableCompatClass()));
        result.put(ROLL_FACTORY + ".class", write(rollSinceFactoryClass()));
        result.put(ROLL_MATH_FUNCTION + ".class", write(rollSinceMathFunctionClass()));
        return result;
    }

    private static ClassNode baseClass(String name, int access, String superName) {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.access = access;
        node.name = name;
        node.superName = superName;
        return node;
    }

    private static ClassNode parserCompatClass() {
        ClassNode node = baseClass(PARSER_COMPAT, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, "java/lang/Object");
        node.fields.add(new FieldNode(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, "INSTANCE", PARSER_COMPAT_DESC, null, null));

        MethodNode constructor = new MethodNode(ACC_PRIVATE, "<init>", "()V", null, null);
        constructor.instructions.add(new VarInsnNode(ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        constructor.instructions.add(new InsnNode(RETURN));
        node.methods.add(constructor);

        MethodNode clinit = new MethodNode(ACC_STATIC, "<clinit>", "()V", null, null);
        clinit.instructions.add(new TypeInsnNode(NEW, PARSER_COMPAT));
        clinit.instructions.add(new InsnNode(DUP));
        clinit.instructions.add(new MethodInsnNode(INVOKESPECIAL, PARSER_COMPAT, "<init>", "()V", false));
        clinit.instructions.add(new FieldInsnNode(PUTSTATIC, PARSER_COMPAT, "INSTANCE", PARSER_COMPAT_DESC));
        clinit.instructions.add(new InsnNode(RETURN));
        node.methods.add(clinit);

        MethodNode getVariable = new MethodNode(ACC_PUBLIC, "getVariable",
                "(Ljava/lang/String;)" + VARIABLE_COMPAT_DESC, null, null);
        getVariable.instructions.add(new VarInsnNode(ALOAD, 1));
        getVariable.instructions.add(new MethodInsnNode(INVOKESTATIC, MATH_PARSER, "getVariableFor",
                "(Ljava/lang/String;)" + VARIABLE_DESC, false));
        getVariable.instructions.add(new VarInsnNode(ASTORE, 2));
        getVariable.instructions.add(new TypeInsnNode(NEW, VARIABLE_COMPAT));
        getVariable.instructions.add(new InsnNode(DUP));
        getVariable.instructions.add(new VarInsnNode(ALOAD, 2));
        getVariable.instructions.add(new MethodInsnNode(INVOKESPECIAL, VARIABLE_COMPAT, "<init>",
                "(" + VARIABLE_DESC + ")V", false));
        getVariable.instructions.add(new InsnNode(ARETURN));
        getVariable.maxLocals = 3;
        getVariable.maxStack = 3;
        node.methods.add(getVariable);

        node.methods.add(setter("setValue"));
        node.methods.add(setter("setMemoizedValue"));

        MethodNode register = new MethodNode(ACC_PUBLIC | ACC_STATIC, "registerRollSince", "()V", null, null);
        register.instructions.add(new org.objectweb.asm.tree.LdcInsnNode("alienevo.roll_since"));
        register.instructions.add(new TypeInsnNode(NEW, ROLL_FACTORY));
        register.instructions.add(new InsnNode(DUP));
        register.instructions.add(new MethodInsnNode(INVOKESPECIAL, ROLL_FACTORY, "<init>", "()V", false));
        register.instructions.add(new MethodInsnNode(INVOKESTATIC, MATH_PARSER, "registerFunction",
                "(Ljava/lang/String;L" + MATH_FACTORY + ";)V", false));
        register.instructions.add(new InsnNode(RETURN));
        register.maxLocals = 0;
        register.maxStack = 3;
        node.methods.add(register);
        return node;
    }

    private static MethodNode setter(String name) {
        MethodNode method = new MethodNode(ACC_PUBLIC, name,
                "(Ljava/lang/String;" + DOUBLE_SUPPLIER + ")V", null, null);
        method.instructions.add(new VarInsnNode(ALOAD, 1));
        method.instructions.add(new VarInsnNode(ALOAD, 2));
        method.instructions.add(new MethodInsnNode(INVOKESTATIC, MATH_PARSER, "setVariable",
                "(Ljava/lang/String;" + DOUBLE_SUPPLIER + ")V", false));
        method.instructions.add(new InsnNode(RETURN));
        method.maxLocals = 3;
        method.maxStack = 2;
        return method;
    }

    private static ClassNode variableCompatClass() {
        ClassNode node = baseClass(VARIABLE_COMPAT, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, "java/lang/Object");
        node.fields.add(new FieldNode(ACC_PRIVATE | ACC_FINAL, "variable", VARIABLE_DESC, null, null));

        MethodNode constructor = new MethodNode(ACC_PUBLIC, "<init>", "(" + VARIABLE_DESC + ")V", null, null);
        constructor.instructions.add(new VarInsnNode(ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        constructor.instructions.add(new VarInsnNode(ALOAD, 0));
        constructor.instructions.add(new VarInsnNode(ALOAD, 1));
        constructor.instructions.add(new FieldInsnNode(PUTFIELD, VARIABLE_COMPAT, "variable", VARIABLE_DESC));
        constructor.instructions.add(new InsnNode(RETURN));
        constructor.maxLocals = 2;
        constructor.maxStack = 2;
        node.methods.add(constructor);

        MethodNode get = new MethodNode(ACC_PUBLIC, "get", "()D", null, null);
        get.instructions.add(new VarInsnNode(ALOAD, 0));
        get.instructions.add(new FieldInsnNode(GETFIELD, VARIABLE_COMPAT, "variable", VARIABLE_DESC));
        get.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, VARIABLE, "get", "()D", false));
        get.instructions.add(new InsnNode(DRETURN));
        get.maxLocals = 1;
        get.maxStack = 2;
        node.methods.add(get);
        return node;
    }

    private static ClassNode rollSinceFactoryClass() {
        ClassNode node = baseClass(ROLL_FACTORY, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, "java/lang/Object");
        node.interfaces.add(MATH_FACTORY);

        MethodNode constructor = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.instructions.add(new VarInsnNode(ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        constructor.instructions.add(new InsnNode(RETURN));
        constructor.maxLocals = 1;
        constructor.maxStack = 1;
        node.methods.add(constructor);

        MethodNode create = new MethodNode(ACC_PUBLIC, "create",
                "([L" + MATH_VALUE + ";)L" + MATH_FUNCTION + ";", null, null);
        create.instructions.add(new TypeInsnNode(NEW, ROLL_MATH_FUNCTION));
        create.instructions.add(new InsnNode(DUP));
        create.instructions.add(new VarInsnNode(ALOAD, 1));
        create.instructions.add(new MethodInsnNode(INVOKESPECIAL, ROLL_MATH_FUNCTION, "<init>",
                "([L" + MATH_VALUE + ";)V", false));
        create.instructions.add(new InsnNode(ARETURN));
        create.maxLocals = 2;
        create.maxStack = 3;
        node.methods.add(create);
        return node;
    }

    private static ClassNode rollSinceMathFunctionClass() {
        ClassNode node = baseClass(ROLL_MATH_FUNCTION, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, MATH_FUNCTION);
        node.fields.add(new FieldNode(ACC_PRIVATE | ACC_FINAL, "args", "[L" + MATH_VALUE + ";", null, null));

        MethodNode constructor = new MethodNode(ACC_PUBLIC, "<init>", "([L" + MATH_VALUE + ";)V", null, null);
        constructor.instructions.add(new VarInsnNode(ALOAD, 0));
        constructor.instructions.add(new VarInsnNode(ALOAD, 1));
        constructor.instructions.add(new MethodInsnNode(INVOKESPECIAL, MATH_FUNCTION, "<init>",
                "([L" + MATH_VALUE + ";)V", false));
        constructor.instructions.add(new VarInsnNode(ALOAD, 0));
        constructor.instructions.add(new VarInsnNode(ALOAD, 1));
        constructor.instructions.add(new FieldInsnNode(PUTFIELD, ROLL_MATH_FUNCTION, "args", "[L" + MATH_VALUE + ";"));
        constructor.instructions.add(new InsnNode(RETURN));
        constructor.maxLocals = 2;
        constructor.maxStack = 2;
        node.methods.add(constructor);

        MethodNode getName = new MethodNode(ACC_PUBLIC, "getName", "()Ljava/lang/String;", null, null);
        getName.instructions.add(new org.objectweb.asm.tree.LdcInsnNode("alienevo.roll_since"));
        getName.instructions.add(new InsnNode(ARETURN));
        getName.maxLocals = 1;
        getName.maxStack = 1;
        node.methods.add(getName);

        MethodNode getMinArgs = new MethodNode(ACC_PUBLIC, "getMinArgs", "()I", null, null);
        getMinArgs.instructions.add(new InsnNode(ICONST_1));
        getMinArgs.instructions.add(new InsnNode(IRETURN));
        getMinArgs.maxLocals = 1;
        getMinArgs.maxStack = 1;
        node.methods.add(getMinArgs);

        MethodNode getArgs = new MethodNode(ACC_PUBLIC, "getArgs", "()[L" + MATH_VALUE + ";", null, null);
        getArgs.instructions.add(new VarInsnNode(ALOAD, 0));
        getArgs.instructions.add(new FieldInsnNode(GETFIELD, ROLL_MATH_FUNCTION, "args", "[L" + MATH_VALUE + ";"));
        getArgs.instructions.add(new InsnNode(ARETURN));
        getArgs.maxLocals = 1;
        getArgs.maxStack = 1;
        node.methods.add(getArgs);

        MethodNode compute = new MethodNode(ACC_PUBLIC, "compute", "()D", null, null);
        LabelNode hasActor = new LabelNode();
        compute.instructions.add(new MethodInsnNode(INVOKESTATIC,
                "net/habb/alienevo/compat/geckolib/MolangEvalContext", "get",
                "()Lnet/minecraft/world/entity/LivingEntity;", false));
        compute.instructions.add(new VarInsnNode(ASTORE, 1));
        compute.instructions.add(new VarInsnNode(ALOAD, 1));
        compute.instructions.add(new JumpInsnNode(IFNONNULL, hasActor));
        compute.instructions.add(new InsnNode(DCONST_0));
        compute.instructions.add(new InsnNode(DRETURN));
        compute.instructions.add(hasActor);
        compute.instructions.add(new VarInsnNode(ALOAD, 1));
        compute.instructions.add(new VarInsnNode(ALOAD, 0));
        compute.instructions.add(new FieldInsnNode(GETFIELD, ROLL_MATH_FUNCTION, "args", "[L" + MATH_VALUE + ";"));
        compute.instructions.add(new InsnNode(ICONST_0));
        compute.instructions.add(new InsnNode(AALOAD));
        compute.instructions.add(new MethodInsnNode(INVOKEINTERFACE, MATH_VALUE, "get", "()D", true));
        compute.instructions.add(new MethodInsnNode(INVOKESTATIC,
                "net/habb/alienevo/client/model/animation/aliens/arburian_pelarota/ChestSpinAnimation",
                "rollSinceKeyframe", "(Lnet/minecraft/world/entity/LivingEntity;D)F", false));
        compute.instructions.add(new InsnNode(F2D));
        compute.instructions.add(new InsnNode(DRETURN));
        compute.maxLocals = 2;
        compute.maxStack = 4;
        node.methods.add(compute);
        return node;
    }

    private static MethodNode find(ClassNode node, String name, String descriptor) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(descriptor)) return method;
        }
        return null;
    }

    private static byte[] write(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                if (type1.equals(type2)) return type1;
                return "java/lang/Object";
            }
        };
        node.accept(writer);
        return writer.toByteArray();
    }
}
