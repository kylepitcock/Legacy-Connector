package org.kylepitcock.legacyconnector.loader.synth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.kylepitcock.legacyconnector.loader.shim.ShimIndex;
import org.kylepitcock.legacyconnector.loader.synth.SynthDemand.Kind;
import org.kylepitcock.legacyconnector.loader.synth.SynthDemand.Member;
import org.kylepitcock.legacyconnector.loader.transform.Hierarchy;
import org.kylepitcock.legacyconnector.loader.transform.ShimLinker;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Generates the legacy API the hand-written shim does not provide, from the union of what the loaded legacy mods
 * reference ({@link SynthDemand}). Nothing here is mod specific: every class is derived from the legacy class
 * hierarchy plus the descriptors found in mod bytecode.
 *
 * <ul>
 *     <li>Classes keep their legacy supertypes, so a synthesized {@code BlockColored} is a shim {@code Block} and a
 *     synthesized {@code PlayerDropsEvent} is a shim {@code Event}.</li>
 *     <li>Constructors chain to the closest real superclass constructor.</li>
 *     <li>Methods return defaults: zero/false, empty mutable collections, empty arrays, otherwise null.
 *     Each first call is logged by the runtime, so the log shows which stubs a modpack actually exercises.</li>
 *     <li>Enum constants exist (named after the referenced fields), so {@code ArmorMaterial.IRON} is not null.</li>
 *     <li>Members missing from hand-written classes live in {@code X$$Missing} helpers: virtual calls dispatch to a
 *     mod override when the receiver has one, fields get per-object side storage.</li>
 * </ul>
 */
public final class SynthJarBuilder {
    static final String RT = "org/kylepitcock/legacyconnector/core/synth/Synth";
    static final String IMPL_SUFFIX = "$$Impl";

    private final LegacyVersion version;
    private final ShimIndex shim;
    private final Hierarchy hierarchy;
    private final SynthDemand demand;
    /** Synth class -> constructor descriptors to generate (after propagation up synthesized superclasses). */
    private final Map<String, Set<String>> ctors = new TreeMap<>();
    /** Every synthesized class of this build. */
    private Set<String> planned = Set.of();
    /** Synthesized interfaces that need a trivial implementation (to give static fields of that type a value). */
    private final Set<String> impls = new TreeSet<>();

    public SynthJarBuilder(LegacyVersion version, ShimIndex shim, ClassLoader libraries, SynthDemand demand) {
        this.version = version;
        this.shim = shim;
        this.hierarchy = new Hierarchy(shim, libraries, Map.of(), version);
        this.demand = demand;
    }

    /** Internal name -> class bytes. */
    public Map<String, byte[]> build() {
        Map<String, byte[]> out = new LinkedHashMap<>();
        Set<String> synthClasses = new TreeSet<>(demand.classes);
        for (String owner : demand.members.keySet()) {
            if (hierarchy.isSynth(owner)) synthClasses.add(owner);
        }
        // Nested classes too: reflection builds their names from the outer class (Outer.class.getName() + "$Inner").
        LegacyHierarchyData data = LegacyHierarchyData.forVersion(version);
        for (String c : List.copyOf(synthClasses)) {
            for (String n : data.nested(c.substring(version.synthPrefix().length()))) {
                String relocated = hierarchy.relocate(n);
                if (hierarchy.isSynth(relocated)) synthClasses.add(relocated);
            }
        }
        // Supertypes of everything, transitively.
        for (String c : List.copyOf(synthClasses)) addSupers(c, synthClasses);
        planConstructors(synthClasses);
        planned = synthClasses;
        for (String c : synthClasses) {
            out.put(c, synthClass(c));
        }
        for (String i : impls) out.put(i + IMPL_SUFFIX, implClass(i));
        for (var e : demand.members.entrySet()) {
            if (hierarchy.isShim(e.getKey())) {
                String helper = ShimLinker.helperClass(e.getKey(), hierarchy);
                out.put(helper, helperClass(helper, e.getKey(), e.getValue()));
            }
        }
        return out;
    }

    private void addSupers(String c, Set<String> into) {
        ClassInfo info = hierarchy.find(c);
        if (info == null) return;
        List<String> parents = new ArrayList<>(info.interfaces);
        if (info.superName != null) parents.add(info.superName);
        for (String p : parents) if (hierarchy.isSynth(p) && into.add(p)) addSupers(p, into);
    }

    private boolean isInterface(String c) {
        LegacyHierarchyData.Entry e = hierarchy.legacyEntry(c);
        if (e != null) return (e.access() & Opcodes.ACC_INTERFACE) != 0;
        return demand.interfaces.contains(c);
    }

    private boolean isEnum(String c) {
        LegacyHierarchyData.Entry e = hierarchy.legacyEntry(c);
        return e != null && (e.access() & Opcodes.ACC_ENUM) != 0 && "java/lang/Enum".equals(e.superName());
    }

    private String superOf(String c) {
        if (isInterface(c)) return "java/lang/Object";
        ClassInfo info = hierarchy.find(c);
        return info == null || info.superName == null ? "java/lang/Object" : info.superName;
    }

    private void planConstructors(Set<String> classes) {
        for (String c : classes) {
            if (isInterface(c)) continue;
            Set<String> set = ctors.computeIfAbsent(c, k -> new TreeSet<>());
            for (Member m : demand.members.getOrDefault(c, Set.of())) if (m.kind() == Kind.CONSTRUCTOR) set.add(m.desc());
            set.add("()V"); // reflection (newInstance) and subclasses that never call a specific one
            if (isEnum(c)) set.add("(Ljava/lang/String;I)V");
            set.addAll(runtimeConstructors(c));
            // Entities are created by the runtime as (World) when they arrive from the server or a save.
            if (extendsEntity(c)) set.add("(Llegacyconnector/v1_7_10/net/minecraft/world/World;)V");
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String c : classes) {
                String sup = superOf(c);
                if (!hierarchy.isSynth(sup) || !ctors.containsKey(sup)) continue;
                if (ctors.get(sup).addAll(ctors.get(c))) changed = true;
            }
        }
    }

    /**
     * Constructors of the nearest hand-written ancestor that take Legacy Connector-internal parameters (the vanilla wrapper
     * constructors, e.g. {@code Block(VanillaInfo)}): mirrored so the runtime can create vanilla objects as their
     * legacy runtime class (a synthesized {@code BlockSapling} for the vanilla sapling).
     */
    private boolean extendsEntity(String c) {
        Set<String> seen = new HashSet<>();
        ClassInfo info = hierarchy.find(c);
        for (String a = info == null ? null : info.superName; a != null && seen.add(a); ) {
            if (a.equals("legacyconnector/v1_7_10/net/minecraft/entity/Entity")) return true;
            ClassInfo next = hierarchy.find(a);
            a = next == null ? null : next.superName;
        }
        return false;
    }

    private Set<String> runtimeConstructors(String c) {
        Set<String> out = new TreeSet<>();
        Set<String> seen = new HashSet<>();
        for (String a = superOf(c); a != null && !a.equals("java/lang/Object") && seen.add(a); a = superOf(a)) {
            if (hierarchy.isSynth(a)) continue;
            ClassInfo info = hierarchy.find(a);
            if (info == null) break;
            for (String key : info.methods.keySet()) {
                if (key.startsWith("<init>(") && key.contains("Lorg/kylepitcock/")) out.add(key.substring("<init>".length()));
            }
            break;
        }
        return out;
    }

    // ------------------------------------------------------------------ synthesized classes

    private byte[] synthClass(String name) {
        boolean itf = isInterface(name);
        boolean enumType = !itf && isEnum(name);
        LegacyHierarchyData.Entry legacy = hierarchy.legacyEntry(name);
        ClassInfo info = hierarchy.find(name);
        String sup = superOf(name);
        int access = Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER | Opcodes.ACC_SYNTHETIC;
        if (itf) access = Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT;
        else if (legacy != null && (legacy.access() & Opcodes.ACC_ABSTRACT) != 0) access |= Opcodes.ACC_ABSTRACT;
        if (legacy != null && (legacy.access() & Opcodes.ACC_ANNOTATION) != 0 && itf) access |= Opcodes.ACC_ANNOTATION;
        if (enumType) access |= Opcodes.ACC_ENUM;
        List<String> ifaces = new ArrayList<>();
        if (info != null) for (String i : info.interfaces) if (!i.equals(name)) ifaces.add(i);

        ClassWriter cw = writer();
        cw.visit(Opcodes.V1_8, access, name, null, sup, ifaces.toArray(new String[0]));
        Set<Member> members = demand.members.getOrDefault(name, Set.of());
        // Everything vanilla declared on the class it stands in for (not only what mods call): reflection finds it.
        List<LegacyMethods.Method> vanillaMethods = LegacyMethods.of(version, legacyName(name).replace('.', '/'));
        if (!vanillaMethods.isEmpty()) {
            Set<Member> all = new java.util.TreeSet<>(members);
            for (LegacyMethods.Method vm : vanillaMethods) {
                all.add(new Member(vm.isStatic() ? Kind.STATIC_METHOD : Kind.METHOD, vm.name(), relocateDesc(vm.desc())));
            }
            members = all;
        }
        Set<String> declared = new HashSet<>();
        List<Member> enumConstants = new ArrayList<>();
        List<Member> staticInits = new ArrayList<>();
        List<Member> instanceInits = new ArrayList<>();

        for (Member m : members) {
            if (m.kind() != Kind.FIELD && m.kind() != Kind.STATIC_FIELD) continue;
            boolean isStatic = m.kind() == Kind.STATIC_FIELD;
            if (itf && !isStatic) continue;
            if (!declared.add("F" + m.name() + ":" + m.desc())) continue;
            int fa = Opcodes.ACC_PUBLIC | (isStatic ? Opcodes.ACC_STATIC : 0) | (itf ? Opcodes.ACC_FINAL : 0);
            if (enumType && isStatic && m.desc().equals("L" + name + ";")) {
                fa |= Opcodes.ACC_ENUM | Opcodes.ACC_FINAL;
                enumConstants.add(m);
            } else if (isStatic && (Defaults.isCollection(m.desc()) || m.desc().startsWith("[") || itf || instantiable(m.desc()) != null
                    || synthDefaultOwner(m.desc()) != null)) {
                staticInits.add(m);
            } else if (!isStatic && (Defaults.isCollection(m.desc()) || m.desc().startsWith("["))) {
                instanceInits.add(m);
            }
            cw.visitField(fa, m.name(), m.desc(), null, null).visitEnd();
        }
        if (enumType) {
            cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC, "$VALUES", "[L" + name + ";", null, null).visitEnd();
        }

        // Constructors.
        if (!itf) {
            Set<String> descs = new java.util.LinkedHashSet<>(ctors.getOrDefault(name, Set.of("()V")));
            // the enum constants are created by the generated static initializer through (String, int)
            if (enumType && !enumConstants.isEmpty()) descs.add("(Ljava/lang/String;I)V");
            for (String desc : descs) {
                constructor(cw, name, sup, desc, enumType, instanceInits);
            }
        }

        // Methods.
        for (Member m : members) {
            if (m.kind() != Kind.METHOD && m.kind() != Kind.STATIC_METHOD) continue;
            boolean isStatic = m.kind() == Kind.STATIC_METHOD;
            if (!declared.add("M" + m.name() + m.desc())) continue;
            if (enumType && isStatic && (m.name().equals("values") && m.desc().equals("()[L" + name + ";")
                    || m.name().equals("valueOf") && m.desc().equals("(Ljava/lang/String;)L" + name + ";"))) {
                continue; // generated below
            }
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | (isStatic ? Opcodes.ACC_STATIC : 0), m.name(), m.desc(), null, null);
            mv.visitCode();
            Type ret = Type.getReturnType(m.desc());
            if (!isStatic && !itf && implementedAbove(name, m.name() + m.desc())) {
                // The legacy class overrode a method the hand-written shim implements further up: keep that
                // behaviour (mods reach it through super.m(), e.g. EntityFlying.moveEntityWithHeading).
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                int slot = 1;
                for (Type a : Type.getArgumentTypes(m.desc())) {
                    mv.visitVarInsn(a.getOpcode(Opcodes.ILOAD), slot);
                    slot += a.getSize();
                }
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superOf(name), m.name(), m.desc(), false);
                mv.visitInsn(ret.getOpcode(Opcodes.IRETURN));
                mv.visitMaxs(0, 0);
                mv.visitEnd();
                continue;
            }
            if (isStatic) staticResolverCall(mv, legacyName(name) + "." + m.name(), m.desc());
            else if (!itf) instanceResolverCall(mv, legacyName(name) + "." + m.name(), m.desc());
            stubCall(mv, legacyName(name) + "." + m.name() + m.desc());
            if (!isStatic && returnsSelf(name, ret)) {
                // Builder-style setters (fluid.setDensity(..).setViscosity(..)) must keep the chain alive.
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitInsn(Opcodes.ARETURN);
            } else if (isStatic && !itf && (access & Opcodes.ACC_ABSTRACT) == 0 && !enumType
                    && m.desc().equals("()L" + name + ";")) {
                // Singleton accessors (VillagerRegistry.instance()): one lazily created instance.
                if (declared.add("Fmcc$instance")) {
                    cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, "mcc$instance", "L" + name + ";", null, null).visitEnd();
                }
                Label have = new Label();
                mv.visitFieldInsn(Opcodes.GETSTATIC, name, "mcc$instance", "L" + name + ";");
                mv.visitJumpInsn(Opcodes.IFNONNULL, have);
                mv.visitTypeInsn(Opcodes.NEW, name);
                mv.visitInsn(Opcodes.DUP);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, name, "<init>", "()V", false);
                mv.visitFieldInsn(Opcodes.PUTSTATIC, name, "mcc$instance", "L" + name + ";");
                mv.visitLabel(have);
                mv.visitFieldInsn(Opcodes.GETSTATIC, name, "mcc$instance", "L" + name + ";");
                mv.visitInsn(Opcodes.ARETURN);
            } else if (enumType && !isStatic && m.desc().equals("()Ljava/lang/String;")
                    && java.util.Set.of("getName", "getString", "getUnlocalizedName", "getTranslationKey", "getNameTranslationKey").contains(m.name())) {
                // A vanilla enum's name (BlockPlanks.EnumType.OAK is "oak"): the lower-case constant name.
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Enum", "name", "()Ljava/lang/String;", false);
                mv.visitFieldInsn(Opcodes.GETSTATIC, "java/util/Locale", "ROOT", "Ljava/util/Locale;");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toLowerCase", "(Ljava/util/Locale;)Ljava/lang/String;", false);
                mv.visitInsn(Opcodes.ARETURN);
            } else {
                Defaults.returnDefault(mv, ret);
            }
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        if (!itf && (access & Opcodes.ACC_ABSTRACT) == 0) implementInheritedAbstract(cw, name, declared);
        if (enumType) enumMethods(cw, name);

        // Static initializer.
        if (!enumConstants.isEmpty() || !staticInits.isEmpty() || enumType) {
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitCode();
            int ordinal = 0;
            for (Member c : enumConstants) {
                mv.visitTypeInsn(Opcodes.NEW, name);
                mv.visitInsn(Opcodes.DUP);
                mv.visitLdcInsn(c.name());
                mv.visitLdcInsn(ordinal++);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, name, "<init>", "(Ljava/lang/String;I)V", false);
                mv.visitFieldInsn(Opcodes.PUTSTATIC, name, c.name(), c.desc());
            }
            if (enumType) {
                mv.visitLdcInsn(enumConstants.size());
                mv.visitTypeInsn(Opcodes.ANEWARRAY, name);
                for (int i = 0; i < enumConstants.size(); i++) {
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitLdcInsn(i);
                    mv.visitFieldInsn(Opcodes.GETSTATIC, name, enumConstants.get(i).name(), enumConstants.get(i).desc());
                    mv.visitInsn(Opcodes.AASTORE);
                }
                mv.visitFieldInsn(Opcodes.PUTSTATIC, name, "$VALUES", "[L" + name + ";");
            }
            for (Member f : staticInits) {
                String concrete = instantiable(f.desc());
                String factory = synthDefaultOwner(f.desc());
                if (factory != null) {
                    // A hand-written shim type that knows how to stand in for vanilla constants of its type
                    // (e.g. IAttribute -> a RangedAttribute named after the vanilla attribute).
                    mv.visitLdcInsn(legacyName(name) + "." + f.name());
                    String t = f.desc().substring(1, f.desc().length() - 1);
                    ClassInfo ti = hierarchy.find(t);
                    boolean tItf = ti != null && (ti.access & Opcodes.ACC_INTERFACE) != 0;
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, t, "mcc$synthDefault", "(Ljava/lang/String;)" + f.desc(), tItf);
                } else if (concrete != null) {
                    // Registries/handlers held in static fields (BlockDispenser.dispenseBehaviorRegistry): mods call
                    // methods on them right away, so give them an object whose methods are the usual stubs.
                    mv.visitTypeInsn(Opcodes.NEW, concrete);
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, concrete, "<init>", "()V", false);
                } else if (f.desc().startsWith("[") && StaticArrays.push(mv, version, legacyName(name), f.name(), f.desc())) {
                    // vanilla's literal array (dye names, ...)
                } else {
                    Defaults.pushDefault(mv, Type.getType(f.desc()));
                }
                mv.visitFieldInsn(Opcodes.PUTSTATIC, name, f.name(), f.desc());
            }
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    private void constructor(ClassWriter cw, String name, String sup, String desc, boolean enumType, List<Member> instanceInits) {
        Type[] args = Type.getArgumentTypes(desc);
        boolean enumCtor = enumType && desc.equals("(Ljava/lang/String;I)V");
        MethodVisitor mv = cw.visitMethod(enumCtor ? Opcodes.ACC_PRIVATE : Opcodes.ACC_PUBLIC, "<init>", desc, null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        int[] slots = slots(args, 1);
        if (enumType) {
            if (enumCtor) {
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitVarInsn(Opcodes.ILOAD, 2);
            } else {
                mv.visitLdcInsn("SYNTHETIC");
                mv.visitLdcInsn(-1);
            }
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Enum", "<init>", "(Ljava/lang/String;I)V", false);
        } else if (hierarchy.isSynth(sup)) {
            for (int i = 0; i < args.length; i++) mv.visitVarInsn(args[i].getOpcode(Opcodes.ILOAD), slots[i]);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, sup, "<init>", desc, false);
        } else {
            ClassInfo supInfo = hierarchy.find(sup);
            CtorChooser.Choice choice = null;
            if (supInfo != null && supInfo.methods.containsKey("<init>" + desc)) {
                // The superclass has this exact constructor (e.g. the vanilla wrapper constructor): pass through.
                int[] identity = new int[args.length];
                for (int i = 0; i < identity.length; i++) identity[i] = i;
                choice = new CtorChooser.Choice(desc, identity);
            }
            if (choice == null && supInfo != null) choice = CtorChooser.choose(supInfo, args);
            if (choice == null) choice = new CtorChooser.Choice("()V", new int[0]);
            Type[] target = Type.getArgumentTypes(choice.desc());
            for (int j = 0; j < target.length; j++) {
                int i = choice.map()[j];
                if (i >= 0) mv.visitVarInsn(target[j].getOpcode(Opcodes.ILOAD), slots[i]);
                else mv.visitInsn(CtorChooser.defaultConst(target[j]));
            }
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, sup, "<init>", choice.desc(), false);
        }
        for (Member f : instanceInits) {
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            if (!f.desc().startsWith("[") || !InstanceArrays.push(mv, version, legacyName(name), f.name(), f.desc())) Defaults.pushDefault(mv, Type.getType(f.desc()));
            mv.visitFieldInsn(Opcodes.PUTFIELD, name, f.name(), f.desc());
        }
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void enumMethods(ClassWriter cw, String name) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "values", "()[L" + name + ";", null, null);
        mv.visitCode();
        mv.visitFieldInsn(Opcodes.GETSTATIC, name, "$VALUES", "[L" + name + ";");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "[L" + name + ";", "clone", "()Ljava/lang/Object;", false);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "[L" + name + ";");
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "valueOf", "(Ljava/lang/String;)L" + name + ";", null, null);
        mv.visitCode();
        mv.visitLdcInsn(Type.getObjectType(name));
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Enum", "valueOf", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;", false);
        mv.visitTypeInsn(Opcodes.CHECKCAST, name);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /** The hand-written shim type of a field descriptor when it declares {@code static T mcc$synthDefault(String)}. */
    private String synthDefaultOwner(String desc) {
        if (!desc.startsWith("L")) return null;
        String t = desc.substring(1, desc.length() - 1);
        if (hierarchy.isSynth(t)) return null;
        ClassInfo info = hierarchy.find(t);
        if (info == null) return null;
        Integer access = info.methods.get("mcc$synthDefault(Ljava/lang/String;)" + desc);
        return access != null && (access & Opcodes.ACC_STATIC) != 0 ? t : null;
    }

    /** True when a hand-written shim superclass of {@code name} has a concrete {@code nameDesc}. */
    private boolean implementedAbove(String name, String nameDesc) {
        Set<String> seen = new HashSet<>();
        for (String c = superOf(name); c != null && seen.add(c); ) {
            ClassInfo info = hierarchy.find(c);
            if (info == null) return false;
            if (!hierarchy.isSynth(c)) {
                Integer access = info.methods.get(nameDesc);
                if (access != null) return (access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_STATIC)) == 0;
            }
            c = info.superName;
        }
        return false;
    }

    /**
     * A concrete synthesized class must implement the abstract methods it inherits from hand-written shim classes
     * and interfaces, or calling one through the shim type throws AbstractMethodError.
     */
    private void implementInheritedAbstract(ClassWriter cw, String name, Set<String> declared) {
        Map<String, Boolean> concrete = new TreeMap<>(); // name+desc -> has concrete impl
        Set<String> seen = new HashSet<>();
        List<String> interfaces = new ArrayList<>();
        String c = superOf(name);
        while (c != null && seen.add(c)) {
            ClassInfo info = hierarchy.find(c);
            if (info == null) break;
            for (var e : info.methods.entrySet()) {
                boolean abs = (e.getValue() & Opcodes.ACC_ABSTRACT) != 0;
                if (!abs) concrete.put(e.getKey(), true);
                else concrete.putIfAbsent(e.getKey(), false);
            }
            interfaces.addAll(info.interfaces);
            c = info.superName;
        }
        ClassInfo self = hierarchy.find(name);
        if (self != null) interfaces.addAll(self.interfaces);
        while (!interfaces.isEmpty()) {
            String i = interfaces.remove(interfaces.size() - 1);
            if (!seen.add(i)) continue;
            ClassInfo info = hierarchy.find(i);
            if (info == null) continue;
            for (var e : info.methods.entrySet()) {
                if (e.getKey().startsWith("<")) continue;
                if ((e.getValue() & Opcodes.ACC_STATIC) != 0) continue;
                boolean abs = (e.getValue() & Opcodes.ACC_ABSTRACT) != 0;
                if (!abs) concrete.putIfAbsent(e.getKey(), true);
                else concrete.putIfAbsent(e.getKey(), false);
            }
            interfaces.addAll(info.interfaces);
        }
        for (var e : concrete.entrySet()) {
            if (e.getValue()) continue;
            String key = e.getKey();
            int paren = key.indexOf('(');
            String mName = key.substring(0, paren);
            String mDesc = key.substring(paren);
            if (!declared.add("M" + mName + mDesc)) continue;
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, mName, mDesc, null, null);
            mv.visitCode();
            stubCall(mv, legacyName(name) + "." + mName + mDesc);
            Defaults.returnDefault(mv, Type.getReturnType(mDesc));
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }

    /**
     * The class to instantiate for a static field of type {@code desc}: a concrete synthesized class, or the generated
     * implementation of a synthesized interface. Null for anything else (shim types, enums, abstract classes).
     */
    private String instantiable(String desc) {
        if (!desc.startsWith("L")) return null;
        String t = desc.substring(1, desc.length() - 1);
        if (!hierarchy.isSynth(t) || !planned.contains(t) || isEnum(t)) return null;
        if (isInterface(t)) {
            LegacyHierarchyData.Entry e = hierarchy.legacyEntry(t);
            if (e != null && (e.access() & Opcodes.ACC_ANNOTATION) != 0) return null;
            impls.add(t);
            return t + IMPL_SUFFIX;
        }
        LegacyHierarchyData.Entry e = hierarchy.legacyEntry(t);
        return e != null && (e.access() & Opcodes.ACC_ABSTRACT) != 0 ? null : t;
    }

    /** {@code class I$$Impl implements I}: synthesized interface methods are defaults; inherited abstract ones get stubs. */
    private byte[] implClass(String iface) {
        String name = iface + IMPL_SUFFIX;
        ClassWriter cw = writer();
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER | Opcodes.ACC_SYNTHETIC, name, null, "java/lang/Object", new String[] {iface});
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        Set<String> declared = new HashSet<>();
        for (Member m : demand.members.getOrDefault(iface, Set.of())) {
            if (m.kind() == Kind.METHOD) declared.add("M" + m.name() + m.desc()); // defaults on the interface
        }
        List<String> queue = new ArrayList<>(List.of(iface));
        Set<String> seen = new HashSet<>();
        while (!queue.isEmpty()) {
            String i = queue.remove(0);
            if (!seen.add(i)) continue;
            ClassInfo info = hierarchy.find(i);
            if (info == null) continue;
            queue.addAll(info.interfaces);
            if (hierarchy.isSynth(i)) continue; // synthesized interfaces only have default methods
            for (var e : info.methods.entrySet()) {
                if ((e.getValue() & Opcodes.ACC_ABSTRACT) == 0 || e.getKey().startsWith("<")) continue;
                int paren = e.getKey().indexOf('(');
                String mName = e.getKey().substring(0, paren);
                String mDesc = e.getKey().substring(paren);
                if (!declared.add("M" + mName + mDesc)) continue;
                MethodVisitor stub = cw.visitMethod(Opcodes.ACC_PUBLIC, mName, mDesc, null, null);
                stub.visitCode();
                stubCall(stub, legacyName(i) + "." + mName + mDesc);
                Defaults.returnDefault(stub, Type.getReturnType(mDesc));
                stub.visitMaxs(0, 0);
                stub.visitEnd();
            }
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    // ------------------------------------------------------------------ helpers for hand-written classes

    private byte[] helperClass(String helper, String owner, Set<Member> members) {
        ClassWriter cw = writer();
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER | Opcodes.ACC_SYNTHETIC, helper, null, "java/lang/Object", null);
        String legacyOwner = legacyName(owner);
        Set<String> done = new HashSet<>();
        for (Member m : members) {
            switch (m.kind()) {
                case HELPER_VIRTUAL -> {
                    if (!done.add(m.name() + m.desc())) continue;
                    MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, m.name(), m.desc(), null, null);
                    mv.visitCode();
                    Type[] args = Type.getArgumentTypes(m.desc());
                    Type ret = Type.getReturnType(m.desc());
                    String original = "(" + m.desc().substring(m.desc().indexOf(';') + 1);
                    int[] slots = slots(args, 0);
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitLdcInsn(m.name());
                    mv.visitLdcInsn(original);
                    mv.visitLdcInsn(args.length - 1);
                    mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
                    for (int i = 1; i < args.length; i++) {
                        mv.visitInsn(Opcodes.DUP);
                        mv.visitLdcInsn(i - 1);
                        mv.visitVarInsn(args[i].getOpcode(Opcodes.ILOAD), slots[i]);
                        Defaults.box(mv, args[i]);
                        mv.visitInsn(Opcodes.AASTORE);
                    }
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, RT, "dispatch", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;", false);
                    int local = slots.length == 0 ? 0 : slots[slots.length - 1] + args[args.length - 1].getSize();
                    mv.visitVarInsn(Opcodes.ASTORE, local);
                    mv.visitVarInsn(Opcodes.ALOAD, local);
                    mv.visitFieldInsn(Opcodes.GETSTATIC, RT, "NONE", "Ljava/lang/Object;");
                    Label fallback = new Label();
                    mv.visitJumpInsn(Opcodes.IF_ACMPEQ, fallback);
                    if (ret.getSort() == Type.VOID) {
                        mv.visitInsn(Opcodes.RETURN);
                    } else {
                        mv.visitVarInsn(Opcodes.ALOAD, local);
                        Defaults.unbox(mv, ret);
                        mv.visitInsn(ret.getOpcode(Opcodes.IRETURN));
                    }
                    mv.visitLabel(fallback);
                    // a runtime implementation of the missing method (its signature names synthesized classes, so the
                    // hand-written class cannot declare it), then the default
                    instanceResolverCall(mv, legacyOwner + "." + m.name(), original);
                    stubCall(mv, legacyOwner + "." + m.name() + original);
                    if (returnsSelf(owner, ret)) {
                        mv.visitVarInsn(Opcodes.ALOAD, 0);
                        mv.visitInsn(Opcodes.ARETURN);
                    } else {
                        Defaults.returnDefault(mv, ret);
                    }
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                }
                case HELPER_SPECIAL, HELPER_STATIC -> {
                    String name = (m.kind() == Kind.HELPER_SPECIAL ? "super$" : "static$") + m.name();
                    if (!done.add(name + m.desc())) continue;
                    MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, name, m.desc(), null, null);
                    mv.visitCode();
                    if (m.kind() == Kind.HELPER_STATIC) staticResolverCall(mv, legacyOwner + "." + m.name(), m.desc());
                    stubCall(mv, legacyOwner + "." + m.name() + m.desc());
                    Defaults.returnDefault(mv, Type.getReturnType(m.desc()));
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                }
                case HELPER_FIELD, HELPER_STATIC_FIELD -> {
                    boolean isStatic = m.kind() == Kind.HELPER_STATIC_FIELD;
                    if (!done.add((isStatic ? "S" : "I") + m.name() + m.desc())) continue;
                    fieldAccessors(cw, owner, legacyOwner + "." + m.name(), m.name(), m.desc(), isStatic);
                }
                default -> { }
            }
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    private void fieldAccessors(ClassWriter cw, String owner, String key, String name, String desc, boolean isStatic) {
        Type t = Type.getType(desc);
        String self = isStatic ? "" : "L" + owner + ";";
        // getter: Object v = Synth.getField(self|null, key); if (v == null) { v = default; Synth.setField(...) } return unbox(v)
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, (isStatic ? "sget$" : "get$") + name, "(" + self + ")" + desc, null, null);
        mv.visitCode();
        if (isStatic) mv.visitInsn(Opcodes.ACONST_NULL);
        else mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn(key);
        if (Defaults.isCollection(desc) || Defaults.blankable(t) || t.getSort() != Type.OBJECT && t.getSort() != Type.ARRAY) {
            Defaults.pushDefault(mv, t);
            Defaults.box(mv, t);
        } else {
            mv.visitInsn(Opcodes.ACONST_NULL);
        }
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, RT, "getField", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;", false);
        Defaults.unbox(mv, t);
        mv.visitInsn(t.getOpcode(Opcodes.IRETURN));
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, (isStatic ? "sput$" : "set$") + name, "(" + self + desc + ")V", null, null);
        mv.visitCode();
        if (isStatic) mv.visitInsn(Opcodes.ACONST_NULL);
        else mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn(key);
        mv.visitVarInsn(t.getOpcode(Opcodes.ILOAD), isStatic ? 0 : 1);
        Defaults.box(mv, t);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, RT, "setField", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    // ------------------------------------------------------------------ utilities

    /** True if an instance of {@code owner} is a valid return value of type {@code ret} (and ret is not Object). */
    private boolean returnsSelf(String owner, Type ret) {
        if (ret.getSort() != Type.OBJECT || ret.getInternalName().equals("java/lang/Object")) return false;
        return isSubtype(owner, ret.getInternalName(), new HashSet<>());
    }

    private boolean isSubtype(String c, String target, Set<String> seen) {
        if (c == null || !seen.add(c)) return false;
        if (c.equals(target)) return true;
        ClassInfo info = hierarchy.find(c);
        if (info == null) return false;
        if (isSubtype(info.superName, target, seen)) return true;
        for (String i : info.interfaces) if (isSubtype(i, target, seen)) return true;
        return false;
    }

    /**
     * A static stub first asks the runtime's static method resolvers ({@code Synth.callStatic}); a resolver that knows the
     * method (e.g. {@code PotionType.getPotionTypeForName}) answers it, otherwise the stub's default follows.
     */
    private static void staticResolverCall(MethodVisitor mv, String key, String desc) {
        resolverCall(mv, key, desc, false);
    }

    /**
     * An instance stub of a synthesized class first asks the runtime's instance method resolvers ({@code Synth.callInstance}):
     * vanilla enums answer their names and metadata there, for example.
     */
    private static void instanceResolverCall(MethodVisitor mv, String key, String desc) {
        resolverCall(mv, key, desc, true);
    }

    private static void resolverCall(MethodVisitor mv, String key, String desc, boolean instance) {
        Type[] args = Type.getArgumentTypes(desc);
        Type ret = Type.getReturnType(desc);
        int[] slots = slots(args, instance ? 1 : 0);
        mv.visitLdcInsn(key);
        if (instance) mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn(args.length);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        for (int i = 0; i < args.length; i++) {
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn(i);
            mv.visitVarInsn(args[i].getOpcode(Opcodes.ILOAD), slots[i]);
            Defaults.box(mv, args[i]);
            mv.visitInsn(Opcodes.AASTORE);
        }
        if (instance) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, RT, "callInstance", "(Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
        } else {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, RT, "callStatic", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;", false);
        }
        int local = slots.length == 0 ? (instance ? 1 : 0) : slots[slots.length - 1] + args[args.length - 1].getSize();
        mv.visitVarInsn(Opcodes.ASTORE, local);
        mv.visitVarInsn(Opcodes.ALOAD, local);
        mv.visitFieldInsn(Opcodes.GETSTATIC, RT, "NONE", "Ljava/lang/Object;");
        Label fallback = new Label();
        mv.visitJumpInsn(Opcodes.IF_ACMPEQ, fallback);
        if (ret.getSort() == Type.VOID) {
            mv.visitInsn(Opcodes.RETURN);
        } else {
            mv.visitVarInsn(Opcodes.ALOAD, local);
            if (ret.getSort() == Type.OBJECT || ret.getSort() == Type.ARRAY) mv.visitTypeInsn(Opcodes.CHECKCAST, ret.getInternalName());
            else Defaults.unbox(mv, ret);
            mv.visitInsn(ret.getOpcode(Opcodes.IRETURN));
        }
        mv.visitLabel(fallback);
    }

    private static void stubCall(MethodVisitor mv, String key) {
        mv.visitLdcInsn(key);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, RT, "stub", "(Ljava/lang/String;)V", false);
    }

    /** A vanilla descriptor with its class names relocated the way mod bytecode is (shim class, or synthesized). */
    private String relocateDesc(String desc) {
        if (desc.indexOf('L') < 0) return desc;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("L([^;\\[()]+);").matcher(desc);
        StringBuilder out = new StringBuilder();
        while (m.find()) m.appendReplacement(out, java.util.regex.Matcher.quoteReplacement("L" + hierarchy.relocate(m.group(1)) + ";"));
        m.appendTail(out);
        return out.toString();
    }

    private String legacyName(String relocated) {
        for (String p : List.of(version.synthPrefix(), version.shimPrefix())) {
            if (relocated.startsWith(p)) return relocated.substring(p.length()).replace('/', '.');
        }
        return relocated.replace('/', '.');
    }

    private static int[] slots(Type[] args, int first) {
        int[] s = new int[args.length];
        int slot = first;
        for (int i = 0; i < args.length; i++) {
            s[i] = slot;
            slot += args[i].getSize();
        }
        return s;
    }

    private static ClassWriter writer() {
        return new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(String a, String b) {
                return "java/lang/Object"; // generated code only merges values it treats as Object
            }
        };
    }
}
