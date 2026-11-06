package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** Keep the lectern's flat ingredient preview readable from any angle. */
final class Am2LecternFix {
    private static final String PREFIX = "legacyconnector/v1_7_10/net/minecraft/";

    private Am2LecternFix() {}

    static void apply(ClassNode node) {
        if (!node.name.equals("am2/blocks/renderers/LecternRenderer")) return;
        for (var method : node.methods) {
            if (!method.name.equals("renderHelperIcon")) continue;
            for (var instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode call) || !call.owner.equals(PREFIX + "client/renderer/entity/RenderItem")
                        || !call.name.equals("doRender") || !call.desc.equals("(L" + PREFIX + "entity/item/EntityItem;DDDFF)V")) continue;
                call.name = "mcc$renderIngredientPreview";
                return;
            }
        }
    }
}
