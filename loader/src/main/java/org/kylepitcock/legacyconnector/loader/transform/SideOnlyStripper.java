package org.kylepitcock.legacyconnector.loader.transform;

import java.util.List;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

/**
 * FML's SideTransformer: fields and methods annotated {@code @SideOnly(Side.CLIENT)} do not exist on a dedicated
 * server (and {@code SERVER} ones not on the client). Mods relied on it, e.g. by declaring client-only method
 * signatures in common classes; keeping them would make verification load client classes on the server.
 */
public final class SideOnlyStripper {
    private SideOnlyStripper() {}

    /** @param side "CLIENT" or "SERVER": the side we are running on */
    public static int apply(ClassNode node, String shimPrefix, String side) {
        String desc = "L" + shimPrefix + "cpw/mods/fml/relauncher/SideOnly;";
        int before = node.fields.size() + node.methods.size();
        node.fields.removeIf(f -> otherSide(f.visibleAnnotations, desc, side) || otherSide(f.invisibleAnnotations, desc, side));
        node.methods.removeIf(m -> otherSide(m.visibleAnnotations, desc, side) || otherSide(m.invisibleAnnotations, desc, side));
        return before - node.fields.size() - node.methods.size();
    }

    private static boolean otherSide(List<AnnotationNode> annotations, String desc, String side) {
        if (annotations == null) return false;
        for (AnnotationNode a : annotations) {
            if (!a.desc.equals(desc) || a.values == null) continue;
            for (int i = 0; i + 1 < a.values.size(); i += 2) {
                if ("value".equals(a.values.get(i)) && a.values.get(i + 1) instanceof String[] e && e.length == 2 && !e[1].equals(side)) return true;
            }
        }
        return false;
    }
}
