package org.kylepitcock.legacyconnector.loader.transform.compat;

import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * MOD-SPECIFIC FIX (BuildCraft 3.x to 7.x: {@code core.utils.Utils} of 1.4.7 to 1.6.4, {@code core.lib.inventory.InvUtils} of 1.7.10), kept isolated so it is easy to remove: {@code buildcraft.core.utils.Utils.getInventory(IInventory)}
 * paired a chest with any chest tile beside it (the last of west, east, north, south won), which only worked while 1.4.7 to 1.7.10 refused
 * to put a third chest next to a double chest. The method is redirected to {@code compat.mods.buildcraft.ChestPairing}, which pairs a chest
 * with the partner its modern block state names.
 */
public final class BuildCraftChestPairing {
    private static final String HELPER = "org/kylepitcock/legacyconnector/compat/mods/buildcraft/ChestPairing";

    private BuildCraftChestPairing() {}

    public static int apply(ClassNode node, LegacyVersion version) {
        if (version.family() != LegacyVersion.V1_7_10 || !(node.name.equals("buildcraft/core/utils/Utils") || node.name.equals("buildcraft/core/lib/inventory/InvUtils"))) return 0;
        String inv = "L" + version.linkPrefix() + "net/minecraft/inventory/IInventory;";
        String desc = "(" + inv + ")" + inv;
        int changed = 0;
        for (MethodNode m : node.methods) {
            if (!m.name.equals("getInventory") || !m.desc.equals(desc) || (m.access & Opcodes.ACC_STATIC) == 0) continue;
            InsnList code = new InsnList();
            code.add(new VarInsnNode(Opcodes.ALOAD, 0));
            code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, "getInventory", "(Llegacyconnector/v1_7_10/net/minecraft/inventory/IInventory;)Llegacyconnector/v1_7_10/net/minecraft/inventory/IInventory;", false));
            code.add(new InsnNode(Opcodes.ARETURN));
            m.instructions = code;
            m.tryCatchBlocks.clear();
            m.localVariables = null;
            m.maxStack = 1;
            m.maxLocals = 1;
            changed++;
        }
        return changed;
    }
}
