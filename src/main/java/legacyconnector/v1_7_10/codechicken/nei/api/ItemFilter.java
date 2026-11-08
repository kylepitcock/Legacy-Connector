package legacyconnector.v1_7_10.codechicken.nei.api;

import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** A predicate on item stacks (NEI subsets, search). */
public interface ItemFilter {
    boolean matches(ItemStack item);

    /** Ignores nothing: accepts every stack. */
    ItemFilter ALL = item -> true;
}
