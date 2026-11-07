package org.kylepitcock.legacyconnector.loader.transform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.kylepitcock.legacyconnector.loader.synth.CtorChooser;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Bridge methods of a forward version (1.6.4 on the 1.7.10 shim), like the ones javac generates for covariant overrides: a mod block that
 * overrides {@code breakBlock(World, x, y, z, int id, int meta)} overrides nothing in the 1.7.10 shim, which calls
 * {@code breakBlock(World, x, y, z, Block, int)}. For each such method the mod declares, the class gets the 1.7.10-shaped method, which converts
 * the arguments (a Block or Item to its numeric id through the version's facade, a differing reference type by a cast) and calls the mod's method;
 * the result is converted back. Calls of the old method on shim types, including {@code super.} calls of an override, are rewritten to the new one,
 * so the shim's own implementation and the mod's other classes stay consistent.
 *
 * <p>Table: {@code legacyconnector/forward/<id>/bridge-methods.tsv} (see the file's header). Runs after relocation, before {@link ForwardCalls}.</p>
 */
public final class BridgeMethods {
    /** One row of the table, relocated: {@code owner} is the shim class (or interface) the new method belongs to. */
    record Rule(String owner, String oldName, String oldDesc, String newName, String newDesc) {}

    private static final Map<LegacyVersion, List<String[]>> RAW = new EnumMap<>(LegacyVersion.class);

    private BridgeMethods() {}

    /** @return the number of bridge methods added plus call sites rewritten in {@code node} */
    public static int apply(ClassNode node, Hierarchy h, LegacyVersion version) {
        if (version.forward() == null) return 0;
        List<Rule> rules = rules(version, h);
        if (rules.isEmpty()) return 0;
        Map<String, List<Rule>> byOld = new java.util.HashMap<>();
        for (Rule r : rules) byOld.computeIfAbsent(r.oldName() + r.oldDesc(), k -> new ArrayList<>()).add(r);

        int n = 0;
        for (MethodNode method : List.copyOf(node.methods)) {
            if (method.instructions == null || method.instructions.size() == 0) continue;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof MethodInsnNode m) || m.owner.startsWith("[")) continue;
                List<Rule> candidates = byOld.get(m.name + m.desc);
                if (candidates == null) continue;
                for (Rule rule : candidates) {
                    if (!callApplies(m, rule, h)) continue;
                    AbstractInsnNode last = rewriteCall(method, m, rule, h);
                    if (last != null) {
                        insn = last;
                        n++;
                        break;
                    }
                }
            }
        }

        if ((node.access & Opcodes.ACC_INTERFACE) == 0) {
            for (Rule rule : rules) {
                MethodNode old = declared(node, rule.oldName(), rule.oldDesc());
                if (old == null || (old.access & Opcodes.ACC_STATIC) != 0) continue;
                if (declared(node, rule.newName(), rule.newDesc()) != null) continue;
                if (!h.isSubclassOf(node.name, rule.owner())) continue;
                MethodNode bridge = bridge(node, old, rule, h);
                if (bridge == null) continue;
                node.methods.add(bridge);
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ bridge

    private static MethodNode bridge(ClassNode node, MethodNode old, Rule rule, Hierarchy h) {
        Type[] oldArgs = Type.getArgumentTypes(rule.oldDesc());
        Type[] newArgs = Type.getArgumentTypes(rule.newDesc());
        if (newArgs.length < oldArgs.length) return null;
        InsnList il = new InsnList();
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        int slot = 1;
        int stack = 1;
        for (int i = 0; i < newArgs.length; i++) {
            if (i < oldArgs.length) {
                il.add(new VarInsnNode(newArgs[i].getOpcode(Opcodes.ILOAD), slot));
                if (!convert(il, newArgs[i], oldArgs[i], h)) return null;
                stack += Math.max(newArgs[i].getSize(), oldArgs[i].getSize()) + 1;
            }
            slot += newArgs[i].getSize();
        }
        boolean priv = (old.access & Opcodes.ACC_PRIVATE) != 0;
        il.add(new MethodInsnNode(priv ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL, node.name, rule.oldName(), rule.oldDesc(), false));
        Type oldRet = Type.getReturnType(rule.oldDesc());
        Type newRet = Type.getReturnType(rule.newDesc());
        if (newRet.getSort() == Type.VOID) {
            if (oldRet.getSize() > 0) il.add(new InsnNode(oldRet.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
        } else if (!convert(il, oldRet, newRet, h)) {
            return null;
        }
        il.add(new InsnNode(newRet.getOpcode(Opcodes.IRETURN)));
        MethodNode b = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, rule.newName(), rule.newDesc(), null, null);
        b.instructions = il;
        b.maxLocals = slot;
        b.maxStack = Math.max(stack, 2) + Math.max(newRet.getSize(), 1);
        return b;
    }

    /**
     * Converts the value on top of the stack from type {@code from} to {@code to}; false when this is not possible (no facade).
     * Block/Item to int is the id of the object, int to Block/Item the object of the id, reference types are cast when they differ.
     */
    private static boolean convert(InsnList il, Type from, Type to, Hierarchy h) {
        if (from.equals(to)) return true;
        if (from.getSort() == Type.OBJECT && to.getSort() == Type.INT) {
            MethodInsnNode id = idOf(h, from.getInternalName());
            if (id == null) return false;
            il.add(id);
            return true;
        }
        if (from.getSort() == Type.INT && to.getSort() == Type.OBJECT) {
            MethodInsnNode obj = fromId(h, to.getInternalName());
            if (obj == null) return false;
            il.add(obj);
            return true;
        }
        if ((from.getSort() == Type.OBJECT || from.getSort() == Type.ARRAY) && (to.getSort() == Type.OBJECT || to.getSort() == Type.ARRAY)) {
            if (!to.getInternalName().equals("java/lang/Object")) il.add(new TypeInsnNode(Opcodes.CHECKCAST, to.getInternalName()));
            return true;
        }
        return false;
    }

    private static MethodInsnNode idOf(Hierarchy h, String cls) {
        String blockCls = h.relocate("net/minecraft/block/Block");
        String itemCls = h.relocate("net/minecraft/item/Item");
        String name = cls.equals(blockCls) ? "mcc$get$blockID" : cls.equals(itemCls) ? "mcc$get$itemID" : null;
        if (name == null) return null;
        String facade = h.facadeOf(cls);
        String desc = "(L" + cls + ";)I";
        if (facade == null || !h.find(facade).hasMethod(name, desc)) return null;
        return new MethodInsnNode(Opcodes.INVOKESTATIC, facade, name, desc, false);
    }

    private static MethodInsnNode fromId(Hierarchy h, String cls) {
        String blockCls = h.relocate("net/minecraft/block/Block");
        String itemCls = h.relocate("net/minecraft/item/Item");
        if (!cls.equals(blockCls) && !cls.equals(itemCls)) return null;
        String facade = h.facadeOf(cls);
        String desc = "(I)L" + cls + ";";
        if (facade == null || !h.find(facade).hasMethod("mcc$fromId", desc)) return null;
        return new MethodInsnNode(Opcodes.INVOKESTATIC, facade, "mcc$fromId", desc, false);
    }

    private static MethodNode declared(ClassNode node, String name, String desc) {
        for (MethodNode m : node.methods) if (m.name.equals(name) && m.desc.equals(desc)) return m;
        return null;
    }

    // ------------------------------------------------------------------ call sites

    /** The call's receiver type is a shim type (or a mod class inheriting the shim's method) below the rule's owner, and no mod class declares the old method on the way. */
    private static boolean callApplies(MethodInsnNode m, Rule rule, Hierarchy h) {
        Set<String> seen = new HashSet<>();
        String c = m.owner;
        while (c != null && seen.add(c)) {
            ClassInfo ci = h.find(c);
            if (ci == null) break;
            if (!h.isShim(c) && ci.hasMethod(m.name, m.desc)) return false; // the mod's own method: it keeps the 1.6.4 signature
            c = ci.superName;
        }
        return h.isSubclassOf(m.owner, rule.owner());
    }

    private static AbstractInsnNode rewriteCall(MethodNode method, MethodInsnNode m, Rule rule, Hierarchy h) {
        Type[] oldArgs = Type.getArgumentTypes(m.desc);
        Type[] newArgs = Type.getArgumentTypes(rule.newDesc());
        if (newArgs.length < oldArgs.length) return null;
        Type oldRet = Type.getReturnType(m.desc);
        Type newRet = Type.getReturnType(rule.newDesc());

        boolean needsConversion = newArgs.length != oldArgs.length;
        for (int i = 0; i < oldArgs.length; i++) needsConversion |= oldArgs[i].getSort() == Type.INT && newArgs[i].getSort() == Type.OBJECT;
        InsnList pre = new InsnList();
        if (needsConversion) {
            // spill the arguments to fresh locals, then push them again converted (the receiver stays below)
            int[] slots = new int[oldArgs.length];
            int slot = method.maxLocals;
            for (int i = 0; i < oldArgs.length; i++) {
                slots[i] = slot;
                slot += oldArgs[i].getSize();
            }
            for (int i = oldArgs.length - 1; i >= 0; i--) pre.add(new VarInsnNode(oldArgs[i].getOpcode(Opcodes.ISTORE), slots[i]));
            int pushed = 0;
            for (int i = 0; i < newArgs.length; i++) {
                if (i < oldArgs.length) {
                    pre.add(new VarInsnNode(oldArgs[i].getOpcode(Opcodes.ILOAD), slots[i]));
                    if (oldArgs[i].getSort() == Type.INT && newArgs[i].getSort() == Type.OBJECT) {
                        MethodInsnNode conv = fromId(h, newArgs[i].getInternalName());
                        if (conv == null) return null;
                        pre.add(conv);
                    }
                } else {
                    pre.add(new InsnNode(CtorChooser.defaultConst(newArgs[i])));
                }
                pushed += newArgs[i].getSize();
            }
            method.maxLocals = Math.max(method.maxLocals, slot);
            method.maxStack += pushed + 2;
        }
        InsnList post = new InsnList();
        if (oldRet.getSort() != Type.VOID && !convert(post, newRet, oldRet, h)) return null;
        if (needsConversion) method.instructions.insertBefore(m, pre);
        m.name = rule.newName();
        m.desc = rule.newDesc();
        if (post.size() == 0) return m;
        AbstractInsnNode last = post.getLast();
        method.instructions.insert(m, post);
        method.maxStack += 1;
        return last;
    }

    // ------------------------------------------------------------------ table

    static List<Rule> rules(LegacyVersion version, Hierarchy h) {
        List<String[]> raw;
        synchronized (RAW) {
            raw = RAW.computeIfAbsent(version, BridgeMethods::load);
        }
        List<Rule> out = new ArrayList<>(raw.size());
        for (String[] f : raw) {
            out.add(new Rule(h.relocate(f[0]), f[1], relocateDesc(f[2], h), f[3], relocateDesc(f[4], h)));
        }
        return out;
    }

    private static List<String[]> load(LegacyVersion v) {
        List<String[]> out = new ArrayList<>();
        for (String id : v.forwardTableIds()) {
            String resource = "legacyconnector/forward/" + id + "/bridge-methods.tsv";
            try (InputStream in = BridgeMethods.class.getClassLoader().getResourceAsStream(resource)) {
                if (in == null) continue;
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] f = line.strip().split("\t");
                    if (f.length < 5) throw new IllegalStateException("Corrupt bridge rule in " + resource + ": " + line);
                    out.add(f);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Corrupt bridge table " + resource, e);
            }
        }
        return List.copyOf(out);
    }

    private static String relocateDesc(String desc, Hierarchy h) {
        StringBuilder out = new StringBuilder(desc.length() + 24);
        for (int i = 0; i < desc.length(); i++) {
            char ch = desc.charAt(i);
            out.append(ch);
            if (ch == 'L') {
                int end = desc.indexOf(';', i);
                if (end < 0) break;
                out.append(h.relocate(desc.substring(i + 1, end))).append(';');
                i = end;
            }
        }
        return out.toString();
    }
}
