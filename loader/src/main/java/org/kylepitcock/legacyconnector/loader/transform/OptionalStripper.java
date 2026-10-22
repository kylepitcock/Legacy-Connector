package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * FML's {@code @Optional} soft dependencies (legacy ModAPITransformer): an {@code @Optional.Interface} is removed from
 * the class, and an {@code @Optional.Method} removed entirely, when the named mod is not loaded. Without this, a mod
 * implementing e.g. an IC2 interface fails with NoClassDefFoundError when IC2 is absent. An interface that cannot be
 * found anywhere is removed even if its mod id is present, since it could never load; so is any other interface that
 * cannot be found (an absent mod's API).
 */
public final class OptionalStripper {
    private OptionalStripper() {}

    /** @param present legacy mod ids loaded alongside (case-insensitive) */
    public static int apply(ClassNode node, String shimPrefix, Set<String> present, Hierarchy hierarchy) {
        String base = "L" + shimPrefix + "cpw/mods/fml/common/Optional$";
        int stripped = 0;
        List<AnnotationNode> annotations = all(node.visibleAnnotations, node.invisibleAnnotations);
        List<String> removeIfaces = new ArrayList<>();
        for (AnnotationNode a : annotations) {
            if (a.desc.equals(base + "Interface;")) {
                check(a, present, hierarchy, removeIfaces);
            } else if (a.desc.equals(base + "InterfaceList;")) {
                Object v = value(a, "value");
                if (v instanceof List<?> list) for (Object o : list) if (o instanceof AnnotationNode inner) check(inner, present, hierarchy, removeIfaces);
            }
        }
        // Interfaces that exist nowhere (an absent mod's API, implemented without @Optional because the mod relied on
        // its own coremod - e.g. DragonAPI's @Strippable - to remove them) would make the class fail to load.
        for (String iface : node.interfaces) {
            if (hierarchy.isAbsent(iface)) removeIfaces.add(iface);
        }
        for (String iface : removeIfaces) {
            if (node.interfaces.remove(iface)) stripped++;
        }
        for (var it = node.methods.iterator(); it.hasNext(); ) {
            MethodNode m = it.next();
            for (AnnotationNode a : all(m.visibleAnnotations, m.invisibleAnnotations)) {
                if (a.desc.equals(base + "Method;") && !isPresent(value(a, "modid"), present)) {
                    it.remove();
                    stripped++;
                    break;
                }
            }
        }
        return stripped;
    }

    private static void check(AnnotationNode a, Set<String> present, Hierarchy hierarchy, List<String> remove) {
        Object iface = value(a, "iface");
        if (!(iface instanceof String name)) return;
        // The iface string was already relocated if it named a Minecraft/Forge class.
        String internal = name.replace('.', '/');
        if (!isPresent(value(a, "modid"), present) || hierarchy.find(internal) == null) remove.add(internal);
    }

    private static boolean isPresent(Object modid, Set<String> present) {
        return modid instanceof String s && present.contains(s.toLowerCase(java.util.Locale.ROOT));
    }

    private static Object value(AnnotationNode a, String key) {
        if (a.values == null) return null;
        for (int i = 0; i + 1 < a.values.size(); i += 2) if (key.equals(a.values.get(i))) return a.values.get(i + 1);
        return null;
    }

    private static List<AnnotationNode> all(List<AnnotationNode> a, List<AnnotationNode> b) {
        List<AnnotationNode> out = new ArrayList<>();
        if (a != null) out.addAll(a);
        if (b != null) out.addAll(b);
        return out;
    }
}
