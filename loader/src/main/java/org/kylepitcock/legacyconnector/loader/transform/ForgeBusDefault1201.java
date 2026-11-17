package org.kylepitcock.legacyconnector.loader.transform;

import java.util.function.Predicate;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.kylepitcock.legacyconnector.loader.shim.ShimIndex;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * {@code @Mod.EventBusSubscriber} without {@code bus = MOD} subscribes to the FORGE bus in Forge 1.20.1, where mod bus events (registration events, setup
 * events, {@code RegisterGuiOverlaysEvent}...) are never posted: such a handler never ran. NeoForge's automatic subscriber puts a handler of a mod bus event
 * on the mod bus whatever the annotation says, so the handler would run (Iron's Spells registered its overlays twice: once from its own
 * {@code addListener}, once from the stray annotation). The {@code @SubscribeEvent} of those handlers is removed.
 */
final class ForgeBusDefault1201 {
    private ForgeBusDefault1201() {}

    private static final String SUBSCRIBER = "Lnet/neoforged/fml/common/EventBusSubscriber;";
    private static final String SUBSCRIBE = "Lnet/neoforged/bus/api/SubscribeEvent;";
    private static final String MOD_BUS_EVENT = "net/neoforged/fml/event/IModBusEvent";

    /** @return the number of handlers made inert */
    static int apply(ClassNode node, Mc1201Hierarchy hierarchy, ShimIndex shim) {
        if (node.visibleAnnotations == null) return 0;
        AnnotationNode sub = null;
        for (AnnotationNode a : node.visibleAnnotations) if (SUBSCRIBER.equals(a.desc)) sub = a;
        if (sub == null || isModBus(sub)) return 0;
        int n = 0;
        for (MethodNode m : node.methods) {
            if (m.visibleAnnotations == null) continue;
            Type[] params = Type.getArgumentTypes(m.desc);
            if (params.length != 1 || params[0].getSort() != Type.OBJECT) continue;
            if (!modBusEvent(params[0].getInternalName(), hierarchy, shim)) continue;
            if (m.visibleAnnotations.removeIf(a -> SUBSCRIBE.equals(a.desc))) n++;
        }
        return n;
    }

    private static boolean isModBus(AnnotationNode sub) {
        if (sub.values == null) return false;
        for (int i = 0; i + 1 < sub.values.size(); i += 2) {
            if ("bus".equals(sub.values.get(i)) && sub.values.get(i + 1) instanceof String[] e && e.length == 2 && "MOD".equals(e[1])) return true;
        }
        return false;
    }

    private static boolean modBusEvent(String type, Mc1201Hierarchy hierarchy, ShimIndex shim) {
        for (int depth = 0; type != null && depth < 24; depth++) {
            ClassInfo info = shim.get(type);
            if (info == null) return hierarchy.isSubclassOf(type, MOD_BUS_EVENT);
            if (info.interfaces.contains(MOD_BUS_EVENT)) return true;
            for (String i : info.interfaces) if (modBusEvent(i, hierarchy, shim)) return true;
            type = info.superName;
        }
        return false;
    }
}
