package org.kylepitcock.legacyconnector.loader.transform;

import java.util.Objects;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.IincInsnNode;

/**
 * Migrates AlienEvo's startup Molang query pre-registration from GeckoLib's
 * removed 1.20.1 MolangParser singleton to GeckoLib 4.9's MathParser registry.
 *
 * The original method only calls getVariable for each entry in its own QUERIES
 * array. It does not install custom functions or suppliers, so preserving that
 * loop with MathParser.getVariableFor is behaviorally equivalent.
 */
final class AlienEvoMolang1201 implements Opcodes {
    static final String BOOTSTRAP = "net/habb/alienevo/compat/geckolib/MolangQueryBootstrap";
    static final String MATH_PARSER = "software/bernie/geckolib/loading/math/MathParser";
    static final String VARIABLE = "software/bernie/geckolib/loading/math/value/Variable";
    static final String OLD_PARSER = "software/bernie/geckolib/core/molang/MolangParser";

    private AlienEvoMolang1201() {}

    /** Returns the number of methods migrated. */
    static int apply(ClassNode node) {
        Objects.requireNonNull(node, "node");
        if (!BOOTSTRAP.equals(node.name)) return 0;

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

        MethodNode init = null;
        for (MethodNode method : node.methods) {
            if (method.name.equals("init") && method.desc.equals("()V")
                    && (method.access & ACC_STATIC) != 0) {
                init = method;
                break;
            }
        }
        if (init == null) {
            throw new IllegalStateException("AlienEvo MolangQueryBootstrap no longer has its inspected static init() method");
        }

        // Be idempotent: the new API call is our marker for a migrated method.
        for (var instruction : init.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKESTATIC
                    && call.owner.equals(MATH_PARSER)
                    && call.name.equals("getVariableFor")
                    && call.desc.equals("(Ljava/lang/String;)L" + VARIABLE + ";")) {
                return 0;
            }
        }

        InsnList code = new InsnList();
        LabelNode loop = new LabelNode();
        LabelNode done = new LabelNode();

        // String[] queries = QUERIES;
        code.add(new FieldInsnNode(GETSTATIC, node.name, "QUERIES", "[Ljava/lang/String;"));
        code.add(new VarInsnNode(ASTORE, 0));
        // int length = queries.length;
        code.add(new VarInsnNode(ALOAD, 0));
        code.add(new InsnNode(ARRAYLENGTH));
        code.add(new VarInsnNode(ISTORE, 1));
        // for (int i = 0; i < length; i++) MathParser.getVariableFor(queries[i]);
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
                "(Ljava/lang/String;)L" + VARIABLE + ";", false));
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
}
