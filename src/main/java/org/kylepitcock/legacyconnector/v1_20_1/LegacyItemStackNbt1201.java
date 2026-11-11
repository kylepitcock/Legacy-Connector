package org.kylepitcock.legacyconnector.v1_20_1;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Restores the small ItemStack NBT surface used by Forge/KubeJS 1.20.1 mods.
 *
 * <p>Since 1.20.5, arbitrary per-stack NBT is represented by the
 * {@code minecraft:custom_data} data component. The legacy API exposed the
 * compound directly and allowed scripts to mutate it in place, so
 * {@link #getNbt(ItemStack)} intentionally uses CustomData#getUnsafe rather
 * than returning a detached copy.</p>
 */
public final class LegacyItemStackNbt1201 {
    private LegacyItemStackNbt1201() {
    }

    /** Equivalent to the old ItemStack.hasTag()/KubeJS hasNBT() check. */
    public static boolean hasNBT(ItemStack stack) {
        return stack.get(DataComponents.CUSTOM_DATA) != null;
    }

    /**
     * Returns the live custom-data compound, or null when the stack has no
     * custom data, matching the old ItemStack#getTag() behavior.
     */
    @SuppressWarnings("deprecation")
    public static CompoundTag getNbt(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? null : customData.getUnsafe();
    }

    /**
     * Replaces the stack's custom data. A copy is stored so later mutations of
     * the caller's input tag do not mutate the ItemStack unexpectedly.
     */
    public static void setNbt(ItemStack stack, CompoundTag nbt) {
        if (nbt == null) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt.copy()));
        }
    }
}
