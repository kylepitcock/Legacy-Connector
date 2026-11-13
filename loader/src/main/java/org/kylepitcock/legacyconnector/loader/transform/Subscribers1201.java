package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * {@code @Mod.EventBusSubscriber} classes whose hierarchy has <b>instance</b> {@code @SubscribeEvent} methods. Forge registered such a class for its static
 * methods only and ignored the instance ones (the object is registered by hand where the mod wants them: Citadel's {@code ServerProxy}); NeoForge's automatic
 * subscriber throws for the whole class. The annotation is removed and the constructors of the mod's {@code @Mod} class register the static methods
 * ({@code ForgeApi1201.subscribeStatics}), on the bus and for the sides the annotation named.
 */
final class Subscribers1201 {
    private static final String SUBSCRIBER = "Lnet/neoforged/fml/common/EventBusSubscriber;";
    private static final String MOD = "Lnet/neoforged/fml/common/Mod;";
    private static final String SUBSCRIBE = "Lnet/neoforged/bus/api/SubscribeEvent;";
    private static final String HELPER = "legacyconnector/v1_20_1/ForgeApi1201";

    private Subscribers1201() {}

    private record Spec(String bus, String dists, String className) {}

    private static AnnotationNode find(List<AnnotationNode> list, String desc) {
        if (list != null) for (AnnotationNode a : list) if (a.desc.equals(desc)) return a;
        return null;
    }

    /**
     * Events NeoForge refuses to listen to directly (abstract); Forge delivered every subclass. Read from {@code neoforge-abstract-events.tsv}, which
     * {@code loader/tools/EventTables1201Tool} generates from the NeoForge 21.1 jars (the idea of generating the list is Retromod's).
     */
    static final java.util.Set<String> ABSTRACT_EVENTS = loadAbstractEvents();

    private static java.util.Set<String> loadAbstractEvents() {
        java.util.Set<String> out = new java.util.HashSet<>();
        try (java.io.InputStream in = Subscribers1201.class.getClassLoader().getResourceAsStream("legacyconnector/forward/1.20.1/neoforge-abstract-events.tsv")) {
            if (in != null) {
                for (String l : new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).split("\\R")) {
                    if (l.isBlank() || l.startsWith("#")) continue;
                    out.add(l.split("\t")[0]);
                }
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        return java.util.Collections.unmodifiableSet(out);
    }
    private static boolean hasInstanceSubscriber(ClassNode node, Map<String, ClassNode> byName) {
        for (ClassNode c = node; c != null; c = c.superName == null ? null : byName.get(c.superName)) {
            for (MethodNode m : c.methods) {
                if (find(m.visibleAnnotations, SUBSCRIBE) == null) continue;
                if ((m.access & Opcodes.ACC_STATIC) == 0) return true;
                Type[] args = Type.getArgumentTypes(m.desc);
                if (args.length == 1 && args[0].getSort() == Type.OBJECT && ABSTRACT_EVENTS.contains(args[0].getInternalName())) return true;
            }
        }
        return false;
    }

    private static boolean hasAnySubscriber(ClassNode node, Map<String, ClassNode> byName) {
        for (ClassNode c = node; c != null; c = c.superName == null ? null : byName.get(c.superName))
            for (MethodNode m : c.methods) if (find(m.visibleAnnotations, SUBSCRIBE) != null) return true;
        return false;
    }

    /** @return the number of classes whose annotation was replaced */
    static int apply(List<ClassNode> nodes) {
        Map<String, ClassNode> byName = new LinkedHashMap<>();
        for (ClassNode n : nodes) byName.put(n.name, n);
        List<Spec> specs = new ArrayList<>();
        for (ClassNode n : nodes) {
            AnnotationNode sub = find(n.visibleAnnotations, SUBSCRIBER);
            if (sub == null || hasAnySubscriber(n, byName) && !hasInstanceSubscriber(n, byName)) continue;
            String bus = "GAME";
            StringBuilder dists = new StringBuilder();
            if (sub.values != null) {
                for (int i = 0; i + 1 < sub.values.size(); i += 2) {
                    Object key = sub.values.get(i);
                    Object v = sub.values.get(i + 1);
                    if ("bus".equals(key) && v instanceof String[] e && e.length == 2) bus = e[1].equals("MOD") ? "MOD" : "GAME";
                    if ("value".equals(key) && v instanceof List<?> l) {
                        for (Object o : l) if (o instanceof String[] e && e.length == 2) dists.append(dists.isEmpty() ? "" : ",").append(e[1]);
                    }
                }
            }
            n.visibleAnnotations.remove(sub);
            specs.add(new Spec(bus, dists.toString(), n.name.replace('/', '.')));
        }
        if (specs.isEmpty()) return 0;
        ClassNode main = null;
        for (ClassNode n : nodes) if (find(n.visibleAnnotations, MOD) != null) { main = n; break; }
        if (main == null) return 0;
        for (MethodNode m : main.methods) {
            if (!m.name.equals("<init>") || m.instructions == null) continue;
            AbstractInsnNode superCall = null;
            for (AbstractInsnNode in : m.instructions) {
                if (in instanceof MethodInsnNode mi && mi.getOpcode() == Opcodes.INVOKESPECIAL && mi.name.equals("<init>") && mi.owner.equals(main.superName)) {
                    superCall = in;
                    break;
                }
            }
            if (superCall == null) continue; // delegates to another constructor of the class
            InsnList add = new InsnList();
            for (Spec s : specs) {
                add.add(new LdcInsnNode(Type.getObjectType(main.name)));
                add.add(new LdcInsnNode(s.bus()));
                add.add(new LdcInsnNode(s.dists()));
                add.add(new LdcInsnNode(s.className()));
                add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, "subscribeStatics", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false));
            }
            m.instructions.insert(superCall, add);
            m.maxStack = Math.max(m.maxStack, 4);
        }
        return specs.size();
    }
}
