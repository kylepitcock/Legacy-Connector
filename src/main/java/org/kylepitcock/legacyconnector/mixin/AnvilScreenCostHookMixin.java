package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Coremod hook site (generic, {@code CoremodSites.ANVIL_MAX_COST}): the "Too Expensive!" limit of the anvil screen ({@code GuiRepair}), which 1.7.10 coremods
 * replaced together with the container's. A no-op without such a hook.
 */
@Mixin(AnvilScreen.class)
public abstract class AnvilScreenCostHookMixin {
    @ModifyConstant(method = "renderLabels", constant = @Constant(intValue = 40), require = 0)
    private int legacyconnector$coremodMaxCost(int cost) {
        return CoremodSites.ANVIL_MAX_COST.listed() ? CoremodSites.anvilMaxCost(cost) : cost;
    }
}
