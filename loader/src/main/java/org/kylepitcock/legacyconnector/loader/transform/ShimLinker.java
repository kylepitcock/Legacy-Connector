package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.kylepitcock.legacyconnector.loader.shim.ShimIndex;
import org.kylepitcock.legacyconnector.loader.synth.CtorChooser;
import org.kylepitcock.legacyconnector.loader.synth.SynthDemand;
import org.kylepitcock.legacyconnector.loader.synth.SynthDemand.Kind;
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
 * Post-relocation pass that makes every legacy API reference link:
 * <ol>
 *     <li>field accesses on shim classes become accessor calls where the shim models the field "virtually";</li>
 *     <li>members missing from a <em>synthesized</em> class are recorded as demand, so the generated class
 *     declares them;</li>
 *     <li>members missing from a <em>hand-written</em> shim class are redirected to generated static helpers
 *     (dynamic dispatch to a mod override where one exists, otherwise a logged default);</li>
 *     <li>constructors missing from a hand-written shim class are adapted in place to the closest real one.</li>
 * </ol>
 *
 * <p>Virtual fields exist because legacy mods read Minecraft state straight from fields
 * ({@code entity.posX}, {@code world.isRemote}) while the shim objects are views over modern objects
 * whose state lives elsewhere. A shim exposes such a field by declaring
 * {@code static T mcc$get$name(Owner)} and optionally {@code static void mcc$set$name(Owner, T)}.</p>
 */
public final class ShimLinker {
    /** Helper classes for hand-written shim class {@code X} are named {@code <synth prefix><legacy X>$$Missing}. */
    public static final String HELPER_SUFFIX = "$$Missing";

    private ShimLinker() {}

    public static int apply(ClassNode node, Hierarchy hierarchy, LinkReport report) {
        return apply(node, hierarchy, report, null);
    }

    public static int apply(ClassNode node, Hierarchy hierarchy, LinkReport report, SynthDemand demand) {
        int redirected = 0;
        for (MethodNode method : node.methods) {
            InsnList insns = method.instructions;
            for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof FieldInsnNode f && f.getOpcode() == Opcodes.PUTSTATIC && isVanillaSingletonHolder(f.owner)
                        && f.desc.startsWith("L")) {
                    // dup; getstatic <the same field>; invokestatic adopt(Object, Object); putstatic
                    InsnList hook = new InsnList();
                    hook.add(new InsnNode(Opcodes.DUP));
                    hook.add(new FieldInsnNode(Opcodes.GETSTATIC, f.owner, f.name, f.desc));
                    hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "org/kylepitcock/legacyconnector/v1_7_10/bridge/VanillaReplacement", "adopt",
                            "(Ljava/lang/Object;Ljava/lang/Object;)V", false));
                    insns.insertBefore(f, hook);
                    method.maxStack += 3;
                } else if (insn instanceof FieldInsnNode f) {
                    AbstractInsnNode replacement = virtualField(f, hierarchy);
                    if (replacement != null) {
                        insns.set(f, replacement);
                        insn = replacement;
                        redirected++;
                        check(hierarchy, report, node.name, (MethodInsnNode) replacement);
                    } else if (isLegacyApi(f.owner, hierarchy) || isModClass(f.owner, hierarchy)) {
                        // Local owners matter too: `this.blockHardness` in a mod block resolves into the shim.
                        Hierarchy.Lookup l = hierarchy.resolveField(f.owner, f.name, f.desc);
                        AbstractInsnNode fixed = demand == null ? null : missingField(f, l, hierarchy, demand, report, node.name);
                        if (fixed == null) {
                            record(report, l, node.name, f.owner, f.name + ":" + f.desc, true);
                        } else if (fixed != f) {
                            insns.set(f, fixed);
                            insn = fixed;
                        }
                    } else {
                        checkLibrary(hierarchy, report, node.name, f.owner, f.name + ":" + f.desc, hierarchy.resolveField(f.owner, f.name, f.desc));
                    }
                } else if (insn instanceof MethodInsnNode m) {
                    if (demand != null && !m.owner.startsWith("[")) {
                        AbstractInsnNode fixed = missingMethod(method, m, hierarchy, demand, report, node.name);
                        if (fixed != null) {
                            insn = fixed;
                            continue;
                        }
                    }
                    AbstractInsnNode cast = relinkReturnType(m, hierarchy, report, node.name);
                    check(hierarchy, report, node.name, m);
                    if (cast != null) {
                        insns.insert(m, cast);
                        insn = cast;
                    }
                } else if (insn instanceof TypeInsnNode t) {
                    String type = t.desc.startsWith("[") ? Type.getType(t.desc).getElementType().getInternalName() : t.desc;
                    if (isShimNamespace(type) && !hierarchy.isShim(type)) report.missingClass(type, node.name);
                }
            }
        }
        // Supertypes are the most important links: a missing shim superclass fails at class load.
        if (node.superName != null && isShimNamespace(node.superName) && !hierarchy.isShim(node.superName)) {
            report.missingClass(node.superName, node.name);
        }
        for (String iface : node.interfaces) {
            if (isShimNamespace(iface) && !hierarchy.isShim(iface)) report.missingClass(iface, node.name);
            if (demand != null && hierarchy.isSynth(iface)) demand.interfaces.add(iface);
        }
        return redirected;
    }

    // ------------------------------------------------------------------ synthesis

    /** Classes of this jar or of a peer legacy jar. */
    private static boolean isModClass(String name, Hierarchy h) {
        return h.isLocal(name) || h.isPeer(name);
    }

    private static boolean isLegacyApi(String owner, Hierarchy h) {
        return isShimNamespace(owner) || h.isSynth(owner);
    }

    /**
     * The class a missing member gets attached to: the first legacy API class (hand-written or synthesized) on
     * the owner's superclass chain, or for interface calls on its superinterfaces. Null if the chain leaves the
     * known world (an optional dependency) or never reaches the legacy API.
     */
    static String anchor(String owner, Hierarchy h, boolean itf) {
        // Superclass chain first: a class member wins over an interface default.
        List<String> interfaces = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String c = owner;
        while (c != null && seen.add(c)) {
            if (h.isShim(c) || h.isSynth(c)) {
                if (!itf || (h.find(c).access & Opcodes.ACC_INTERFACE) != 0 || interfaces.isEmpty()) return c;
                break;
            }
            ClassInfo info = h.find(c);
            if (info == null) return null;
            interfaces.addAll(info.interfaces);
            if ((info.access & Opcodes.ACC_INTERFACE) != 0) break;
            c = info.superName;
        }
        // Then superinterfaces (an abstract mod class implementing a legacy interface it does not implement itself).
        while (!interfaces.isEmpty()) {
            String i = interfaces.remove(0);
            if (!seen.add(i)) continue;
            if (h.isShim(i) || h.isSynth(i)) return i;
            ClassInfo info = h.find(i);
            if (info != null) interfaces.addAll(info.interfaces);
        }
        return null;
    }

    public static String helperClass(String shimOwner, Hierarchy h) {
        String legacy = shimOwner.substring(shimOwner.indexOf('/', "legacyconnector/".length()) + 1);
        return h.synthPrefix() + legacy + HELPER_SUFFIX;
    }

    private static AbstractInsnNode missingField(FieldInsnNode f, Hierarchy.Lookup l, Hierarchy h, SynthDemand demand, LinkReport report, String from) {
        if (l != Hierarchy.Lookup.MISSING_MEMBER && l != Hierarchy.Lookup.MISSING_CLASS) return null;
        boolean isStatic = f.getOpcode() == Opcodes.GETSTATIC || f.getOpcode() == Opcodes.PUTSTATIC;
        String anchor = anchor(f.owner, h, false);
        if (anchor == null) return null;
        if (h.isSynth(anchor)) {
            demand.add(anchor, isStatic ? Kind.STATIC_FIELD : Kind.FIELD, f.name, f.desc);
            report.stubbed(anchor, f.name + ":" + f.desc, from);
            return f; // the JVM resolves it through the owner's superclass chain
        }
        // Hand-written shim lacks the field: helper accessors backed by side storage.
        demand.add(anchor, isStatic ? Kind.HELPER_STATIC_FIELD : Kind.HELPER_FIELD, f.name, f.desc);
        report.stubbed(anchor, f.name + ":" + f.desc, from);
        String helper = helperClass(anchor, h);
        boolean get = f.getOpcode() == Opcodes.GETFIELD || f.getOpcode() == Opcodes.GETSTATIC;
        String self = isStatic ? "" : "L" + anchor + ";";
        String prefix = isStatic ? (get ? "sget$" : "sput$") : (get ? "get$" : "set$");
        String desc = get ? "(" + self + ")" + f.desc : "(" + self + f.desc + ")V";
        return new MethodInsnNode(Opcodes.INVOKESTATIC, helper, prefix + f.name, desc, false);
    }

    /** Returns the instruction that now stands where {@code m} was (possibly {@code m} itself), or null if untouched. */
    private static AbstractInsnNode missingMethod(MethodNode method, MethodInsnNode m, Hierarchy h, SynthDemand demand, LinkReport report, String from) {
        if (!isLegacyApi(m.owner, h) && !isModClass(m.owner, h)) return null;
        Hierarchy.Lookup l = h.resolveMethod(m.owner, m.name, m.desc);
        if (l != Hierarchy.Lookup.MISSING_MEMBER && l != Hierarchy.Lookup.MISSING_CLASS) return null;

        if (m.name.equals("<init>")) {
            if (h.isSynth(m.owner)) {
                demand.add(m.owner, Kind.CONSTRUCTOR, m.name, m.desc);
                report.stubbed(m.owner, m.name + m.desc, from);
                return m;
            }
            if (h.isShim(m.owner)) return adaptConstructor(method, m, h, report, from);
            return null;
        }

        boolean itf = m.itf || m.getOpcode() == Opcodes.INVOKEINTERFACE;
        String anchor = anchor(m.owner, h, itf);
        if (anchor == null) return null;
        if (h.isSynth(anchor)) {
            demand.add(anchor, m.getOpcode() == Opcodes.INVOKESTATIC ? Kind.STATIC_METHOD : Kind.METHOD, m.name, m.desc);
            if (itf && h.isSynth(m.owner)) demand.interfaces.add(m.owner);
            report.stubbed(anchor, m.name + m.desc, from);
            return m;
        }
        Kind kind;
        String desc;
        String name = m.name;
        switch (m.getOpcode()) {
            case Opcodes.INVOKESTATIC -> {
                kind = Kind.HELPER_STATIC;
                desc = m.desc;
                name = "static$" + m.name;
            }
            case Opcodes.INVOKESPECIAL -> {
                kind = Kind.HELPER_SPECIAL;
                desc = "(L" + anchor + ";" + m.desc.substring(1);
                name = "super$" + m.name;
            }
            default -> {
                kind = Kind.HELPER_VIRTUAL;
                desc = "(L" + anchor + ";" + m.desc.substring(1);
            }
        }
        demand.add(anchor, kind, m.name, kind == Kind.HELPER_STATIC ? m.desc : desc);
        report.stubbed(anchor, m.name + m.desc, from);
        m.setOpcode(Opcodes.INVOKESTATIC);
        m.owner = helperClass(anchor, h);
        m.name = name;
        m.desc = desc;
        m.itf = false;
        return m;
    }

    /**
     * {@code new Shim(a, b)} / {@code super(a, b)} where the hand-written shim lacks that constructor: spill the
     * arguments to fresh locals and call the real constructor that can use the most of them (same types, in
     * order), filling the rest with defaults. Stays inside one basic block, so stack map frames remain valid.
     */
    private static AbstractInsnNode adaptConstructor(MethodNode method, MethodInsnNode m, Hierarchy h, LinkReport report, String from) {
        Type[] args = Type.getArgumentTypes(m.desc);
        CtorChooser.Choice choice = CtorChooser.choose(h.find(m.owner), args);
        if (choice == null) {
            report.missingMember(m.owner, m.name + m.desc, from, true);
            return null;
        }
        InsnList pre = new InsnList();
        int[] slots = new int[args.length];
        int slot = method.maxLocals;
        for (int i = 0; i < args.length; i++) {
            slots[i] = slot;
            slot += args[i].getSize();
        }
        for (int i = args.length - 1; i >= 0; i--) pre.add(new VarInsnNode(args[i].getOpcode(Opcodes.ISTORE), slots[i]));
        Type[] target = Type.getArgumentTypes(choice.desc());
        int pushed = 0;
        for (int j = 0; j < target.length; j++) {
            int i = choice.map()[j];
            pre.add(i >= 0 ? new VarInsnNode(target[j].getOpcode(Opcodes.ILOAD), slots[i]) : new InsnNode(CtorChooser.defaultConst(target[j])));
            pushed += target[j].getSize();
        }
        method.maxLocals = slot;
        method.maxStack += pushed;
        method.instructions.insertBefore(m, pre);
        report.stubbed(m.owner, m.name + m.desc + " -> " + choice.desc(), from);
        m.desc = choice.desc();
        return m;
    }

    // ------------------------------------------------------------------ virtual fields and reporting

    private static AbstractInsnNode virtualField(FieldInsnNode f, Hierarchy hierarchy) {
        boolean isStatic = f.getOpcode() == Opcodes.GETSTATIC || f.getOpcode() == Opcodes.PUTSTATIC;
        boolean isGet = f.getOpcode() == Opcodes.GETSTATIC || f.getOpcode() == Opcodes.GETFIELD;
        if (hierarchy.isLocal(f.owner) && hierarchy.find(f.owner).hasField(f.name, f.desc)) return null;
        String declaring = hierarchy.findVirtualFieldOwner(f.owner, f.name, f.desc, isStatic);
        if (declaring == null) return null;
        String self = isStatic ? "" : "L" + declaring + ";";
        // Read-only virtual field: the write links to the phantom field the shim declares (and is lost, as the
        // modern state it views cannot be assigned this way).
        if (!isGet && !hierarchy.find(declaring).hasMethod(ShimIndex.SETTER_PREFIX + f.name, "(" + self + f.desc + ")V")) return null;
        if (isGet) {
            return new MethodInsnNode(Opcodes.INVOKESTATIC, declaring, ShimIndex.GETTER_PREFIX + f.name, "(" + self + ")" + f.desc, false);
        }
        return new MethodInsnNode(Opcodes.INVOKESTATIC, declaring, ShimIndex.SETTER_PREFIX + f.name, "(" + self + f.desc + ")V", false);
    }

    /**
     * A library method a mod calls with a return type the installed version of the library no longer (or not yet)
     * declares ({@code IMutation.getAllele0()} returning {@code IAllele} became {@code IAlleleSpecies}): call the
     * one with the same name and arguments, narrowing the result back to the type the caller expects.
     */
    private static AbstractInsnNode relinkReturnType(MethodInsnNode m, Hierarchy h, LinkReport report, String from) {
        if (m.owner.startsWith("[") || isShimNamespace(m.owner) || h.isLocal(m.owner) || h.isSynth(m.owner)) return null;
        if (h.resolveMethod(m.owner, m.name, m.desc) != Hierarchy.Lookup.MISSING_MEMBER) return null;
        String args = m.desc.substring(0, m.desc.indexOf(')') + 1);
        Type wanted = Type.getReturnType(m.desc);
        if (wanted.getSort() != Type.OBJECT) return null;
        String found = null;
        java.util.ArrayDeque<String> todo = new java.util.ArrayDeque<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        todo.add(m.owner);
        while (!todo.isEmpty()) {
            String c = todo.poll();
            if (c == null || !seen.add(c)) continue;
            var info = h.find(c);
            if (info == null) continue;
            for (String key : info.methods.keySet()) {
                if (key.startsWith(m.name + args) && !key.equals(m.name + m.desc)) {
                    String ret = key.substring(m.name.length() + args.length());
                    if (ret.startsWith("L")) {
                        if (found != null && !found.equals(ret)) return null;
                        found = ret;
                    }
                }
            }
            if (info.superName != null) todo.add(info.superName);
            todo.addAll(info.interfaces);
        }
        if (found == null) return null;
        report.stubbed(m.owner, m.name + m.desc + " -> " + found, from);
        m.desc = args + found;
        return new TypeInsnNode(Opcodes.CHECKCAST, wanted.getInternalName());
    }

    private static void check(Hierarchy hierarchy, LinkReport report, String from, MethodInsnNode m) {
        if (m.owner.startsWith("[")) return; // array clone() etc.
        if (hierarchy.isSynth(m.owner)) return; // demanded above
        if (isShimNamespace(m.owner) || hierarchy.isLocal(m.owner)) {
            record(report, hierarchy.resolveMethod(m.owner, m.name, m.desc), from, m.owner, m.name + m.desc, true);
        } else {
            checkLibrary(hierarchy, report, from, m.owner, m.name + m.desc, hierarchy.resolveMethod(m.owner, m.name, m.desc));
        }
    }

    private static void checkLibrary(Hierarchy hierarchy, LinkReport report, String from, String owner, String member, Hierarchy.Lookup l) {
        if (l == null) return;
        if (l == Hierarchy.Lookup.MISSING_CLASS) {
            report.externalClass(owner, from);
        } else if (l == Hierarchy.Lookup.MISSING_MEMBER) {
            report.missingMember(owner, member, from, false);
        }
    }

    private static void record(LinkReport report, Hierarchy.Lookup l, String from, String owner, String member, boolean shim) {
        switch (l) {
            case MISSING_CLASS -> report.missingClass(owner, from);
            case MISSING_MEMBER -> report.missingMember(owner, member, from, shim);
            default -> { }
        }
    }

    private static boolean isVanillaSingletonHolder(String owner) {
        return isShimNamespace(owner) && (owner.endsWith("/net/minecraft/init/Blocks") || owner.endsWith("/net/minecraft/init/Items"));
    }

    static boolean isShimNamespace(String internalName) {
        return internalName.startsWith("legacyconnector/");
    }
}
