package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.item.BannerItem;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.BANNER_TOOLTIP_LAYERS}): 1.12.2 coremods that raise the banner layer limit change the 6 layers the
 * banner tooltip lists. A no-op without such a hook.
 */
@CoremodHook
@Mixin(BannerItem.class)
public abstract class BannerTooltipLayersHookMixin {
    @ModifyConstant(method = "appendHoverTextFromBannerBlockEntityTag", constant = @Constant(intValue = 6), require = 0)
    private static int legacyconnector$coremodBannerTooltipLayers(int limit) {
        return CoremodSites1122.BANNER_TOOLTIP_LAYERS.listed() ? CoremodSites1122.bannerTooltipLayers(limit) : limit;
    }
}
