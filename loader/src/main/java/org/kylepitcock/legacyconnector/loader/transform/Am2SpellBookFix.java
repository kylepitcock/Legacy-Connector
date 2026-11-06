package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** The pickup hook must finish the written book already held by the menu. */
final class Am2SpellBookFix {
    private static final String STACK = "legacyconnector/v1_7_10/net/minecraft/item/ItemStack";
    private static final String FINISH = "(L" + STACK + ";Ljava/lang/String;Ljava/lang/String;)L" + STACK + ";";

    private Am2SpellBookFix() {}

    static void apply(ClassNode node) {
        if (!node.name.equals("am2/blocks/tileentities/TileEntityInscriptionTable")) return;
        for (var method : node.methods) {
            if (!method.name.equals("writeRecipeAndDataToBook")) continue;
            for (var instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode call) || !call.owner.equals("am2/lore/Story")
                        || !call.name.equals("finalizeStory") || !call.desc.equals(FINISH)) continue;
                call.owner = STACK;
                call.name = "mcc$finishWrittenBook";
                return;
            }
        }
    }
}
