package org.kylepitcock.legacyconnector.v1_12_2.adapter;

import legacyconnector.v1_12_2.net.minecraft.item.Item;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.kylepitcock.legacyconnector.v1_12_2.entity.Hosts1122;

/**
 * The name of a stack as 1.12.2 built it: the mod's own getItemStackDisplayName when it overrides it, otherwise the translation of
 * getUnlocalizedName(stack) + ".name". The key depends on the stack (metadata and NBT): AbyssalCraft's nuggets, essences, scrolls
 * and statues are named "item.nugget.coralium.name" or "tile.statue.3.name", not the key of the item class alone.
 */
final class ItemNames1122 {
    private ItemNames1122() {}

    /** The name component, or null when the legacy item gives no usable name (the caller then keeps the modern default). */
    static Component name(Item legacy, ItemStack stack, int fallbackMeta) {
        try {
            legacyconnector.v1_12_2.net.minecraft.item.ItemStack view = Stacks1122.fromModern(stack);
            if (view == null || view.isEmpty()) view = new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(legacy, 1, fallbackMeta);
            if (Hosts1122.overrides(legacy.getClass(), "getItemStackDisplayName", legacyconnector.v1_12_2.net.minecraft.item.ItemStack.class)) {
                String name = legacy.getItemStackDisplayName(view);
                if (name != null) return Component.literal(name);
            }
            // 1.12's getItemStackDisplayName translates getUnlocalizedNameInefficiently(stack): Botania's ItemMod prefixes the mod id there
            String key = Hosts1122.overrides(legacy.getClass(), "getUnlocalizedNameInefficiently", legacyconnector.v1_12_2.net.minecraft.item.ItemStack.class)
                    ? legacy.getUnlocalizedNameInefficiently(view) : legacy.getUnlocalizedName(view);
            if (key != null) return Component.translatable(key + ".name");
        } catch (RuntimeException | LinkageError e) {
            // keep the default name
        }
        return null;
    }
}
