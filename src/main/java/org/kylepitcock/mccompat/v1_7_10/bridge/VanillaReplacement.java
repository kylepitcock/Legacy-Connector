package org.kylepitcock.mccompat.v1_7_10.bridge;

import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.item.Item;

/**
 * Mods replaced vanilla singletons ({@code Blocks.quartz_ore = new BlockOre(){...}}) and then overwrote the registry
 * entry. The linker routes those assignments here so the replacement stands for the vanilla block it replaced
 * (it has an item form, a name and an id).
 */
public final class VanillaReplacement {
    private VanillaReplacement() {}

    public static void adopt(Object replacement, Object original) {
        if (replacement == original || replacement == null || original == null) return;
        if (replacement instanceof Block r && original instanceof Block o && r.mcc$vanilla == null && o.mcc$vanilla != null) {
            BlockBridge.adoptVanilla(r, o);
        } else if (replacement instanceof Item r && original instanceof Item o && r.mcc$vanilla == null && o.mcc$vanilla != null) {
            r.mcc$vanilla = o.mcc$vanilla;
        }
    }
}
