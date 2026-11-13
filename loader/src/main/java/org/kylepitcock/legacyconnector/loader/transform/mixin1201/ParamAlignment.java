package org.kylepitcock.legacyconnector.loader.transform.mixin1201;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.objectweb.asm.Type;

/**
 * Proves that a method of 1.21.1 is an older one with parameters added: the old parameter types must reappear, in order, among the new ones, in exactly one
 * way, and the return type must be unchanged. Where the alignment is ambiguous (two equal types) nothing is claimed.
 *
 * <p>Portions adapted from Retromod (https://github.com/Bownlux/Retromod), MIT License, Copyright (c) 2026 Bownlux
 * ({@code AutomaticMixinTranslator.inferCompleteInsertions / uniqueAlignments / insertionsFromAlignment}).
 */
public final class ParamAlignment {
    /** A parameter the new method has in front of old parameter number {@code index} (equal to the old count for a trailing one). */
    public record Insert(int index, String desc) {}

    /** A small cap keeps accidental long-descriptor subsequence matches out of the automatic repair. */
    public static final int MAX_INSERTED = 3;

    private ParamAlignment() {}

    /** The insertions turning {@code oldDesc} into {@code newDesc}, or null when that is not provable (return type changed, removed or reordered parameters, ambiguity). */
    public static List<Insert> pureInsertions(String oldDesc, String newDesc) {
        Type[] o = args(oldDesc);
        Type[] n = args(newDesc);
        if (o == null || n == null || n.length <= o.length || !Type.getReturnType(oldDesc).equals(Type.getReturnType(newDesc))) return null;
        List<Insert> ins = insertions(Arrays.asList(o), Arrays.asList(n));
        return ins == null || ins.isEmpty() || ins.size() > MAX_INSERTED ? null : ins;
    }

    static Type[] args(String desc) {
        try {
            return Type.getArgumentTypes(desc);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static List<Insert> insertions(List<Type> oldArgs, List<Type> newArgs) {
        if (oldArgs.isEmpty()) {
            List<Insert> out = new ArrayList<>();
            for (Type t : newArgs) out.add(new Insert(0, t.getDescriptor()));
            return out;
        }
        List<int[]> found = new ArrayList<>(2);
        collect(oldArgs, newArgs, 0, 0, new int[oldArgs.size()], found);
        if (found.size() != 1) return null;
        int[] pos = found.get(0);
        List<Insert> out = new ArrayList<>();
        int oldIndex = 0;
        for (int i = 0; i < newArgs.size(); i++) {
            if (oldIndex < pos.length && pos[oldIndex] == i) oldIndex++;
            else out.add(new Insert(oldIndex, newArgs.get(i).getDescriptor()));
        }
        return out;
    }

    /** Finds at most two alignments; two are enough to prove ambiguity. */
    private static void collect(List<Type> o, List<Type> n, int oi, int ni, int[] pos, List<int[]> out) {
        if (out.size() >= 2) return;
        if (oi == o.size()) {
            out.add(Arrays.copyOf(pos, pos.length));
            return;
        }
        for (int i = ni; i <= n.size() - (o.size() - oi); i++) {
            if (!o.get(oi).equals(n.get(i))) continue;
            pos[oi] = i;
            collect(o, n, oi + 1, i + 1, pos, out);
            if (out.size() >= 2) return;
        }
    }

    /** The descriptor after the insertions (null when an index is out of range). */
    public static String apply(String desc, List<Insert> inserts) {
        Type[] a = args(desc);
        if (a == null) return null;
        List<Type> list = new ArrayList<>(Arrays.asList(a));
        List<Insert> sorted = new ArrayList<>(inserts);
        sorted.sort(java.util.Comparator.comparingInt(Insert::index));
        for (int k = sorted.size() - 1; k >= 0; k--) {
            if (sorted.get(k).index() < 0 || sorted.get(k).index() > list.size()) return null;
            list.add(sorted.get(k).index(), Type.getType(sorted.get(k).desc()));
        }
        return Type.getMethodDescriptor(Type.getReturnType(desc), list.toArray(new Type[0]));
    }
}
