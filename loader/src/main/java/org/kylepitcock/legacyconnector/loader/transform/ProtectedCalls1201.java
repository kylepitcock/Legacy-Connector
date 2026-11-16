package org.kylepitcock.legacyconnector.loader.transform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * 1.21.1 made a large part of {@code BlockBehaviour}, {@code Item}, {@code Entity}, ... protected that 1.20.1 had public ({@code state.getBlock().onRemove(...)},
 * {@code neighborChanged}, {@code onProjectileHit}, ...). A mod class that calls such a method on an object that is not its own class (or a subclass of it) fails
 * verification ("Bad access to protected data"). The call goes through {@code ProtectedAccess1201.invoke}, which reaches the method by reflection (the
 * receiver and the dispatch are the same). The table of protected methods ({@code mc-protected.tsv.gz}) is made by {@code tools/dev/gen_protected_members_1201.py}.
 */
final class ProtectedCalls1201 {
    private ProtectedCalls1201() {}

    private static final String HELPER = "org/kylepitcock/legacyconnector/v1_20_1/migrate/ProtectedAccess1201";
    private static volatile Set<String> protectedMethods;

    private static Set<String> table() {
        Set<String> t = protectedMethods;
        if (t != null) return t;
        synchronized (ProtectedCalls1201.class) {
            if (protectedMethods != null) return protectedMethods;
            Set<String> set = new HashSet<>(8192);
            try (InputStream raw = ProtectedCalls1201.class.getClassLoader().getResourceAsStream("legacyconnector/forward/1.20.1/mc-protected.tsv.gz")) {
                if (raw != null) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(new GZIPInputStream(raw), StandardCharsets.UTF_8));
                    for (String l = r.readLine(); l != null; l = r.readLine()) {
                        if (!l.isEmpty() && l.charAt(0) != '#') set.add(l);
                    }
                }
            } catch (IOException e) {
                // no table: nothing is rewritten
            }
            protectedMethods = set;
            return set;
        }
    }

    /** @return the number of calls rewritten in {@code node} */
    static int apply(ClassNode node, Mc1201Hierarchy h) {
        int count = 0;
        for (MethodNode m : node.methods) {
            if (m.instructions == null || m.instructions.size() == 0) continue;
            for (AbstractInsnNode in : m.instructions.toArray()) {
                if (!(in instanceof MethodInsnNode call) || call.getOpcode() != Opcodes.INVOKEVIRTUAL || call.owner.startsWith("[")) continue;
                String declaring = protectedDeclarer(call, h);
                if (declaring == null) continue;
                // legal protected access: the caller extends the declaring class and the receiver is the caller or one of its subclasses
                if (h.isSubclassOf(node.name, declaring) && (call.owner.equals(node.name) || h.isSubclassOf(call.owner, node.name))) continue;
                rewrite(m, call, declaring);
                count++;
            }
        }
        return count;
    }

    /** The Minecraft class that declares the (protected) method the call resolves to, or null when it is not protected or a mod class declares it. */
    private static String protectedDeclarer(MethodInsnNode call, Mc1201Hierarchy h) {
        if (call.name.equals("<init>") || call.name.equals("<clinit>")) return null;
        if (h.declaredIn(call.owner, call.name, call.desc) == Mc1201Hierarchy.Origin.MOD) return null;
        String sig = call.name + call.desc;
        for (String a : h.minecraftAncestors(call.owner)) {
            McMembers1201.Members members = McMembers1201.get(a);
            if (members == null || !members.methods().contains(sig)) continue;
            return table().contains(a + "\t" + sig) ? a : null;
        }
        return null;
    }

    /** {@code recv, args...} becomes {@code ProtectedAccess1201.invoke(recv, Declaring.class, name, desc, new Object[] {args})} with the result converted back. */
    private static void rewrite(MethodNode m, MethodInsnNode call, String declaring) {
        Type[] args = Type.getArgumentTypes(call.desc);
        Type ret = Type.getReturnType(call.desc);
        int[] slot = new int[args.length];
        int next = m.maxLocals;
        for (int i = 0; i < args.length; i++) {
            slot[i] = next;
            next += args[i].getSize();
        }
        m.maxLocals = next;
        InsnList il = new InsnList();
        for (int i = args.length - 1; i >= 0; i--) il.add(new VarInsnNode(args[i].getOpcode(Opcodes.ISTORE), slot[i]));
        il.add(new LdcInsnNode(Type.getObjectType(declaring)));
        il.add(new LdcInsnNode(call.name));
        il.add(new LdcInsnNode(call.desc));
        il.add(new LdcInsnNode(args.length));
        il.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
        for (int i = 0; i < args.length; i++) {
            il.add(new InsnNode(Opcodes.DUP));
            il.add(new LdcInsnNode(i));
            il.add(new VarInsnNode(args[i].getOpcode(Opcodes.ILOAD), slot[i]));
            box(il, args[i]);
            il.add(new InsnNode(Opcodes.AASTORE));
        }
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, "invoke",
                "(Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;", false));
        unbox(il, ret);
        m.instructions.insertBefore(call, il);
        m.instructions.remove(call);
        m.maxStack = Math.max(m.maxStack, 6 + 3);
    }

    private static void box(InsnList il, Type t) {
        String[] w = wrapper(t);
        if (w != null) il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, w[0], "valueOf", "(" + t.getDescriptor() + ")L" + w[0] + ";", false));
    }

    private static void unbox(InsnList il, Type t) {
        if (t.getSort() == Type.VOID) {
            il.add(new InsnNode(Opcodes.POP));
            return;
        }
        String[] w = wrapper(t);
        if (w != null) {
            il.add(new TypeInsnNode(Opcodes.CHECKCAST, w[0]));
            il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, w[0], w[1], "()" + t.getDescriptor(), false));
        } else if (!t.getInternalName().equals("java/lang/Object")) {
            il.add(new TypeInsnNode(Opcodes.CHECKCAST, t.getSort() == Type.ARRAY ? t.getDescriptor() : t.getInternalName()));
        }
    }

    /** wrapper class and unboxing method of a primitive, null for references */
    private static String[] wrapper(Type t) {
        return switch (t.getSort()) {
            case Type.BOOLEAN -> new String[] {"java/lang/Boolean", "booleanValue"};
            case Type.BYTE -> new String[] {"java/lang/Byte", "byteValue"};
            case Type.CHAR -> new String[] {"java/lang/Character", "charValue"};
            case Type.SHORT -> new String[] {"java/lang/Short", "shortValue"};
            case Type.INT -> new String[] {"java/lang/Integer", "intValue"};
            case Type.LONG -> new String[] {"java/lang/Long", "longValue"};
            case Type.FLOAT -> new String[] {"java/lang/Float", "floatValue"};
            case Type.DOUBLE -> new String[] {"java/lang/Double", "doubleValue"};
            default -> null;
        };
    }
}
