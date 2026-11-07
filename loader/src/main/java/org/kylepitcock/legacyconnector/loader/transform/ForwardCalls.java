package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.mapping.ForwardMembers;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Call-site rewrites of a forward version (1.6.4 on the 1.7.10 shim) for members whose name is not the only thing that changed:
 * the receiver or arguments differ ({@code getTagList(String)} to a helper, {@code addChatMessage(String)} to a
 * {@code ChatComponentText}, {@code getBlockMaterial(x, y, z)} to {@code getBlock(x, y, z).getMaterial()}), a field became a getter
 * ({@code player.username}) or a constructor lost its name argument ({@code new NBTTagString("n", "v")}). Name-only renames are done
 * earlier by {@link LegacyRemapper} ({@link ForwardMembers#method}); this pass runs after relocation, before {@link ShimLinker}, so a
 * call it fixes never reaches the stub generation. The rules are the R, G and C lines of {@code forward/<id>/member-manual.tsv}.
 */
public final class ForwardCalls {
    private ForwardCalls() {}

    /** @return the number of call sites rewritten */
    public static int apply(ClassNode node, Hierarchy h, LegacyVersion version) {
        ForwardMembers table = ForwardMembers.forVersion(version);
        if (version.forward() == null || table.callRules().isEmpty() && table.fieldCallRules().isEmpty() && table.constructorRules().isEmpty()) return 0;
        Normalizer norm = new Normalizer(version, h);
        int count = 0;
        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode m && !m.owner.startsWith("[")) {
                    AbstractInsnNode last = m.name.equals("<init>") ? constructor(method, m, table, norm) : call(method, m, table, norm);
                    if (last != null) {
                        insn = last;
                        count++;
                    }
                } else if (insn instanceof FieldInsnNode f && f.getOpcode() == Opcodes.GETFIELD) {
                    String[] rule = findField(f, table, norm);
                    if (rule != null) {
                        if (rule[4].equals("field")) {
                            // G owner field desc field newField newDesc [owner name desc]: read the retyped field, then convert the value
                            FieldInsnNode retyped = new FieldInsnNode(Opcodes.GETFIELD, f.owner, rule[5], norm.linkDesc(rule[6]));
                            method.instructions.set(f, retyped);
                            insn = retyped;
                            if (rule.length >= 10) {
                                MethodInsnNode convert = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, norm.link(rule[7]), rule[8], norm.linkDesc(rule[9]), false);
                                method.instructions.insert(retyped, convert);
                                insn = convert;
                            }
                        } else if (rule[4].equals("static")) {
                            // G owner field desc static helperOwner helper helperDesc: a static helper (receiver first) in the version's shim
                            MethodInsnNode helper = new MethodInsnNode(Opcodes.INVOKESTATIC, rule[5], rule[6], norm.linkDesc(rule[7]), false);
                            method.instructions.set(f, helper);
                            insn = helper;
                        } else {
                            // G owner field desc getterOwner getterName getterDesc
                            MethodInsnNode getter = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, rule[4].equals("=") ? f.owner : norm.link(rule[4]), rule[5], norm.linkDesc(rule[6]), false);
                            method.instructions.set(f, getter);
                            insn = getter;
                        }
                        count++;
                    }
                }
            }
        }
        return count;
    }

    // ------------------------------------------------------------------ calls

    private static AbstractInsnNode call(MethodNode method, MethodInsnNode m, ForwardMembers table, Normalizer norm) {
        String[] rule = findCall(m, table, norm);
        if (rule == null) return null;
        // R owner name desc op args...
        InsnList list = method.instructions;
        String op = rule[4];
        boolean itf = m.getOpcode() == Opcodes.INVOKEINTERFACE;
        switch (op) {
            case "call" -> {
                m.owner = rule[5].equals("=") ? m.owner : norm.link(rule[5]);
                m.name = rule[6];
                m.desc = norm.linkDesc(rule[7]);
                return m;
            }
            case "call-pop" -> {
                // the 1.7.10 method returns what the 1.6.4 one did not (a fluent setter); the caller does not expect a value
                String newDesc = norm.linkDesc(rule[6]);
                m.name = rule[5];
                m.desc = newDesc;
                if (Type.getReturnType(newDesc).getSort() == Type.VOID) return m;
                InsnNode pop = new InsnNode(Type.getReturnType(newDesc).getSize() == 2 ? Opcodes.POP2 : Opcodes.POP);
                list.insert(m, pop);
                return pop;
            }
            case "droplast" -> {
                // R ... droplast newName newDesc: the trailing arguments the new descriptor lacks are discarded
                String newDesc = norm.linkDesc(rule[6]);
                Type[] old = Type.getArgumentTypes(m.desc);
                int keep = Type.getArgumentTypes(newDesc).length;
                InsnList drop = new InsnList();
                for (int i = old.length - 1; i >= keep; i--) drop.add(new InsnNode(old[i].getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
                list.insertBefore(m, drop);
                m.name = rule[5];
                m.desc = newDesc;
                return m;
            }
            case "append-int" -> {
                // R ... append-int newName newDesc value: the new method takes one more int/boolean argument
                int value = Integer.parseInt(rule[7]);
                list.insertBefore(m, value >= -1 && value <= 5 ? new InsnNode(Opcodes.ICONST_0 + value) : new org.objectweb.asm.tree.LdcInsnNode(value));
                method.maxStack += 1;
                m.name = rule[5];
                m.desc = norm.linkDesc(rule[6]);
                return m;
            }
            case "static" -> {
                MethodInsnNode helper = new MethodInsnNode(Opcodes.INVOKESTATIC, rule[5], rule[6], norm.linkDesc(rule[7]), false);
                list.set(m, helper);
                return helper;
            }
            case "wrap-text" -> {
                // [..., string] -> [..., new ChatComponentText(string)]
                String text = norm.link("net/minecraft/util/ChatComponentText");
                InsnList wrap = new InsnList();
                wrap.add(new TypeInsnNode(Opcodes.NEW, text));
                wrap.add(new InsnNode(Opcodes.DUP_X1));
                wrap.add(new InsnNode(Opcodes.SWAP));
                wrap.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, text, "<init>", "(Ljava/lang/String;)V", false));
                list.insertBefore(m, wrap);
                method.maxStack += 3;
                m.name = rule[5];
                m.desc = norm.linkDesc(rule[6]);
                return m;
            }
            case "chain" -> {
                // R owner name desc chain o1 n1 d1 o2 n2 d2: the first call's result is the receiver of the second
                MethodInsnNode first = new MethodInsnNode(itf ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, rule[5].equals("=") ? m.owner : norm.link(rule[5]), rule[6], norm.linkDesc(rule[7]), itf);
                MethodInsnNode second = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, norm.link(rule[8]), rule[9], norm.linkDesc(rule[10]), false);
                list.insertBefore(m, first);
                list.set(m, second);
                return second;
            }
            default -> {
                return null;
            }
        }
    }

    private static AbstractInsnNode constructor(MethodNode method, MethodInsnNode m, ForwardMembers table, Normalizer norm) {
        String[] rule = table.constructorRules().get(norm.strip(m.owner) + ".<init>" + norm.strip(m.desc));
        if (rule == null) return null;
        if (rule.length > 4 && rule[4].equals("args")) return constructorArgs(method, m, rule, norm);
        // C owner desc newDesc drop: drop the first argument (a name the tag no longer carries)
        Type[] args = Type.getArgumentTypes(m.desc);
        if (args.length == 0) return null;
        int rest = 0;
        for (int i = 1; i < args.length; i++) rest += args[i].getSize();
        InsnList drop = new InsnList();
        switch (rest) {
            case 0 -> drop.add(new InsnNode(args[0].getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
            case 1 -> {
                if (args[0].getSize() != 1) return null;
                drop.add(new InsnNode(Opcodes.SWAP));
                drop.add(new InsnNode(Opcodes.POP));
            }
            case 2 -> {
                if (args[0].getSize() != 1) return null;
                drop.add(new InsnNode(Opcodes.DUP2_X1));
                drop.add(new InsnNode(Opcodes.POP2));
                drop.add(new InsnNode(Opcodes.POP));
            }
            default -> {
                return null;
            }
        }
        method.instructions.insertBefore(m, drop);
        m.desc = norm.linkDesc(rule[3]);
        return m;
    }

    /**
     * C owner desc newDesc args spec...: the constructor takes other arguments, one spec per new argument: {@code src} (the old argument
     * with that index, as is) or {@code src:HelperOwner#helper#(desc)Ret} (a static helper applied to it first). The old arguments are spilled to
     * fresh locals and pushed again in the new order, so the conversion may read any of them (1.6.4 {@code Achievement(int id, String name, ...)}
     * becomes the 1.7.10 constructor taking {@code "achievement." + name} and the name).
     */
    private static AbstractInsnNode constructorArgs(MethodNode method, MethodInsnNode m, String[] rule, Normalizer norm) {
        String newDesc = norm.linkDesc(rule[3]);
        Type[] old = Type.getArgumentTypes(m.desc);
        Type[] nw = Type.getArgumentTypes(newDesc);
        if (rule.length - 5 != nw.length) return null;
        int[] slots = new int[old.length];
        int slot = method.maxLocals;
        for (int i = 0; i < old.length; i++) {
            slots[i] = slot;
            slot += old[i].getSize();
        }
        InsnList il = new InsnList();
        for (int i = old.length - 1; i >= 0; i--) il.add(new VarInsnNode(old[i].getOpcode(Opcodes.ISTORE), slots[i]));
        int pushed = 0;
        for (int j = 0; j < nw.length; j++) {
            String spec = rule[5 + j];
            int colon = spec.indexOf(':');
            int src = Integer.parseInt(colon < 0 ? spec : spec.substring(0, colon));
            if (src >= old.length || src < -1 || src == -1 && colon < 0) return null;
            // "-1:Owner#helper#()Ret": an argument the old constructor did not have, made by a static helper without parameters
            if (src >= 0) il.add(new VarInsnNode(old[src].getOpcode(Opcodes.ILOAD), slots[src]));
            if (colon >= 0) {
                String[] h = spec.substring(colon + 1).split("#");
                il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, h[0], h[1], norm.linkDesc(h[2]), false));
            }
            pushed += nw[j].getSize();
        }
        method.maxLocals = Math.max(method.maxLocals, slot);
        method.maxStack += pushed + 2;
        method.instructions.insertBefore(m, il);
        m.desc = newDesc;
        return m;
    }

    // ------------------------------------------------------------------ rule lookup through the receiver's supertypes

    private static String[] findCall(MethodInsnNode m, ForwardMembers table, Normalizer norm) {
        String desc = norm.strip(m.desc);
        return walk(m.owner, c -> table.callRules().get(norm.strip(c) + "." + m.name + desc), norm);
    }

    private static String[] findField(FieldInsnNode f, ForwardMembers table, Normalizer norm) {
        String desc = norm.strip(f.desc);
        return walk(f.owner, c -> {
            String[] r = table.fieldCallRules().get(norm.strip(c) + "." + f.name);
            return r != null && r[3].equals(desc) ? r : null;
        }, norm);
    }

    private static String[] walk(String owner, java.util.function.Function<String, String[]> lookup, Normalizer norm) {
        ArrayDeque<String> todo = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();
        todo.add(owner);
        while (!todo.isEmpty() && seen.size() < 48) {
            String c = todo.poll();
            if (c == null || !seen.add(c)) continue;
            String[] r = lookup.apply(c);
            if (r != null) return r;
            ClassInfo info = norm.hierarchy.find(c);
            if (info == null) continue;
            if (info.superName != null) todo.add(info.superName);
            todo.addAll(info.interfaces);
        }
        return null;
    }

    /** Relocated (prefixed) names to the legacy form the tables use, and back. */
    private static final class Normalizer {
        final Hierarchy hierarchy;
        private final String link;
        private final String synth;

        Normalizer(LegacyVersion version, Hierarchy hierarchy) {
            this.hierarchy = hierarchy;
            this.link = version.linkPrefix();
            this.synth = version.synthPrefix();
        }

        String strip(String s) {
            return s.replace(link, "").replace(synth, "");
        }

        String link(String legacy) {
            return legacy.startsWith("legacyconnector") ? legacy : hierarchy.relocate(legacy);
        }

        String linkDesc(String legacyDesc) {
            StringBuilder out = new StringBuilder(legacyDesc.length() + 24);
            for (int i = 0; i < legacyDesc.length(); i++) {
                char ch = legacyDesc.charAt(i);
                out.append(ch);
                if (ch == 'L') {
                    int end = legacyDesc.indexOf(';', i);
                    if (end < 0) break;
                    out.append(link(legacyDesc.substring(i + 1, end))).append(';');
                    i = end;
                }
            }
            return out.toString();
        }
    }

    /** For tests: the table keys a rewrite looks up (kept in one place). */
    static Map<String, String[]> rules(LegacyVersion version) {
        return ForwardMembers.forVersion(version).callRules();
    }
}
