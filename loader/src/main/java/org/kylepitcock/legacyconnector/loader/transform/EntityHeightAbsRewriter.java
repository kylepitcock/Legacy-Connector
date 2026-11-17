package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

/**
 * 1.7.10 worlds had no negative heights, so mod code that computes an entity's height sometimes wraps it in {@code Math.abs} to keep
 * it positive (Traincraft's slope movement: {@code setPosition(x, Math.abs(j + tan(angle) * run + yOffset + 0.3), z)}). On a modern
 * overworld (-64..319) that flips a cart below y=0 into the air: trains on slopes of a superflat world launched into the sky (#105).
 *
 * <p>Rule. Where the value passed as the y argument of {@code Entity.setPosition(DDD)V}, or stored into an entity's {@code posY}, is
 * the direct result of {@code Math.abs(D)D}, the {@code abs} call is dropped: for the heights a 1.7.10 world has the result is the
 * same, below 0 it is the height the code meant. The producer is found by data-flow analysis (the call has to be the last operation
 * of the argument expression), so an {@code abs} deeper in the expression is untouched.</p>
 */
public final class EntityHeightAbsRewriter {
    private EntityHeightAbsRewriter() {}

    /** Number of {@code Math.abs} calls dropped in the class. */
    public static int apply(ClassNode node, LegacyVersion version) {
        if (version.family() != LegacyVersion.V1_7_10) return 0;
        int dropped = 0;
        for (MethodNode m : node.methods) {
            if (m.instructions == null || m.instructions.size() == 0 || !mentionsAbs(m)) continue;
            Frame<SourceValue>[] frames;
            try {
                frames = new Analyzer<>(new SourceInterpreter()).analyze(node.name, m);
            } catch (AnalyzerException | RuntimeException e) {
                continue;
            }
            List<AbstractInsnNode> drop = new ArrayList<>();
            AbstractInsnNode[] insns = m.instructions.toArray();
            for (int i = 0; i < insns.length; i++) {
                Frame<SourceValue> f = frames[i];
                if (f == null) continue;
                SourceValue v = null;
                if (insns[i] instanceof MethodInsnNode mi && isSetPosition(mi) && f.getStackSize() >= 3) {
                    v = f.getStack(f.getStackSize() - 2); // (x, y, z): z is on top
                } else if (insns[i] instanceof FieldInsnNode fi && fi.getOpcode() == Opcodes.PUTFIELD && isPosY(fi) && f.getStackSize() >= 1) {
                    v = f.getStack(f.getStackSize() - 1);
                }
                if (v == null) continue;
                Set<AbstractInsnNode> sources = v.insns;
                if (sources.isEmpty()) continue;
                boolean allAbs = true;
                for (AbstractInsnNode s : sources) allAbs &= isAbs(s);
                if (allAbs) drop.addAll(sources);
            }
            for (AbstractInsnNode abs : drop) {
                if (abs.getPrevious() == null && abs.getNext() == null) continue;
                m.instructions.remove(abs);
                dropped++;
            }
        }
        return dropped;
    }

    private static boolean mentionsAbs(MethodNode m) {
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) if (isAbs(i)) return true;
        return false;
    }

    private static boolean isAbs(AbstractInsnNode i) {
        return i instanceof MethodInsnNode mi && mi.getOpcode() == Opcodes.INVOKESTATIC && mi.owner.equals("java/lang/Math")
                && mi.name.equals("abs") && mi.desc.equals("(D)D");
    }

    private static boolean isSetPosition(MethodInsnNode mi) {
        return (mi.name.equals("setPosition") || mi.name.equals("func_70107_b")) && mi.desc.equals("(DDD)V")
                && mi.getOpcode() != Opcodes.INVOKESTATIC;
    }

    private static boolean isPosY(FieldInsnNode fi) {
        return (fi.name.equals("posY") || fi.name.equals("field_70163_u")) && fi.desc.equals("D");
    }
}
