package org.kylepitcock.legacyconnector.loader.transform.mixin1201;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.LocalVariableAnnotationNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeAnnotationNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Gives a mixin handler the parameters its target gained in 1.21.1: the new parameter is inserted into the handler descriptor at the position it has in the
 * target (the handler ignores it), the local variable slots of the handler body move up by its width, parameter annotations move with their parameter, and a
 * MixinExtras {@code @Local(index = n)} (a slot of the target) moves with the target's slots. The caller recomputes the frames of the class.
 *
 * <p>Portions adapted from Retromod (https://github.com/Bownlux/Retromod), MIT License, Copyright (c) 2026 Bownlux
 * ({@code MixinHandlerResignature.insertRawParams}, {@code MixinLocalSlotRepair}).
 */
public final class HandlerResigner {
    private static final String CALLBACK_INFO = "org/spongepowered/asm/mixin/injection/callback/CallbackInfo";
    private static final String CALLBACK_INFO_RETURNABLE = "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";
    private static final String LOCAL = "Lcom/llamalad7/mixinextras/sugar/Local;";

    private HandlerResigner() {}

    /** Index of the {@code CallbackInfo(Returnable)} parameter, or -1. */
    public static int callbackIndex(Type[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].getSort() == Type.OBJECT) {
                String in = args[i].getInternalName();
                if (CALLBACK_INFO.equals(in) || CALLBACK_INFO_RETURNABLE.equals(in)) return i;
            }
        }
        return -1;
    }

    /**
     * Whether the insertion can be made without changing what the handler means: no parameter annotation of Mixin that depends on position, and a
     * {@code @Local} capture that still selects the same variable (it selects among the locals of its type, so only an inserted parameter of exactly that
     * type disturbs it).
     */
    public static boolean canResign(MethodNode handler, List<ParamAlignment.Insert> inserts) {
        Type[] args;
        try {
            args = Type.getArgumentTypes(handler.desc);
        } catch (RuntimeException e) {
            return false;
        }
        int callback = callbackIndex(args);
        return capturesSurvive(handler.visibleParameterAnnotations, args, callback, inserts)
                && capturesSurvive(handler.invisibleParameterAnnotations, args, callback, inserts);
    }

    private static boolean capturesSurvive(List<AnnotationNode>[] parameters, Type[] args, int callback, List<ParamAlignment.Insert> inserts) {
        if (parameters == null) return true;
        if (parameters.length != args.length) return false; // abbreviated annotable-parameter table
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i] == null) continue;
            for (AnnotationNode a : parameters[i]) {
                String d = a.desc;
                if (d == null) continue;
                boolean semantic = d.startsWith("Lorg/spongepowered/asm/mixin/") || d.startsWith("Lcom/llamalad7/mixinextras/");
                if (!semantic) continue;
                if (callback < 0 || i <= callback) return false; // sugar parameters follow the CallbackInfo; a position-bound annotation before it is not a shape we know
                if (!LOCAL.equals(d)) continue; // @Share, @Cancellable...: not tied to the target's parameter list
                if (value(a, "index") instanceof Integer idx && idx >= 0) continue; // pinned slot: shifted below
                Object explicit = value(a, "type");
                String captured = explicit instanceof Type t && t.getSort() == Type.OBJECT ? t.getDescriptor() : args[i].getDescriptor();
                for (ParamAlignment.Insert in : inserts) if (in.desc().equals(captured)) return false;
            }
        }
        return true;
    }

    /**
     * For an injector whose handler does not mirror the target's parameters: every {@code @Local} capture must still select the same variable (none pinned
     * by slot, none of an inserted type).
     */
    public static boolean localsSurvive(MethodNode handler, List<ParamAlignment.Insert> inserts) {
        Type[] args;
        try {
            args = Type.getArgumentTypes(handler.desc);
        } catch (RuntimeException e) {
            return false;
        }
        for (List<AnnotationNode>[] parameters : Arrays.asList(handler.visibleParameterAnnotations, handler.invisibleParameterAnnotations)) {
            if (parameters == null) continue;
            for (int i = 0; i < parameters.length && i < args.length; i++) {
                if (parameters[i] == null) continue;
                for (AnnotationNode a : parameters[i]) {
                    if (!LOCAL.equals(a.desc)) continue;
                    if (value(a, "index") instanceof Integer idx && idx >= 0) return false;
                    Object explicit = value(a, "type");
                    String captured = explicit instanceof Type t && t.getSort() == Type.OBJECT ? t.getDescriptor() : args[i].getDescriptor();
                    for (ParamAlignment.Insert in : inserts) if (in.desc().equals(captured)) return false;
                }
            }
        }
        return true;
    }

    /** Inserts the parameters into the handler. Returns false (handler untouched) when it cannot be done safely. */
    public static boolean resign(MethodNode handler, List<ParamAlignment.Insert> inserts) {
        if (inserts == null || inserts.isEmpty() || handler.desc == null || !canResign(handler, inserts)) return false;
        Type[] args = Type.getArgumentTypes(handler.desc);
        Type ret = Type.getReturnType(handler.desc);
        for (ParamAlignment.Insert ins : inserts) if (ins.index() < 0 || ins.index() > args.length) return false;
        boolean isStatic = (handler.access & Opcodes.ACC_STATIC) != 0;
        int[] paramSlot = new int[args.length + 1];
        paramSlot[0] = isStatic ? 0 : 1;
        for (int i = 0; i < args.length; i++) paramSlot[i + 1] = paramSlot[i] + args[i].getSize();

        List<ParamAlignment.Insert> sorted = new ArrayList<>(inserts);
        sorted.sort(Comparator.comparingInt(ParamAlignment.Insert::index));
        int[] insSlot = new int[sorted.size()];
        int[] insWidth = new int[sorted.size()];
        for (int k = 0; k < sorted.size(); k++) {
            insSlot[k] = paramSlot[sorted.get(k).index()];
            insWidth[k] = Type.getType(sorted.get(k).desc()).getSize();
        }
        List<AbstractInsnNode> frames = new ArrayList<>();
        for (AbstractInsnNode insn = handler.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof VarInsnNode v) v.var += shiftFor(v.var, insSlot, insWidth);
            else if (insn instanceof IincInsnNode ii) ii.var += shiftFor(ii.var, insSlot, insWidth);
            else if (insn instanceof FrameNode) frames.add(insn);
        }
        for (AbstractInsnNode f : frames) handler.instructions.remove(f); // the class is written with recomputed frames
        if (handler.localVariables != null) for (LocalVariableNode lv : handler.localVariables) lv.index += shiftFor(lv.index, insSlot, insWidth);
        shiftLocalVariableAnnotations(handler.visibleLocalVariableAnnotations, insSlot, insWidth);
        shiftLocalVariableAnnotations(handler.invisibleLocalVariableAnnotations, insSlot, insWidth);
        shiftPinnedLocals(handler.visibleParameterAnnotations, insSlot, insWidth, paramSlot[0]);
        shiftPinnedLocals(handler.invisibleParameterAnnotations, insSlot, insWidth, paramSlot[0]);

        List<Type> newArgs = new ArrayList<>(Arrays.asList(args));
        for (int k = sorted.size() - 1; k >= 0; k--) newArgs.add(sorted.get(k).index(), Type.getType(sorted.get(k).desc()));
        handler.visibleParameterAnnotations = shiftParameterAnnotations(handler.visibleParameterAnnotations, args.length, sorted);
        handler.invisibleParameterAnnotations = shiftParameterAnnotations(handler.invisibleParameterAnnotations, args.length, sorted);
        if (handler.visibleParameterAnnotations != null) handler.visibleAnnotableParameterCount = newArgs.size();
        if (handler.invisibleParameterAnnotations != null) handler.invisibleAnnotableParameterCount = newArgs.size();
        shiftFormalParameterTypeAnnotations(handler.visibleTypeAnnotations, sorted);
        shiftFormalParameterTypeAnnotations(handler.invisibleTypeAnnotations, sorted);
        handler.desc = Type.getMethodDescriptor(ret, newArgs.toArray(new Type[0]));
        handler.parameters = null;
        handler.signature = null;
        int total = 0;
        for (int w : insWidth) total += w;
        handler.maxLocals += total;
        return true;
    }

    @SuppressWarnings("unchecked")
    private static List<AnnotationNode>[] shiftParameterAnnotations(List<AnnotationNode>[] annotations, int oldCount, List<ParamAlignment.Insert> inserts) {
        if (annotations == null || annotations.length != oldCount) return annotations;
        List<AnnotationNode>[] shifted = (List<AnnotationNode>[]) new List<?>[oldCount + inserts.size()];
        for (int oldIndex = 0; oldIndex < annotations.length; oldIndex++) {
            int newIndex = oldIndex;
            for (ParamAlignment.Insert in : inserts) if (in.index() <= oldIndex) newIndex++;
            shifted[newIndex] = annotations[oldIndex];
        }
        return shifted;
    }

    private static void shiftFormalParameterTypeAnnotations(List<TypeAnnotationNode> annotations, List<ParamAlignment.Insert> inserts) {
        if (annotations == null) return;
        for (TypeAnnotationNode a : annotations) {
            TypeReference ref = new TypeReference(a.typeRef);
            if (ref.getSort() != TypeReference.METHOD_FORMAL_PARAMETER) continue;
            int oldIndex = ref.getFormalParameterIndex();
            int newIndex = oldIndex;
            for (ParamAlignment.Insert in : inserts) if (in.index() <= oldIndex) newIndex++;
            a.typeRef = TypeReference.newFormalParameterReference(newIndex).getValue();
        }
    }

    private static void shiftLocalVariableAnnotations(List<LocalVariableAnnotationNode> annotations, int[] slots, int[] widths) {
        if (annotations == null) return;
        for (LocalVariableAnnotationNode a : annotations) for (int i = 0; i < a.index.size(); i++) a.index.set(i, a.index.get(i) + shiftFor(a.index.get(i), slots, widths));
    }

    /** A {@code @Local(index = n)} names a slot of the target method, whose slots moved with the insertion; an index below the first argument slot is inert and stays. */
    private static void shiftPinnedLocals(List<AnnotationNode>[] parameters, int[] slots, int[] widths, int base) {
        if (parameters == null) return;
        for (List<AnnotationNode> p : parameters) {
            if (p == null) continue;
            for (AnnotationNode a : p) {
                if (!LOCAL.equals(a.desc) || a.values == null) continue;
                for (int i = 0; i + 1 < a.values.size(); i += 2) {
                    if ("index".equals(a.values.get(i)) && a.values.get(i + 1) instanceof Integer idx && idx >= base) a.values.set(i + 1, idx + shiftFor(idx, slots, widths));
                }
            }
        }
    }

    private static Object value(AnnotationNode a, String name) {
        if (a.values == null) return null;
        for (int i = 0; i + 1 < a.values.size(); i += 2) if (name.equals(a.values.get(i))) return a.values.get(i + 1);
        return null;
    }

    private static int shiftFor(int slot, int[] insSlot, int[] insWidth) {
        int sh = 0;
        for (int k = 0; k < insSlot.length; k++) if (insSlot[k] <= slot) sh += insWidth[k];
        return sh;
    }
}
