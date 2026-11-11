package org.kylepitcock.legacyconnector.v1_20_1;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.ItemStackNbt;

/**
 * Restores the small 1.20.1 ItemStack NBT surface used by KubeJS 6 scripts.
 *
 * <p>Delegate to the connector's full ItemStackNbt migration adapter so
 * legacy vanilla keys such as Damage, display.Name, Enchantments and
 * BlockEntityTag continue to map to their dedicated 1.21.1 data components.
 * Arbitrary legacy values are backed by minecraft:custom_data.</p>
 */
public final class LegacyItemStackNbt1201 {
    private LegacyItemStackNbt1201() {
    }

    /** Equivalent to the old ItemStack.hasTag()/KubeJS hasNBT() check. */
    public static boolean hasNBT(ItemStack stack) {
        return ItemStackNbt.hasTag(stack);
    }

    /** Returns the legacy view of the stack's NBT, or null when it has none. */
    public static CompoundTag getNbt(ItemStack stack) {
        return ItemStackNbt.getTag(stack);
    }

    /** Replaces the legacy view, converting vanilla keys to modern components. */
    public static void setNbt(ItemStack stack, CompoundTag nbt) {
        ItemStackNbt.setTag(stack, nbt);
    }
}
