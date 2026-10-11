package org.kylepitcock.mccompat.loader.synth;

import java.util.Map;
import org.kylepitcock.mccompat.loader.shim.ClassInfo;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Picks the real constructor to call when legacy code (or a synthesized subclass) asks for one that does not exist:
 * the one that can use the most of the given arguments (identical types, in order), with the fewest defaulted
 * parameters as tie-break.
 */
public final class CtorChooser {
    private CtorChooser() {}

    /** {@code map[j]} is the index of the argument passed as parameter j, or -1 for a default value. */
    public record Choice(String desc, int[] map) {}

    public static Choice choose(ClassInfo info, Type[] args) {
        Choice best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Map.Entry<String, Integer> e : info.methods.entrySet()) {
            if (!e.getKey().startsWith("<init>(") || (e.getValue() & Opcodes.ACC_PRIVATE) != 0) continue;
            String desc = e.getKey().substring("<init>".length());
            Type[] target = Type.getArgumentTypes(desc);
            // Bridge constructors taking modern or runtime-internal objects are for the runtime's own use.
            if (!java.util.Arrays.stream(target).allMatch(CtorChooser::legacyVisible)) continue;
            int[] map = new int[target.length];
            boolean[] used = new boolean[args.length];
            int matched = 0;
            for (int j = 0; j < target.length; j++) {
                map[j] = -1;
                for (int i = 0; i < args.length; i++) {
                    if (!used[i] && args[i].equals(target[j])) {
                        used[i] = true;
                        map[j] = i;
                        matched++;
                        break;
                    }
                }
            }
            int defaultedObjects = 0;
            for (int j = 0; j < target.length; j++) if (map[j] < 0 && target[j].getSort() >= Type.ARRAY) defaultedObjects++;
            // Every used argument counts most; a null default (likely dereferenced) costs more than a zero.
            int score = matched * 4 - (target.length - matched) - defaultedObjects * 2;
            if ((e.getValue() & Opcodes.ACC_PUBLIC) == 0) score--;
            if (score > bestScore) {
                best = new Choice(desc, map);
                bestScore = score;
            }
        }
        return best;
    }

    static boolean legacyVisible(Type t) {
        if (t.getSort() == Type.ARRAY) t = t.getElementType();
        if (t.getSort() != Type.OBJECT) return true;
        String n = t.getInternalName();
        for (String p : new String[] {"mccompat/", "mccompat_synth/", "java/", "javax/", "com/google/", "io/netty/", "org/apache/",
                "com/mojang/authlib/", "org/lwjgl/", "gnu/trove/"}) {
            if (n.startsWith(p)) return true;
        }
        return false;
    }

    public static int defaultConst(Type t) {
        return switch (t.getSort()) {
            case Type.LONG -> Opcodes.LCONST_0;
            case Type.FLOAT -> Opcodes.FCONST_0;
            case Type.DOUBLE -> Opcodes.DCONST_0;
            case Type.OBJECT, Type.ARRAY -> Opcodes.ACONST_NULL;
            default -> Opcodes.ICONST_0;
        };
    }
}
