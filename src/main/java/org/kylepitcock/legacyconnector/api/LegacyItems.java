package org.kylepitcock.legacyconnector.api;

import net.minecraft.world.item.ItemStack;
import org.kylepitcock.legacyconnector.api.internal.Convert;

/**
 * Item stack conversion between modern and legacy stacks. A legacy stack is an opaque {@code Object}: an instance of the
 * legacy version's own {@code net.minecraft.item.ItemStack}. Use it with {@link LegacyAccess} (e.g. as an argument of a legacy mod's
 * API) or hand it back to {@link #fromLegacy(Object)}.
 *
 * <p>Modern items that did not exist in the legacy version (copper ingot, amethyst shard) get a legacy stand-in item with a stable
 * identity: the same modern item always converts to the same legacy item and damage, so a legacy mod can key data by it
 * (Thaumcraft's aspects, ore dictionary entries, recipes).
 */
public final class LegacyItems {
    private LegacyItems() {}

    /**
     * The legacy stack for a modern one: its item, count, damage (metadata) and tag. Empty stacks convert to {@code null} for 1.7.10
     * (where an empty stack is {@code null}) and to {@code ItemStack.EMPTY} for 1.12.2. For 1.7.10 the result is a live view of the
     * modern stack (changes to the count or damage reach it); copy the modern stack first if that is unwanted.
     */
    public static Object toLegacy(ItemStack modern, LegacyVersion version) {
        return Convert.itemToLegacy(modern, version);
    }

    /** The modern stack for a legacy one ({@code null} or an empty legacy stack give {@link ItemStack#EMPTY}). A copy for 1.12.2, the backing stack for 1.7.10 views. */
    public static ItemStack fromLegacy(Object legacyStack) {
        return Convert.itemFromLegacy(legacyStack);
    }
}
