package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Soft link for Forge 1.20.1 mods. A reference to Minecraft 1.21.1 or NeoForge that does not exist (a class, method or field removed or changed in
 * signature) used to count against the whole mod: the first execution threw {@code NoSuchMethodError}, and a class that merely mentions a missing
 * class in a way the verifier checks failed to load, taking the registration of the mod's content down with it. After the migration has done its
 * renames and rewrites, whatever still cannot link is replaced by a stand-in, so the mod loads and registers its content and only the feature that
 * needs the symbol is lost:
 * <ul>
 *   <li>a call of a missing method throws an {@link UnsupportedOperationException} naming the symbol, when (and only when) it is executed;</li>
 *   <li>a read of a missing field yields the default of its type (null, 0, false), a write is dropped;</li>
 *   <li>an instruction that needs a class 1.21.1 does not have ({@code new}, cast, array, class literal, call on it) throws the same way,
 *       {@code instanceof} is false, a {@code catch} of it is dropped, and as a supertype it becomes {@code Object} (an interface is removed).</li>
 * </ul>
 * Only symbols the layer can prove absent are touched: the owner must be a class of the mod or of the 1.21.1 index ({@link McMembers1201}) and every
 * class on the way up must be known; a chain that leaves into another mod or a library is left as it was. A symbol that resolves is never changed.
 * Each stand-in is reported, one line per symbol ({@link Stats#lines}); a mod with any is "loads" at best.
 */
final class SoftLink1201 implements Opcodes {
    static final String RUNTIME = Mc1201Tables.HELPER_PACKAGE + "SoftLink1201Runtime";
    private static final String[] HOST_PREFIXES = {"net/minecraft/", "net/neoforged/neoforge/", "com/mojang/blaze3d/", "com/mojang/math/"};
    private static final Set<String> OBJECT_METHODS = Set.of("equals(Ljava/lang/Object;)Z", "hashCode()I", "toString()Ljava/lang/String;",
            "getClass()Ljava/lang/Class;", "notify()V", "notifyAll()V", "wait()V", "wait(J)V", "wait(JI)V", "clone()Ljava/lang/Object;", "finalize()V");
    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";

    /** What was replaced, for the report. */
    static final class Stats {
        final Set<String> missingClasses = new TreeSet<>();
        final Set<String> missingMethods = new TreeSet<>();
        final Set<String> missingFields = new TreeSet<>();
        int sites;

        int symbols() {
            return missingClasses.size() + missingMethods.size() + missingFields.size();
        }

        /** One report line per stubbed symbol. */
        List<String> lines() {
            List<String> out = new ArrayList<>();
            for (String c : missingClasses) out.add("Soft link: class " + c.replace('/', '.') + " does not exist in 1.21.1; stand-in throws when used");
            for (String m : missingMethods) out.add("Soft link: method " + m.replace('/', '.') + " does not exist in 1.21.1; stand-in returns the receiver (fluent methods), does nothing (void or primitive results during construction or registration) or throws when called");
            for (String f : missingFields) out.add("Soft link: field " + f.replace('/', '.') + " does not exist in 1.21.1; reads give the default value");
            return out;
        }

        /** {@code class owner}, {@code method owner.name(desc)}, {@code field owner.name:desc} per symbol (the offline gap report keeps them as unresolved). */
        List<String> symbolList() {
            List<String> out = new ArrayList<>();
            for (String c : missingClasses) out.add("class " + c);
            for (String m : missingMethods) out.add("method " + m);
            for (String f : missingFields) out.add("field " + f);
            return out;
        }

        String summary() {
            return missingClasses.size() + " class(es), " + missingMethods.size() + " method(s), " + missingFields.size() + " field(s) in " + sites + " place(s)";
        }
    }

    private enum Res { FOUND, MISSING, UNKNOWN }

    private final Mc1201Hierarchy hierarchy;
    private final Map<String, ClassNode> mod = new HashMap<>();
    final Stats stats = new Stats();

    SoftLink1201(Iterable<ClassNode> nodes, Mc1201Hierarchy hierarchy) {
        this.hierarchy = hierarchy;
        for (ClassNode n : nodes) mod.put(n.name, n);
    }

    // ---- resolution ------------------------------------------------------------------------------------------------------------

    private static boolean hostName(String name) {
        for (String p : HOST_PREFIXES) if (name.startsWith(p)) return true;
        return false;
    }

    /** A class of the 1.21.1 namespaces that exists neither in the mod nor in Minecraft/NeoForge. */
    private boolean missingClass(String name) {
        if (name == null || name.startsWith("[")) return false;
        return hostName(name) && !mod.containsKey(name) && !McMembers1201.hasClass(name);
    }

    private boolean missingType(Type t) {
        if (t.getSort() == Type.ARRAY) t = t.getElementType();
        return t.getSort() == Type.OBJECT && missingClass(t.getInternalName());
    }

    private String missingIn(String desc) {
        for (Type t : Type.getArgumentTypes(desc)) if (missingType(t)) return element(t);
        Type r = Type.getReturnType(desc);
        return missingType(r) ? element(r) : null;
    }

    private static String element(Type t) {
        return (t.getSort() == Type.ARRAY ? t.getElementType() : t).getInternalName();
    }

    private Res resolve(String owner, String key, boolean method, Set<String> seen) {
        if (owner == null) return Res.UNKNOWN;
        if (owner.equals("java/lang/Object")) return method && OBJECT_METHODS.contains(key) ? Res.FOUND : Res.MISSING;
        if (!seen.add(owner)) return Res.MISSING;
        ClassNode n = mod.get(owner);
        if (n != null) {
            if (method) {
                for (MethodNode m : n.methods) if ((m.name + m.desc).equals(key)) return Res.FOUND;
            } else {
                for (FieldNode f : n.fields) if ((f.name + ":" + f.desc).equals(key)) return Res.FOUND;
            }
        } else {
            McMembers1201.Members mm = McMembers1201.get(owner);
            if (mm == null) return Res.UNKNOWN;
            if ((method ? mm.methods() : mm.fields()).contains(key)) return Res.FOUND;
        }
        Res result = Res.MISSING;
        for (String s : hierarchy.supers(owner)) {
            Res r = resolve(s, key, method, seen);
            if (r == Res.FOUND) return Res.FOUND;
            if (r == Res.UNKNOWN) result = Res.UNKNOWN;
        }
        return result;
    }

    /** The class does not have the method, whatever it inherits. */
    private boolean methodMissing(String owner, String name, String desc) {
        if (owner.startsWith("[")) return false;
        if (mod.containsKey(owner) && name.equals("<init>")) return false;
        if (name.equals("<init>") || name.equals("<clinit>")) {
            McMembers1201.Members mm = McMembers1201.get(owner);
            return mm != null && !mm.methods().contains(name + desc) && !mod.containsKey(owner);
        }
        return resolve(owner, name + desc, true, new HashSet<>()) == Res.MISSING;
    }

    private boolean fieldMissing(String owner, String name, String desc) {
        if (owner.startsWith("[")) return false;
        return resolve(owner, name + ":" + desc, false, new HashSet<>()) == Res.MISSING;
    }

    // ---- rewriting -------------------------------------------------------------------------------------------------------------

    /** The class cannot be stood in for: it extends a class 1.21.1 lacks (a mod class is not given another superclass: code assigns it to the original). */
    final Set<String> unfixable = new TreeSet<>();
    private ClassNode current;
    private boolean terminating;

    /**
     * Replaces what cannot link in {@code node}. Returns 0 when nothing changed, 1 when the class changed but keeps its stack map frames (a stand-in
     * leaves the stack as the original instruction did: arguments popped, the default of the result type pushed), 2 when the frames must be computed
     * again (a stand-in that throws: a constructor call on {@code this} cannot be skipped).
     */
    int apply(ClassNode node) {
        if (node.invisibleAnnotations != null && node.invisibleAnnotations.stream().anyMatch(a -> MIXIN.equals(a.desc))) return 0;
        if (node.visibleAnnotations != null && node.visibleAnnotations.stream().anyMatch(a -> MIXIN.equals(a.desc))) return 0;
        current = node;
        terminating = false;
        int changes = 0;
        if (missingClass(node.superName)) unfixable.add(node.superName + " (superclass of " + node.name + ")");
        for (Iterator<String> it = node.interfaces.iterator(); it.hasNext(); ) {
            String i = it.next();
            if (missingClass(i)) {
                stats.missingClasses.add(i);
                stats.sites++;
                it.remove(); // the verifier does not check interface assignability, so the class loads without it
                changes++;
            }
        }
        // An event handler whose event class 1.21.1 lacks (Forge's PotionColorCalculationEvent): the event is never posted, and the
        // method's descriptor alone makes getDeclaredMethods (the automatic subscriber, the event bus) throw NoClassDefFoundError for the whole class.
        for (Iterator<MethodNode> it = node.methods.iterator(); it.hasNext(); ) {
            MethodNode m = it.next();
            if (m.visibleAnnotations == null || m.visibleAnnotations.stream().noneMatch(a -> "Lnet/neoforged/bus/api/SubscribeEvent;".equals(a.desc))) continue;
            String bad = null;
            for (Type t : Type.getArgumentTypes(m.desc)) if (missingType(t)) bad = element(t);
            if (bad == null) continue;
            stats.missingClasses.add(bad);
            stats.sites++;
            it.remove();
            changes++;
        }
        for (MethodNode m : node.methods) {
            if (m.instructions == null || m.instructions.size() == 0) continue;
            int c = method(m);
            if (c > 0) m.maxStack += 3;
            changes += c;
        }
        return changes == 0 ? 0 : terminating ? 2 : 1;
    }

    private int method(MethodNode m) {
        int changes = 0;
        for (Iterator<TryCatchBlockNode> it = m.tryCatchBlocks.iterator(); it.hasNext(); ) {
            TryCatchBlockNode t = it.next();
            if (t.type != null && missingClass(t.type)) {
                stats.missingClasses.add(t.type);
                stats.sites++;
                it.remove();
                changes++;
            }
        }
        AbstractInsnNode[] all = m.instructions.toArray();
        List<AbstractInsnNode> newsToNull = new ArrayList<>();
        for (int idx = 0; idx < all.length; idx++) {
            AbstractInsnNode in = all[idx];
            InsnList repl = null;
            if (in instanceof MethodInsnNode mi) {
                String bad = missingClass(mi.owner) ? mi.owner : missingIn(mi.desc);
                String sym = mi.owner + "." + mi.name + mi.desc;
                boolean ctor = mi.name.equals("<init>") && mi.getOpcode() == INVOKESPECIAL;
                boolean selfCtor = ctor && (mi.owner.equals(current.superName) || mi.owner.equals(current.name));
                boolean gone = false;
                if (bad != null) {
                    stats.missingClasses.add(bad);
                    gone = true;
                } else if (hostOrMod(mi.owner) && methodMissing(mi.owner, mi.name, mi.desc)) {
                    stats.missingMethods.add(sym);
                    gone = true;
                }
                if (gone) {
                    // a constructor of a class that exists: the new object stays uninitialized unless its NEW goes too (a null takes its place)
                    AbstractInsnNode pairedNew = ctor && !selfCtor && !missingClass(mi.owner) ? matchingNew(all, idx) : null;
                    if (selfCtor || (ctor && !missingClass(mi.owner) && pairedNew == null)) repl = terminate(sym);
                    else {
                        repl = call(sym, mi.desc, mi.getOpcode() != INVOKESTATIC, mi.owner, construction(m), bad == null);
                        if (pairedNew != null) newsToNull.add(pairedNew);
                    }
                }
            } else if (in instanceof FieldInsnNode fi) {
                boolean cls = missingClass(fi.owner) || missingType(Type.getType(fi.desc));
                if (cls) stats.missingClasses.add(missingClass(fi.owner) ? fi.owner : element(Type.getType(fi.desc)));
                if (cls || (hostOrMod(fi.owner) && fieldMissing(fi.owner, fi.name, fi.desc))) {
                    if (!cls) stats.missingFields.add(fi.owner + "." + fi.name + ":" + fi.desc);
                    repl = field(fi);
                }
            } else if (in instanceof TypeInsnNode ti && missingClass(Type.getObjectType(ti.desc).getSort() == Type.ARRAY
                    ? element(Type.getObjectType(ti.desc)) : ti.desc)) {
                String c = Type.getObjectType(ti.desc).getSort() == Type.ARRAY ? element(Type.getObjectType(ti.desc)) : ti.desc;
                stats.missingClasses.add(c);
                repl = new InsnList();
                switch (ti.getOpcode()) {
                    case INSTANCEOF -> {
                        repl.add(new InsnNode(POP));
                        repl.add(new InsnNode(ICONST_0));
                    }
                    case NEW -> {
                        failCall(repl, c);
                        repl.add(new InsnNode(ACONST_NULL));
                    }
                    default -> { // CHECKCAST, ANEWARRAY (one operand popped, a null left in its place)
                        failCall(repl, c);
                        repl.add(new InsnNode(POP));
                        repl.add(new InsnNode(ACONST_NULL));
                    }
                }
            } else if (in instanceof MultiANewArrayInsnNode ma && missingType(Type.getType(ma.desc))) {
                String c = element(Type.getType(ma.desc));
                stats.missingClasses.add(c);
                repl = new InsnList();
                failCall(repl, c);
                for (int i = 0; i < ma.dims; i++) repl.add(new InsnNode(POP));
                repl.add(new InsnNode(ACONST_NULL));
            } else if (in instanceof LdcInsnNode l && l.cst instanceof Type t && t.getSort() != Type.METHOD && missingType(t)) {
                stats.missingClasses.add(element(t));
                repl = new InsnList();
                failCall(repl, element(t));
                repl.add(new InsnNode(ACONST_NULL));
            } else if (in instanceof InvokeDynamicInsnNode indy) {
                String bad = badHandle(indy);
                if (bad != null) repl = call(bad, indy.desc, false);
            }
            if (repl != null) {
                m.instructions.insert(in, repl);
                m.instructions.remove(in);
                stats.sites++;
                changes++;
            }
        }
        for (AbstractInsnNode n : newsToNull) {
            if (!m.instructions.contains(n)) continue; // already replaced because its class is missing
            InsnList l = new InsnList();
            failCall(l, ((TypeInsnNode) n).desc);
            l.add(new InsnNode(ACONST_NULL));
            m.instructions.insert(n, l);
            m.instructions.remove(n);
        }
        return changes;
    }

    /** The NEW of the object a constructor call at {@code at} initializes (nested {@code new}s are skipped), or null. */
    private static AbstractInsnNode matchingNew(AbstractInsnNode[] all, int at) {
        String owner = ((MethodInsnNode) all[at]).owner;
        int depth = 0;
        for (int i = at - 1; i >= 0; i--) {
            AbstractInsnNode p = all[i];
            if (p instanceof MethodInsnNode pm && pm.getOpcode() == INVOKESPECIAL && pm.name.equals("<init>")) depth++;
            else if (p.getOpcode() == NEW) {
                if (depth == 0) return ((TypeInsnNode) p).desc.equals(owner) ? p : null;
                depth--;
            }
        }
        return null;
    }

    private boolean hostOrMod(String owner) {
        return !owner.startsWith("[") && (mod.containsKey(owner) || McMembers1201.hasClass(owner));
    }

    private String badHandle(InvokeDynamicInsnNode indy) {
        String bad = missingIn(indy.desc);
        if (bad != null) {
            stats.missingClasses.add(bad);
            return bad;
        }
        for (Object arg : indy.bsmArgs) {
            if (arg instanceof Handle h) {
                if (missingClass(h.getOwner())) {
                    stats.missingClasses.add(h.getOwner());
                    return h.getOwner();
                }
                String hb = missingIn(h.getDesc().startsWith("(") ? h.getDesc() : "()" + h.getDesc());
                if (hb != null) {
                    stats.missingClasses.add(hb);
                    return hb;
                }
                if (!hostOrMod(h.getOwner())) continue;
                boolean isField = h.getTag() <= H_PUTSTATIC;
                boolean gone = isField ? fieldMissing(h.getOwner(), h.getName(), h.getDesc())
                        : methodMissing(h.getOwner(), h.getName(), h.getDesc());
                if (gone) {
                    (isField ? stats.missingFields : stats.missingMethods).add(h.getOwner() + "." + h.getName() + (isField ? ":" : "") + h.getDesc());
                    return h.getOwner() + "." + h.getName() + h.getDesc();
                }
            }
        }
        return null;
    }

    /** {@code SoftLink1201Runtime.fail(symbol)}: logs the symbol once and throws (the verifier does not know it never returns, so the stack stays valid). */
    private static void failCall(InsnList l, String symbol) {
        l.add(new LdcInsnNode(symbol));
        l.add(new MethodInsnNode(INVOKESTATIC, RUNTIME, "fail", "(Ljava/lang/String;)V", false));
    }

    /** Stand-in for a call: throws when executed; the arguments (and receiver) are popped and the default of the result type left, as after the real call. */
    private InsnList call(String symbol, String desc, boolean receiver) {
        return call(symbol, desc, receiver, null, false, false);
    }

    /** The code of a method that runs while the mod constructs itself or registers its content. */
    private static boolean construction(MethodNode m) {
        return m.name.equals("<init>") || m.name.equals("<clinit>") || m.name.startsWith("lambda$static$") || m.name.startsWith("lambda$new$");
    }

    /** {@code target} is the receiver's class or one of its supertypes (builder-style setters answer the receiver). */
    private boolean assignable(String owner, String target, Set<String> seen) {
        if (owner.equals(target) || target.equals("java/lang/Object")) return true;
        if (!seen.add(owner)) return false;
        for (String s : hierarchy.supers(owner)) if (assignable(s, target, seen)) return true;
        return false;
    }

    /**
     * Stand-in for a call. Policy: a fluent method (non-static, returns the receiver's class or a supertype of it) logs once and RETURNS THE
     * RECEIVER; a method with a void or primitive result called while the mod constructs or registers ({@code construction}) logs once and does
     * nothing; every other call throws when executed. Arguments (and the receiver, unless it is the result) are popped and the default of the
     * result type is left, as after the real call.
     */
    private InsnList call(String symbol, String desc, boolean receiver, String owner, boolean construction, boolean classExists) {
        InsnList l = new InsnList();
        Type r = Type.getReturnType(desc);
        boolean fluent = receiver && classExists && owner != null && r.getSort() == Type.OBJECT && !owner.startsWith("[")
                && assignable(owner, r.getInternalName(), new HashSet<>());
        boolean quiet = fluent || (construction && classExists && r.getSort() != Type.OBJECT && r.getSort() != Type.ARRAY);
        if (quiet) {
            l.add(new LdcInsnNode(symbol));
            l.add(new MethodInsnNode(INVOKESTATIC, RUNTIME, "note", "(Ljava/lang/String;)V", false));
        } else failCall(l, symbol);
        Type[] args = Type.getArgumentTypes(desc);
        for (int i = args.length - 1; i >= 0; i--) l.add(new InsnNode(args[i].getSize() == 2 ? POP2 : POP));
        if (fluent) return l; // the receiver stays on the stack as the result
        if (receiver) l.add(new InsnNode(POP));
        if (r.getSort() != Type.VOID) l.add(defaultValue(r));
        return l;
    }

    /** {@code throw SoftLink1201Runtime.unresolved(symbol)}: for a constructor call on {@code this}, which cannot be skipped. */
    private InsnList terminate(String symbol) {
        terminating = true;
        InsnList l = new InsnList();
        l.add(new LdcInsnNode(symbol));
        l.add(new MethodInsnNode(INVOKESTATIC, RUNTIME, "unresolved", "(Ljava/lang/String;)Ljava/lang/UnsupportedOperationException;", false));
        l.add(new InsnNode(ATHROW));
        return l;
    }

    /** A missing field: a read leaves the default value, a write is dropped (receiver and value popped). */
    private static InsnList field(FieldInsnNode fi) {
        InsnList l = new InsnList();
        Type t = Type.getType(fi.desc);
        int size = t.getSize();
        switch (fi.getOpcode()) {
            case GETSTATIC -> l.add(defaultValue(t));
            case GETFIELD -> {
                l.add(new InsnNode(POP));
                l.add(defaultValue(t));
            }
            case PUTSTATIC -> l.add(new InsnNode(size == 2 ? POP2 : POP));
            default -> {
                l.add(new InsnNode(size == 2 ? POP2 : POP));
                l.add(new InsnNode(POP));
            }
        }
        return l;
    }

    private static AbstractInsnNode defaultValue(Type t) {
        return switch (t.getSort()) {
            case Type.LONG -> new InsnNode(LCONST_0);
            case Type.FLOAT -> new InsnNode(FCONST_0);
            case Type.DOUBLE -> new InsnNode(DCONST_0);
            case Type.OBJECT, Type.ARRAY -> new InsnNode(ACONST_NULL);
            default -> new InsnNode(ICONST_0);
        };
    }
}
