package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Finds the hooks a legacy coremod put into one of Minecraft's classes: the class before and after its transformer are compared method by method, and every
 * static call to a class outside Minecraft that the transformer added is a hook, described by where it sits (its {@link Hook#site site}). The site names the
 * place in 1.7.10's code in SRG names and nothing of the mod, so the run time can offer the same place on the modern game to any mod that hooked it
 * ({@code CoremodHooks}); a site the run time does not offer is reported instead of silently lost.
 *
 * <p>Site forms, after {@code <class>.<method><desc>#}:
 * <ul>
 * <li>{@code head-cancel}: called first; the method returns when the hook answers true;</li>
 * <li>{@code replace:<name><desc>}: stands in for that call (the hook takes the receiver first);</li>
 * <li>{@code const:<value>}: receives or replaces that constant;</li>
 * <li>{@code tail}: called before the method's last return;</li>
 * <li>{@code before:<instruction>} / {@code after:<instruction>}: anything else, by its neighbour.</li>
 * </ul>
 * The same site repeated in a method gets {@code @2}, {@code @3}...
 */
public final class CoremodHookDiscovery {
    private CoremodHookDiscovery() {}

    /** One hook call: where it is, what is called, and the other instructions of the same edit. */
    public record Hook(String site, String owner, String name, String desc, List<String> added, List<String> removed) {
        @Override
        public String toString() {
            return site + " -> " + owner + "." + name + desc;
        }
    }

    /** One edit of a method: the instructions removed and added between two instructions the transformer kept. */
    public record Run(int index, String previous, String next, List<String> removed, List<String> added) {}

    /**
     * One structural addition a transformer made to a Minecraft class: {@code kind} is {@code interface}, {@code field} or {@code method}; {@code desc}
     * is empty for an interface; {@code access} is the member's access flags.
     */
    public record Addition(String className, String kind, String name, String desc, int access) {
        @Override
        public String toString() {
            return kind + "\t" + className + "\t" + name + "\t" + desc + "\t" + access;
        }
    }

    /** The interfaces, fields and methods {@code after} has and {@code before} lacks (a coremod that extends a game class, e.g. adds state to {@code Chunk}); SRG names. */
    public static List<Addition> additions(ClassNode before, ClassNode after) {
        List<Addition> out = new ArrayList<>();
        for (String i : after.interfaces) if (!before.interfaces.contains(i)) out.add(new Addition(before.name, "interface", i, "", 0));
        java.util.Set<String> fields = new java.util.HashSet<>();
        for (var f : before.fields) fields.add(f.name + ":" + f.desc);
        for (var f : after.fields) if (!fields.contains(f.name + ":" + f.desc)) out.add(new Addition(before.name, "field", f.name, f.desc, f.access));
        java.util.Set<String> methods = new java.util.HashSet<>();
        for (MethodNode m : before.methods) methods.add(m.name + m.desc);
        for (MethodNode m : after.methods) if (!methods.contains(m.name + m.desc)) out.add(new Addition(before.name, "method", m.name, m.desc, m.access));
        return out;
    }

    /** The hooks {@code after} has and {@code before} lacks; both in SRG names. */
    public static List<Hook> hooks(ClassNode before, ClassNode after) {
        List<Hook> out = new ArrayList<>();
        Map<String, MethodNode> methods = new HashMap<>();
        for (MethodNode m : before.methods) methods.put(m.name + m.desc, m);
        for (MethodNode m : after.methods) {
            MethodNode old = methods.get(m.name + m.desc);
            if (old == null) continue;
            Map<String, Integer> seen = new HashMap<>();
            for (Run run : runs(text(old), text(m))) {
                for (int i = 0; i < run.added.size(); i++) {
                    String insn = run.added.get(i);
                    if (!insn.startsWith("invokestatic ")) continue;
                    String target = insn.substring("invokestatic ".length());
                    int dot = target.indexOf('.');
                    int paren = target.indexOf('(');
                    String owner = target.substring(0, dot);
                    if (isPlatform(owner)) continue;
                    String site = before.name + "." + m.name + m.desc + "#" + anchor(run, i);
                    int n = seen.merge(site, 1, Integer::sum);
                    if (n > 1) site += "@" + n;
                    out.add(new Hook(site, owner, target.substring(dot + 1, paren), target.substring(paren), List.copyOf(run.added), List.copyOf(run.removed)));
                }
            }
        }
        return out;
    }

    private static boolean isPlatform(String owner) {
        return owner.startsWith("net/minecraft/") || owner.startsWith("java/") || owner.startsWith("javax/") || owner.startsWith("org/lwjgl/")
                || owner.startsWith("cpw/mods/fml/") || owner.startsWith("net/minecraftforge/") || owner.startsWith("com/google/") || owner.startsWith("org/apache/");
    }

    private static String anchor(Run run, int at) {
        List<String> added = run.added;
        boolean cancels = at + 2 < added.size() && (added.get(at + 1).equals("ifeq") || added.get(at + 1).equals("ifne")) && added.get(at + 2).endsWith("return");
        if (cancels && run.index <= 1) return "head-cancel";
        for (String gone : run.removed) {
            if (gone.startsWith("invoke")) {
                String target = gone.substring(gone.indexOf(' ') + 1);
                return "replace:" + target.substring(target.indexOf('.') + 1);
            }
        }
        for (String gone : run.removed) if (isConstant(gone)) return "const:" + constant(gone);
        // A constant pushed just before the call is the value the hook adjusts.
        String before = at > 0 ? added.get(at - 1) : run.previous;
        if (before != null && isConstant(before)) return "const:" + constant(before);
        if (cancels) return "cancel-after:" + run.previous;
        if (run.next != null && run.next.endsWith("return") && at == added.size() - 1) return "tail";
        if (run.previous == null) return "head";
        return at == added.size() - 1 && run.next != null && !run.removed.isEmpty() ? "before:" + run.next : "after:" + run.previous;
    }

    private static boolean isConstant(String insn) {
        return insn.startsWith("ldc ") || insn.startsWith("bipush ") || insn.startsWith("sipush ") || insn.matches("[ilfd]const_(m1|\\d)");
    }

    private static String constant(String insn) {
        int space = insn.indexOf(' ');
        if (space > 0) return insn.substring(space + 1);
        String v = insn.substring(insn.indexOf('_') + 1);
        return v.equals("m1") ? "-1" : v;
    }

    /** The edits that turn instruction list {@code a} into {@code b} (longest common subsequence). */
    public static List<Run> runs(List<String> a, List<String> b) {
        int n = a.size();
        int m = b.size();
        int[][] lcs = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                lcs[i][j] = a.get(i).equals(b.get(j)) ? lcs[i + 1][j + 1] + 1 : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }
        List<Run> out = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < n || j < m) {
            if (i < n && j < m && a.get(i).equals(b.get(j))) {
                i++;
                j++;
                continue;
            }
            int start = i;
            List<String> removed = new ArrayList<>();
            List<String> added = new ArrayList<>();
            while ((i < n || j < m) && !(i < n && j < m && a.get(i).equals(b.get(j)))) {
                if (j < m && (i == n || lcs[i][j + 1] >= lcs[i + 1][j])) added.add(b.get(j++));
                else removed.add(a.get(i++));
            }
            out.add(new Run(start, start == 0 ? null : a.get(start - 1), i < n ? a.get(i) : null, removed, added));
        }
        return out;
    }

    /** A method's instructions as text, one per instruction; labels, line numbers and jump targets are left out (they move with every insertion). */
    public static List<String> text(MethodNode m) {
        List<String> out = new ArrayList<>();
        for (AbstractInsnNode insn : m.instructions) {
            int op = insn.getOpcode();
            if (op < 0) continue;
            String name = OPCODES[op];
            out.add(switch (insn) {
                case VarInsnNode v -> name + " " + v.var;
                case FieldInsnNode f -> name + " " + f.owner + "." + f.name + " " + f.desc;
                case MethodInsnNode mi -> name + " " + mi.owner + "." + mi.name + mi.desc;
                case TypeInsnNode t -> name + " " + t.desc;
                case LdcInsnNode l -> name + " " + (l.cst instanceof String s ? "\"" + s + "\"" : String.valueOf(l.cst));
                case IntInsnNode in -> name + " " + in.operand;
                case IincInsnNode ii -> name + " " + ii.var + " " + ii.incr;
                case JumpInsnNode jn -> name;
                case InvokeDynamicInsnNode d -> name + " " + d.name + d.desc;
                case MultiANewArrayInsnNode ma -> name + " " + ma.desc + " " + ma.dims;
                default -> name;
            });
        }
        return out;
    }

    private static final String[] OPCODES = new String[256];

    static {
        // Opcodes declares the instruction constants last, after the access flags, frame types and handle kinds that share small values.
        try {
            boolean instructions = false;
            for (var f : Opcodes.class.getFields()) {
                if (f.getName().equals("NOP")) instructions = true;
                if (!instructions || f.getType() != int.class) continue;
                int v = f.getInt(null);
                if (v >= 0 && v < 256 && OPCODES[v] == null) OPCODES[v] = f.getName().toLowerCase(Locale.ROOT);
            }
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
