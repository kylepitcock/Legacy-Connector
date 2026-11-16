package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.inventory.AnvilMenu;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites.ANVIL_MAX_COST}): 1.7.10 coremods replaced the 40 levels above which {@code ContainerRepair.updateRepairOutput}
 * refuses a repair. A no-op without such a hook.
 */
@CoremodHook
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuCostHookMixin {
    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 40), require = 0)
    private int legacyconnector$coremodMaxCost(int cost) {
        return CoremodSites.ANVIL_MAX_COST.listed() ? CoremodSites.anvilMaxCost(cost) : cost;
    }
}
