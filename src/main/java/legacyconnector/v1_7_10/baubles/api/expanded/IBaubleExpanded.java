package legacyconnector.v1_7_10.baubles.api.expanded;

import legacyconnector.v1_7_10.baubles.api.IBauble;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** Baubles Expanded: a bauble that names the slot types it fits by string ({@link BaubleExpandedSlots}), possibly several. */
public interface IBaubleExpanded extends IBauble {
    String[] getBaubleTypes(ItemStack itemstack);
}
