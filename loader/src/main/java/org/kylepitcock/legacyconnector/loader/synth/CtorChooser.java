package org.kylepitcock.legacyconnector.loader.synth;

import java.util.Map;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
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
        return choose(info, args, null);
    }

    /**
     * As {@link #choose(ClassInfo, Type[])}; {@code narrowable}, when given, also lets an argument stand for a parameter of a subtype of its
     * type (the caller casts it): 1.6.4 narrowed AI task owners from {@code EntityLiving} to {@code EntityCreature}.
     */
    public static Choice choose(ClassInfo info, Type[] args, java.util.function.BiPredicate<Type, Type> narrowable) {
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
            int narrowed = 0;
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
            if (narrowable != null) {
                for (int j = 0; j < target.length; j++) {
                    if (map[j] >= 0 || target[j].getSort() != Type.OBJECT) continue;
                    for (int i = 0; i < args.length; i++) {
                        if (!used[i] && args[i].getSort() == Type.OBJECT && narrowable.test(args[i], target[j])) {
                            used[i] = true;
                            map[j] = i;
                            matched++;
                            narrowed++;
                            break;
                        }
                    }
                }
            }
            // a number the legacy constructor took as a float stands for a double parameter (and so on): the caller converts it, nothing is defaulted
            int widened = 0;
            for (int j = 0; j < target.length; j++) {
                if (narrowable == null || map[j] >= 0 || !isNumeric(target[j])) continue;   // only callers that convert (the linker) opt in
                for (int i = 0; i < args.length; i++) {
                    if (!used[i] && widens(args[i], target[j])) {
                        used[i] = true;
                        map[j] = i;
                        matched++;
                        widened++;
                        break;
                    }
                }
            }
            int defaultedObjects = 0;
            for (int j = 0; j < target.length; j++) if (map[j] < 0 && target[j].getSort() >= Type.ARRAY) defaultedObjects++;
            // Every used argument counts most; a null default (likely dereferenced) costs more than a zero.
            int score = matched * 4 - narrowed - widened - (target.length - matched) - defaultedObjects * 2;
            if ((e.getValue() & Opcodes.ACC_PUBLIC) == 0) score--;
            if (score > bestScore) {
                best = new Choice(desc, map);
                bestScore = score;
            }
        }
        return best;
    }

    private static boolean isNumeric(Type t) {
        return t.getSort() == Type.INT || t.getSort() == Type.LONG || t.getSort() == Type.FLOAT || t.getSort() == Type.DOUBLE;
    }

    /** Lossless primitive widening: int to long, float or double, long to double... and float to double. */
    public static boolean widens(Type from, Type to) {
        int f = from.getSort(), t = to.getSort();
        if (f == t) return false;
        return switch (f) {
            case Type.INT -> t == Type.LONG || t == Type.FLOAT || t == Type.DOUBLE;
            case Type.FLOAT -> t == Type.DOUBLE;
            case Type.LONG -> t == Type.DOUBLE;
            default -> false;
        };
    }

    /** The conversion instruction for {@link #widens}, or -1. */
    public static int widenOpcode(Type from, Type to) {
        int f = from.getSort(), t = to.getSort();
        if (f == Type.INT) return t == Type.LONG ? Opcodes.I2L : t == Type.FLOAT ? Opcodes.I2F : t == Type.DOUBLE ? Opcodes.I2D : -1;
        if (f == Type.FLOAT && t == Type.DOUBLE) return Opcodes.F2D;
        if (f == Type.LONG && t == Type.DOUBLE) return Opcodes.L2D;
        return -1;
    }

    static boolean legacyVisible(Type t) {
        if (t.getSort() == Type.ARRAY) t = t.getElementType();
        if (t.getSort() != Type.OBJECT) return true;
        String n = t.getInternalName();
        for (String p : new String[] {"legacyconnector/", "legacyconnector_synth/", "java/", "javax/", "com/google/", "io/netty/", "org/apache/",
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
