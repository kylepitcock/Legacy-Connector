package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * 1.2.5 to 1.4.7 hook bridges. A mod overrides a game method with its 1.2.5 signature ({@code Item.onItemUse(stack, player, world, x, y, z, side)}); 1.3 changed the signature
 * (floats for the hit position were added, {@code ArrayList} became {@code List}, an argument disappeared), so the host's 1.4.7-shaped call never reaches the override. The mod class
 * gets a second method with the 1.4.7 signature that calls the original: arguments are matched in order by type (dropped ones ignored), arguments the new signature does not carry
 * are passed as zero or null, reference types are cast, the result is cast or dropped or defaulted.
 */
public final class OverrideBridges {
    private OverrideBridges() {}

    /** Result of {@link #align}: for every old parameter the index of the new parameter it is taken from or -1 (default value), and the score. */
    public record Plan(int[] from, int score) {
        public int defaults() {
            int n = 0;
            for (int f : from) if (f < 0) n++;
            return n;
        }
    }

    private static final int NO = Integer.MIN_VALUE / 4;

    private static int pair(Type old, Type neu) {
        boolean po = old.getSort() < Type.ARRAY, pn = neu.getSort() < Type.ARRAY;
        if (po || pn) return po && pn && old.getSort() == neu.getSort() ? 4 : NO;
        return old.equals(neu) ? 4 : 1;
    }

    /** Matches the old parameters, in order, to new ones; an unmatched old parameter costs 2. Null when the parameter lists cannot be matched at all. */
    public static Plan align(Type[] old, Type[] neu) {
        int n = old.length, m = neu.length;
        int[][] best = new int[n + 1][m + 1];
        int[][] choice = new int[n + 1][m + 1]; // -1 default, else the new index
        for (int j = 0; j <= m; j++) best[n][j] = 0;
        for (int i = n - 1; i >= 0; i--) {
            for (int j = 0; j <= m; j++) {
                int b = -2 + best[i + 1][j];
                int c = -1;
                for (int k = j; k < m; k++) {
                    int p = pair(old[i], neu[k]);
                    if (p <= NO) continue;
                    int s = p + best[i + 1][k + 1];
                    if (s > b) {
                        b = s;
                        c = k;
                    }
                }
                best[i][j] = b;
                choice[i][j] = c;
            }
        }
        int[] from = new int[n];
        int j = 0;
        for (int i = 0; i < n; i++) {
            from[i] = choice[i][j];
            if (from[i] >= 0) j = from[i] + 1;
        }
        return new Plan(from, best[0][0]);
    }

    /** Return values that can be converted: equal primitives, any two references, void with anything. */
    public static boolean returnsCompatible(Type old, Type neu) {
        if (old.getSort() == Type.VOID || neu.getSort() == Type.VOID) return true;
        boolean po = old.getSort() < Type.ARRAY, pn = neu.getSort() < Type.ARRAY;
        if (po || pn) return po && pn && old.getSort() == neu.getSort();
        return true;
    }

    /**
     * The plan for bridging {@code oldDesc} to {@code newDesc}, or null with {@code reason[0]} set when the two cannot be bridged (a primitive that changed type, a result that
     * changed kind, more than half of the old parameters without a counterpart).
     */
    public static Plan plan(String oldDesc, String newDesc, String[] reason) {
        Type[] o = Type.getArgumentTypes(oldDesc), n = Type.getArgumentTypes(newDesc);
        if (!returnsCompatible(Type.getReturnType(oldDesc), Type.getReturnType(newDesc))) {
            reason[0] = "result changed from " + Type.getReturnType(oldDesc).getClassName() + " to " + Type.getReturnType(newDesc).getClassName();
            return null;
        }
        Plan p = align(o, n);
        for (int i = 0; i < o.length; i++) {
            // a primitive can be passed as zero, a reference only as null, which the override would dereference (a World the new signature no longer carries)
            if (p.from()[i] < 0 && o[i].getSort() >= Type.ARRAY && !o[i].getClassName().equals("java.lang.String")) {
                reason[0] = "needs a " + o[i].getClassName() + " the new signature does not carry";
                return null;
            }
        }
        if (p.defaults() * 2 > o.length) {
            reason[0] = p.defaults() + " of " + o.length + " parameters have no counterpart";
            return null;
        }
        return p;
    }

    /**
     * The plan for a CALL of {@code oldDesc} that has to reach the 1.4.7 method {@code newDesc}: the old arguments are matched to new parameters (unmatched old arguments dropped, unmatched new
     * parameters passed as zero or null). Null with the reason set when the result changed kind or fewer than half of the new parameters can be taken from the old arguments.
     */
    public static Plan planCall(String oldDesc, String newDesc, String[] reason) {
        Type[] o = Type.getArgumentTypes(oldDesc), n = Type.getArgumentTypes(newDesc);
        if (!returnsCompatible(Type.getReturnType(oldDesc), Type.getReturnType(newDesc))) {
            reason[0] = "result changed from " + Type.getReturnType(oldDesc).getClassName() + " to " + Type.getReturnType(newDesc).getClassName();
            return null;
        }
        Plan p = align(o, n);
        int taken = 0;
        for (int f : p.from()) if (f >= 0) taken++;
        if (taken * 2 < n.length) {
            reason[0] = "only " + taken + " of " + n.length + " new parameters have an old argument";
            return null;
        }
        return p;
    }

    private static void pushDefault(MethodVisitor mv, Type t) {
        switch (t.getSort()) {
            case Type.LONG -> mv.visitInsn(Opcodes.LCONST_0);
            case Type.FLOAT -> mv.visitInsn(Opcodes.FCONST_0);
            case Type.DOUBLE -> mv.visitInsn(Opcodes.DCONST_0);
            case Type.ARRAY, Type.OBJECT -> mv.visitInsn(Opcodes.ACONST_NULL);
            default -> mv.visitInsn(Opcodes.ICONST_0);
        }
    }

    /**
     * An adapter for a call: {@code static name(receiver, old args)} (or, for a {@code super.} call, an instance method {@code name(old args)}) that calls the 1.4.7 method
     * {@code owner.target newDesc} with the arguments taken from the old ones as {@code plan} says and converts the result back to the old return type.
     *
     * @param receiver the type of the receiver for a static adapter, null for a super call or a static target
     * @param opcode   INVOKEVIRTUAL, INVOKEINTERFACE, INVOKESPECIAL or INVOKESTATIC of the call to make
     */
    public static void emitCall(ClassVisitor cv, String name, Type receiver, boolean instanceAdapter, String oldDesc, int opcode, String owner, String target, String newDesc, boolean itf, Plan plan) {
        Type[] o = Type.getArgumentTypes(oldDesc), n = Type.getArgumentTypes(newDesc);
        Type ro = Type.getReturnType(oldDesc), rn = Type.getReturnType(newDesc);
        Type[] adapterArgs;
        if (receiver != null) {
            adapterArgs = new Type[o.length + 1];
            adapterArgs[0] = receiver;
            System.arraycopy(o, 0, adapterArgs, 1, o.length);
        } else {
            adapterArgs = o;
        }
        String adapterDesc = Type.getMethodDescriptor(ro, adapterArgs);
        int access = Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PRIVATE | (instanceAdapter ? 0 : Opcodes.ACC_STATIC);
        MethodVisitor mv = cv.visitMethod(access, name, adapterDesc, null, null);
        if (mv == null) return;
        mv.visitCode();
        int[] slot = new int[adapterArgs.length];
        int s = instanceAdapter ? 1 : 0;
        for (int i = 0; i < adapterArgs.length; i++) {
            slot[i] = s;
            s += adapterArgs[i].getSize();
        }
        int base = receiver != null ? 1 : 0;
        if (instanceAdapter) mv.visitVarInsn(Opcodes.ALOAD, 0);
        else if (receiver != null) mv.visitVarInsn(Opcodes.ALOAD, 0);
        int[] source = new int[n.length];
        java.util.Arrays.fill(source, -1);
        for (int i = 0; i < plan.from().length; i++) if (plan.from()[i] >= 0) source[plan.from()[i]] = i;
        int stack = 1;
        for (int k = 0; k < n.length; k++) {
            if (source[k] >= 0) {
                int i = source[k] + base;
                mv.visitVarInsn(adapterArgs[i].getOpcode(Opcodes.ILOAD), slot[i]);
                if (n[k].getSort() >= Type.ARRAY && !n[k].equals(adapterArgs[i])) mv.visitTypeInsn(Opcodes.CHECKCAST, n[k].getSort() == Type.ARRAY ? n[k].getDescriptor() : n[k].getInternalName());
            } else {
                pushDefault(mv, n[k]);
            }
            stack += n[k].getSize();
        }
        mv.visitMethodInsn(opcode, owner, target, newDesc, itf);
        if (ro.getSort() == Type.VOID) {
            if (rn.getSort() != Type.VOID) mv.visitInsn(rn.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP);
            mv.visitInsn(Opcodes.RETURN);
        } else {
            if (rn.getSort() == Type.VOID) pushDefault(mv, ro);
            else if (ro.getSort() >= Type.ARRAY && !ro.equals(rn)) mv.visitTypeInsn(Opcodes.CHECKCAST, ro.getSort() == Type.ARRAY ? ro.getDescriptor() : ro.getInternalName());
            mv.visitInsn(ro.getOpcode(Opcodes.IRETURN));
        }
        mv.visitMaxs(Math.max(stack, 2) + 2, s);
        mv.visitEnd();
    }

    /** Adds {@code public name newDesc} to the class, calling {@code owner.name oldDesc} on this. */
    public static void emit(ClassVisitor cv, String owner, String name, String oldDesc, String newDesc, Plan plan) {
        Type[] o = Type.getArgumentTypes(oldDesc), n = Type.getArgumentTypes(newDesc);
        Type ro = Type.getReturnType(oldDesc), rn = Type.getReturnType(newDesc);
        MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, name, newDesc, null, null);
        if (mv == null) return;
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        int[] slot = new int[n.length];
        int s = 1;
        for (int i = 0; i < n.length; i++) {
            slot[i] = s;
            s += n[i].getSize();
        }
        int stack = 1;
        for (int i = 0; i < o.length; i++) {
            int k = plan.from()[i];
            if (k >= 0) {
                mv.visitVarInsn(n[k].getOpcode(Opcodes.ILOAD), slot[k]);
                if (o[i].getSort() >= Type.ARRAY && !o[i].equals(n[k])) mv.visitTypeInsn(Opcodes.CHECKCAST, o[i].getSort() == Type.ARRAY ? o[i].getDescriptor() : o[i].getInternalName());
            } else {
                switch (o[i].getSort()) {
                    case Type.LONG -> mv.visitInsn(Opcodes.LCONST_0);
                    case Type.FLOAT -> mv.visitInsn(Opcodes.FCONST_0);
                    case Type.DOUBLE -> mv.visitInsn(Opcodes.DCONST_0);
                    case Type.ARRAY, Type.OBJECT -> mv.visitInsn(Opcodes.ACONST_NULL);
                    default -> mv.visitInsn(Opcodes.ICONST_0);
                }
            }
            stack += o[i].getSize();
        }
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, oldDesc, false);
        if (rn.getSort() == Type.VOID) {
            if (ro.getSort() != Type.VOID) mv.visitInsn(ro.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP);
            mv.visitInsn(Opcodes.RETURN);
        } else {
            if (ro.getSort() == Type.VOID) {
                switch (rn.getSort()) {
                    case Type.LONG -> mv.visitInsn(Opcodes.LCONST_0);
                    case Type.FLOAT -> mv.visitInsn(Opcodes.FCONST_0);
                    case Type.DOUBLE -> mv.visitInsn(Opcodes.DCONST_0);
                    case Type.ARRAY, Type.OBJECT -> mv.visitInsn(Opcodes.ACONST_NULL);
                    default -> mv.visitInsn(Opcodes.ICONST_0);
                }
            } else if (rn.getSort() >= Type.ARRAY && !rn.equals(ro)) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, rn.getSort() == Type.ARRAY ? rn.getDescriptor() : rn.getInternalName());
            }
            mv.visitInsn(rn.getOpcode(Opcodes.IRETURN));
        }
        mv.visitMaxs(Math.max(stack, 2) + 1, s);
        mv.visitEnd();
    }
}
