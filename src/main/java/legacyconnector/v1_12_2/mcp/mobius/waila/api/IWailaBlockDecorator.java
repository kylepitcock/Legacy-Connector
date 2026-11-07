package legacyconnector.v1_12_2.mcp.mobius.waila.api;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** Draws over the looked-at block. Accepted and ignored: Jade has no equivalent hook. */
public interface IWailaBlockDecorator {
    void decorateBlock(ItemStack itemStack, IWailaDataAccessor accessor, IWailaConfigHandler config);
}
