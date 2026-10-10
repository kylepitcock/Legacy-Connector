package org.kylepitcock.mccompat.loader.transform;

import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Call-site rewrites for Java 8 -> Java 21 behaviour changes that cannot be fixed by renaming a class.
 *
 * <p>Every redirect keeps the operand stack shape identical (the replacement is a static method taking the
 * receiver as first argument), so existing stack map frames stay valid and no frame recomputation - which
 * would require loading the whole class hierarchy - is necessary.</p>
 */
public final class JavaCompatRedirects {
    private static final String COMPAT = "org/kylepitcock/mccompat/compat/jdk/";

    /** Key: {@code owner.name+desc} as it appears after relocation. */
    private static final Map<String, MethodInsnNode> INVOKES = new HashMap<>();

    static {
        // Java 12+: Field.modifiers is filtered from reflection, breaking the classic "remove final" hack.
        // Java 18+: static final fields can no longer be written through reflection at all.
        virtualToStatic("java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", COMPAT + "ReflectionCompat");
        virtualToStatic("java/lang/reflect/Field", "setInt", "(Ljava/lang/Object;I)V", COMPAT + "ReflectionCompat");
        virtualToStatic("java/lang/reflect/Field", "set", "(Ljava/lang/Object;Ljava/lang/Object;)V", COMPAT + "ReflectionCompat");
        virtualToStatic("java/lang/reflect/Field", "setBoolean", "(Ljava/lang/Object;Z)V", COMPAT + "ReflectionCompat");
        virtualToStatic("java/lang/reflect/Field", "setLong", "(Ljava/lang/Object;J)V", COMPAT + "ReflectionCompat");
        virtualToStatic("java/lang/reflect/Field", "setFloat", "(Ljava/lang/Object;F)V", COMPAT + "ReflectionCompat");
        virtualToStatic("java/lang/reflect/Field", "setDouble", "(Ljava/lang/Object;D)V", COMPAT + "ReflectionCompat");
        // Java 20+: Thread.stop/suspend/resume throw UnsupportedOperationException.
        virtualToStatic("java/lang/Thread", "stop", "()V", COMPAT + "JdkCompat");
        virtualToStatic("java/lang/Thread", "suspend", "()V", COMPAT + "JdkCompat");
        virtualToStatic("java/lang/Thread", "resume", "()V", COMPAT + "JdkCompat");
        // Java 18+: installing a SecurityManager throws by default.
        staticToStatic("java/lang/System", "setSecurityManager", "(Ljava/lang/SecurityManager;)V", COMPAT + "JdkCompat", "setSecurityManager");
        // Guava 17 -> 32 removals.
        staticToStatic("com/google/common/base/Objects", "toStringHelper", "(Ljava/lang/Object;)Lcom/google/common/base/MoreObjects$ToStringHelper;", "com/google/common/base/MoreObjects", "toStringHelper");
        staticToStatic("com/google/common/base/Objects", "toStringHelper", "(Ljava/lang/Class;)Lcom/google/common/base/MoreObjects$ToStringHelper;", "com/google/common/base/MoreObjects", "toStringHelper");
        staticToStatic("com/google/common/base/Objects", "toStringHelper", "(Ljava/lang/String;)Lcom/google/common/base/MoreObjects$ToStringHelper;", "com/google/common/base/MoreObjects", "toStringHelper");
        staticToStatic("com/google/common/base/Objects", "firstNonNull", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", "com/google/common/base/MoreObjects", "firstNonNull");
        staticToStatic("com/google/common/collect/Iterators", "emptyIterator", "()Lcom/google/common/collect/UnmodifiableIterator;", COMPAT + "GuavaCompat", "emptyIterator");
        virtualToStatic("com/google/common/base/Stopwatch", "elapsedMillis", "()J", COMPAT + "GuavaCompat");
        virtualToStatic("com/google/common/base/Stopwatch", "elapsedTime", "(Ljava/util/concurrent/TimeUnit;)J", COMPAT + "GuavaCompat");
    }

    private JavaCompatRedirects() {}

    private static void virtualToStatic(String owner, String name, String desc, String target) {
        String staticDesc = "(L" + owner + ";" + desc.substring(1);
        INVOKES.put(owner + "." + name + desc, new MethodInsnNode(Opcodes.INVOKESTATIC, target, name, staticDesc, false));
    }

    private static void staticToStatic(String owner, String name, String desc, String target, String targetName) {
        INVOKES.put(owner + "." + name + desc, new MethodInsnNode(Opcodes.INVOKESTATIC, target, targetName, desc, false));
    }

    /** @return number of rewritten call sites */
    public static int apply(ClassNode node) {
        int count = 0;
        for (MethodNode method : node.methods) {
            InsnList insns = method.instructions;
            for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode call) {
                    MethodInsnNode target = INVOKES.get(call.owner + "." + call.name + call.desc);
                    // Only virtual call sites are rewritten for virtual methods; super calls stay intact.
                    if (target != null && (call.getOpcode() != Opcodes.INVOKESPECIAL)) {
                        MethodInsnNode replacement = (MethodInsnNode) target.clone(null);
                        insns.set(call, replacement);
                        insn = replacement;
                        count++;
                    } else if (call.getOpcode() == Opcodes.INVOKESPECIAL && call.owner.equals("com/google/common/base/Stopwatch")
                            && call.name.equals("<init>")) {
                        // new Stopwatch() / new Stopwatch(Ticker) -> Stopwatch.createUnstarted(...)
                        AbstractInsnNode replaced = rewriteConstructor(insns, call, "createUnstarted");
                        if (replaced != null) {
                            insn = replaced;
                            count++;
                        }
                    }
                } else if (insn instanceof TypeInsnNode type && type.getOpcode() == Opcodes.CHECKCAST
                        && type.desc.equals("java/net/URLClassLoader")) {
                    // Java 9+: the app/system class loader is no longer a URLClassLoader.
                    MethodInsnNode replacement = new MethodInsnNode(Opcodes.INVOKESTATIC, COMPAT + "JdkCompat", "asURLClassLoader",
                            "(Ljava/lang/Object;)Ljava/net/URLClassLoader;", false);
                    insns.set(type, replacement);
                    insn = replacement;
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Rewrites {@code NEW T; DUP; <args>; INVOKESPECIAL T.<init>(args)V} into {@code <args>; INVOKESTATIC T.factory(args)T}
     * when NEW/DUP directly precede the argument code, which is what javac emits.
     */
    private static AbstractInsnNode rewriteConstructor(InsnList insns, MethodInsnNode init, String factory) {
        AbstractInsnNode cursor = init.getPrevious();
        int depth = 0;
        // Walk back to the matching NEW; skip nested NEW/<init> pairs within the arguments.
        while (cursor != null) {
            if (cursor instanceof MethodInsnNode m && m.getOpcode() == Opcodes.INVOKESPECIAL && m.name.equals("<init>")) depth++;
            if (cursor instanceof TypeInsnNode t && t.getOpcode() == Opcodes.NEW) {
                if (depth == 0) break;
                depth--;
            }
            cursor = cursor.getPrevious();
        }
        if (!(cursor instanceof TypeInsnNode newInsn) || !newInsn.desc.equals(init.owner)) return null;
        AbstractInsnNode dup = newInsn.getNext();
        if (dup == null || dup.getOpcode() != Opcodes.DUP) return null;
        insns.remove(newInsn);
        insns.remove(dup);
        String desc = init.desc.substring(0, init.desc.length() - 1) + "L" + init.owner + ";";
        MethodInsnNode replacement = new MethodInsnNode(Opcodes.INVOKESTATIC, init.owner, factory, desc, false);
        insns.set(init, replacement);
        return replacement;
    }
}
