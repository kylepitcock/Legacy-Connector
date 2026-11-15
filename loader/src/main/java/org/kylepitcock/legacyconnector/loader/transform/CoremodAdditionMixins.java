package org.kylepitcock.legacyconnector.loader.transform;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.kylepitcock.legacyconnector.loader.LegacyJar;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Applies what a legacy coremod added to a Minecraft class (an interface, the fields behind it and their accessors: {@link CoremodHookDiscovery#additions})
 * the way a modern mod would: as a mixin on the shim class. The mixin is generated in legacy names into the jar that owns the added interface, before that
 * jar is translated, so it is relocated and renamed like the mod's own classes; the jar's generated {@code neoforge.mods.toml} registers its config.
 *
 * <p>The added methods keep the transformer's bodies. Their reads and writes of an added field go to the run time's {@code CoremodState} instead of a field
 * of the shim object: shim views of a game object are not all stable (a chunk view is made per lookup), the state is kept per game object.
 *
 * <p>Limits: static members and constructors are not added; a class whose additions include no interface of a jar with mods is left alone (nothing could
 * reach the members by type).
 */
final class CoremodAdditionMixins {
    static final String PACKAGE = "mcc_coremod/";
    static final String STATE = "org/kylepitcock/legacyconnector/v1_7_10/bridge/CoremodState";
    private static final String MIXIN_DESC = "Lorg/spongepowered/asm/mixin/Mixin;";
    /** Minecraft version -> the changes its coremods' transformers make to that Minecraft's classes (this start's scan; absent on cache hits, which need none). */
    private static final Map<String, List<CoremodHookScan.Change>> CHANGES = new ConcurrentHashMap<>();

    private CoremodAdditionMixins() {}

    static void record(String mc, List<CoremodHookScan.Change> changes) {
        CHANGES.put(mc, List.copyOf(changes));
    }

    private static String packageOf(LegacyJar jar) {
        return PACKAGE + "j" + Integer.toHexString(jar.fileName().hashCode()) + "/";
    }

    /** Adds the mixin classes (legacy names) for the additions whose interface this jar owns; returns how many. */
    static int addTo(LegacyJar jar, Map<String, byte[]> classes) {
        List<CoremodHookScan.Change> changes = CHANGES.get(jar.version.id());
        if (changes == null || !jar.hasMods()) return 0;
        Map<String, ClassNode> mixins = new LinkedHashMap<>();
        Set<String> done = new HashSet<>();
        for (CoremodHookScan.Change c : changes) {
            ClassNode mixin;
            try {
                mixin = mixin(c.before(), c.after(), packageOf(jar), classes.keySet(), done);
            } catch (RuntimeException e) {
                continue; // an unreadable change: the class stays as the shim has it
            }
            if (mixin == null) continue;
            String name = mixin.name;
            for (int n = 2; mixins.containsKey(mixin.name); n++) mixin.name = name + n;
            mixins.put(mixin.name, mixin);
        }
        for (ClassNode mixin : mixins.values()) {
            ClassWriter writer = new ClassWriter(0);
            mixin.accept(writer);
            classes.put(mixin.name, writer.toByteArray());
        }
        return mixins.size();
    }

    /** The mixin for one change, or null when it adds nothing that can be applied; {@code done} keeps members already taken from another pass over the class. */
    static ClassNode mixin(ClassNode before, ClassNode after, String pkg, Set<String> jarClasses, Set<String> done) {
        List<String> interfaces = new ArrayList<>();
        for (String i : after.interfaces) {
            if (!before.interfaces.contains(i) && jarClasses.contains(i) && done.add(before.name + " " + i)) interfaces.add(i);
        }
        if (interfaces.isEmpty()) return null;
        Set<String> oldFields = new HashSet<>();
        for (FieldNode f : before.fields) oldFields.add(f.name + ":" + f.desc);
        Set<String> added = new HashSet<>();
        for (FieldNode f : after.fields) {
            if (!oldFields.contains(f.name + ":" + f.desc) && (f.access & Opcodes.ACC_STATIC) == 0) added.add(f.name + ":" + f.desc);
        }
        Set<String> oldMethods = new HashSet<>();
        for (MethodNode m : before.methods) oldMethods.add(m.name + m.desc);

        ClassNode mixin = new ClassNode();
        mixin.version = Opcodes.V1_6;
        mixin.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_SUPER;
        mixin.name = pkg + before.name.substring(before.name.lastIndexOf('/') + 1).replace('$', '_') + "Additions";
        mixin.superName = "java/lang/Object";
        mixin.interfaces.addAll(interfaces);
        AnnotationNode target = new AnnotationNode(MIXIN_DESC);
        target.values = new ArrayList<>(List.of("value", new ArrayList<>(List.of(Type.getObjectType(before.name)))));
        mixin.invisibleAnnotations = new ArrayList<>(List.of(target));
        for (MethodNode m : after.methods) {
            if (oldMethods.contains(m.name + m.desc) || m.name.startsWith("<") || (m.access & (Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT)) != 0) continue;
            if (!done.add(before.name + " " + m.name + m.desc)) continue;
            MethodNode copy = new MethodNode((m.access & ~(Opcodes.ACC_FINAL | Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC, m.name, m.desc, m.signature,
                    m.exceptions == null ? null : m.exceptions.toArray(new String[0]));
            m.accept(copy);
            state(copy.instructions, before.name, added);
            copy.maxStack += 2;
            for (AbstractInsnNode insn : copy.instructions) if (insn instanceof InvokeDynamicInsnNode) mixin.version = Opcodes.V1_8;
            mixin.methods.add(copy);
        }
        return mixin;
    }

    /** Reads and writes of an added instance field become calls of the per-object state. */
    private static void state(InsnList code, String owner, Set<String> added) {
        for (AbstractInsnNode insn : code.toArray()) {
            if (!(insn instanceof FieldInsnNode f) || !f.owner.equals(owner) || !added.contains(f.name + ":" + f.desc)) continue;
            boolean get = f.getOpcode() == Opcodes.GETFIELD;
            if (!get && f.getOpcode() != Opcodes.PUTFIELD) continue;
            Type type = Type.getType(f.desc);
            String kind;
            String value;
            switch (type.getSort()) {
                case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> { kind = "Int"; value = "I"; }
                case Type.LONG -> { kind = "Long"; value = "J"; }
                case Type.FLOAT -> { kind = "Float"; value = "F"; }
                case Type.DOUBLE -> { kind = "Double"; value = "D"; }
                default -> { kind = ""; value = "Ljava/lang/Object;"; }
            }
            InsnList call = new InsnList();
            call.add(new LdcInsnNode(owner.substring(owner.lastIndexOf('/') + 1) + "." + f.name));
            if (get && kind.isEmpty() && type.getSort() == Type.OBJECT) {
                // with the field's legacy type: a back reference nothing wrote reads as the owner of the object (CoremodState.get)
                call.add(new LdcInsnNode(type.getInternalName()));
                call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, STATE, "get", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;", false));
                call.add(new TypeInsnNode(Opcodes.CHECKCAST, type.getInternalName()));
            } else if (get) {
                call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, STATE, "get" + kind, "(Ljava/lang/Object;Ljava/lang/String;)" + value, false));
                if (kind.isEmpty()) call.add(new TypeInsnNode(Opcodes.CHECKCAST, type.getInternalName()));
            } else {
                call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, STATE, "put" + kind, "(Ljava/lang/Object;" + value + "Ljava/lang/String;)V", false));
            }
            code.insert(insn, call);
            code.remove(insn);
        }
    }

    /** The mixin config of the generated classes among {@code translated} (class names of the translated jar), or null when the jar has none. */
    static byte[] config(LegacyJar jar, Set<String> translated) {
        String pkg = packageOf(jar);
        List<String> names = new ArrayList<>();
        for (String name : translated) if (name.startsWith(pkg)) names.add(name.substring(pkg.length()));
        if (names.isEmpty()) return null;
        StringBuilder json = new StringBuilder("{\"required\":false,\"minVersion\":\"0.8\",\"compatibilityLevel\":\"JAVA_8\",\"package\":\"");
        json.append(pkg.substring(0, pkg.length() - 1).replace('/', '.')).append("\",\"mixins\":[");
        for (int i = 0; i < names.size(); i++) json.append(i == 0 ? "" : ",").append('"').append(names.get(i)).append('"');
        json.append("],\"injectors\":{\"defaultRequire\":0}}");
        return json.toString().getBytes(StandardCharsets.UTF_8);
    }

    static String configName(LegacyJar jar) {
        return LegacyMixinConfigs.PREFIX + jar.fileName().replaceAll("(?i)\\.jar$", "").replaceAll("[^A-Za-z0-9_-]", "_") + ".coremod-additions.json";
    }
}
