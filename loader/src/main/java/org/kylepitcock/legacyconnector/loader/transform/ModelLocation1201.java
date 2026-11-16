package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

/**
 * 1.20.1's {@code ModelResourceLocation} extends {@code ResourceLocation}; 1.21.1's is a record holding one ({@code id()}). Code that hands a model location to
 * something that wants a ResourceLocation (an argument, a field store, the return value, the receiver of a ResourceLocation method) fails verification.
 * A type analysis of each method finds the places where a value that is a ModelResourceLocation is consumed as a ResourceLocation and converts it there
 * ({@code ModelLocationCompat.id}); values deeper in the stack than the top are taken out through temporary locals.
 */
final class ModelLocation1201 {
    private ModelLocation1201() {}

    private static final String MRL = "net/minecraft/client/resources/model/ModelResourceLocation";
    private static final String RL = "net/minecraft/resources/ResourceLocation";
    private static final String HELPER = "org/kylepitcock/legacyconnector/v1_20_1/migrate/ModelLocationCompat";
    private static final Type MRL_T = Type.getObjectType(MRL);
    private static final Type RL_T = Type.getObjectType(RL);

    /** Interpreter that keeps the class of object values created from known types (the basic one reduces them all to one reference value). */
    private static final class Typed extends BasicInterpreter {
        Typed() {
            super(Opcodes.ASM9);
        }

        @Override
        public BasicValue newValue(Type type) {
            if (type != null && type.getSort() == Type.OBJECT) return new BasicValue(type);
            return super.newValue(type);
        }

        @Override
        public BasicValue merge(BasicValue a, BasicValue b) {
            if (a.equals(b)) return a;
            return super.merge(a, b);
        }
    }

    /** @return the number of conversions inserted */
    static int apply(ClassNode node) {
        if (Boolean.getBoolean("legacyconnector.skipModelLocation")) return 0; // diagnostics: what the offline verifier reports without the pass
        if (!mentions(node)) return 0;
        int total = 0;
        for (MethodNode m : node.methods) {
            if (m.instructions == null || m.instructions.size() == 0) continue;
            total += method(node, m);
        }
        return total;
    }

    private static boolean mentions(ClassNode node) {
        for (MethodNode m : node.methods) {
            if (m.desc.contains(MRL)) return true;
            if (m.instructions == null) continue;
            for (AbstractInsnNode in : m.instructions) {
                if (in instanceof MethodInsnNode mi && (mi.desc.contains(MRL) || mi.owner.equals(MRL))) return true;
                if (in instanceof FieldInsnNode fi && (fi.desc.contains(MRL) || fi.owner.equals(MRL))) return true;
                if (in instanceof InvokeDynamicInsnNode d && d.desc.contains(MRL)) return true;
            }
        }
        return false;
    }

    /** A value to convert: the instruction that consumes it, the types of everything it consumes (bottom first) and which of those is the value. */
    private record Fix(AbstractInsnNode at, Type[] types, int index) {}

    private static boolean isMrl(BasicValue v) {
        return v != null && MRL_T.equals(v.getType());
    }

    private static int method(ClassNode owner, MethodNode m) {
        Frame<BasicValue>[] frames;
        try {
            frames = new Analyzer<>(new Typed()).analyze(owner.name, m);
        } catch (AnalyzerException | RuntimeException e) {
            return 0;
        }
        List<Fix> fixes = new ArrayList<>();
        AbstractInsnNode[] insns = m.instructions.toArray();
        Type ret = Type.getReturnType(m.desc);
        Type[] one = {RL_T};
        for (int i = 0; i < insns.length; i++) {
            Frame<BasicValue> f = frames[i];
            if (f == null) continue;
            AbstractInsnNode in = insns[i];
            if (in instanceof MethodInsnNode mi) {
                invoke(fixes, f, in, mi.desc, mi.getOpcode() != Opcodes.INVOKESTATIC ? Type.getObjectType(mi.owner) : null);
            } else if (in instanceof InvokeDynamicInsnNode d) {
                invoke(fixes, f, in, d.desc, null);
            } else if (in instanceof FieldInsnNode fi && (fi.getOpcode() == Opcodes.PUTFIELD || fi.getOpcode() == Opcodes.PUTSTATIC) && fi.desc.equals("L" + RL + ";")) {
                if (isMrl(f.getStack(f.getStackSize() - 1))) fixes.add(new Fix(in, one, 0));
            } else if (in.getOpcode() == Opcodes.ARETURN && ret.equals(RL_T)) {
                if (isMrl(f.getStack(f.getStackSize() - 1))) fixes.add(new Fix(in, one, 0));
            }
        }
        int done = 0;
        for (Fix fix : fixes) {
            InsnList seq = new InsnList();
            int n = fix.types.length;
            int[] slot = new int[n];
            int next = m.maxLocals;
            // what sits above the value goes to temporary locals (top first), the value is converted, then it comes back
            for (int j = n - 1; j > fix.index; j--) {
                slot[j] = next;
                seq.add(new VarInsnNode(fix.types[j].getOpcode(Opcodes.ISTORE), next));
                next += fix.types[j].getSize();
            }
            seq.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, "id", "(L" + MRL + ";)L" + RL + ";", false));
            for (int j = fix.index + 1; j < n; j++) seq.add(new VarInsnNode(fix.types[j].getOpcode(Opcodes.ILOAD), slot[j]));
            m.maxLocals = Math.max(m.maxLocals, next);
            m.instructions.insertBefore(fix.at, seq);
            done++;
        }
        return done;
    }

    /** The arguments (receiver first) of a call that are model locations where the descriptor wants a ResourceLocation. */
    private static void invoke(List<Fix> fixes, Frame<BasicValue> f, AbstractInsnNode at, String desc, Type receiver) {
        Type[] params = Type.getArgumentTypes(desc);
        int n = params.length + (receiver != null ? 1 : 0);
        Type[] all = new Type[n];
        int k = 0;
        if (receiver != null) all[k++] = receiver;
        for (Type p : params) all[k++] = p;
        int top = f.getStackSize() - n;
        for (int j = 0; j < n; j++) {
            if (all[j].equals(RL_T) && isMrl(f.getStack(top + j))) fixes.add(new Fix(at, all, j));
        }
    }
}
