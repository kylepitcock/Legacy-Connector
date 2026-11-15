package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import net.minecraft.world.inventory.LoomMenu;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.BANNER_LAYERS}): 1.12.2 coremods that raise the banner layer limit put a hook after the layer count
 * of the recipe that adds a pattern; the loom is the modern place of that limit (the count of layers a banner may have, 6). A no-op without such a hook.
 */
@Mixin(LoomMenu.class)
public abstract class LoomLayersHookMixin {
    @WrapOperation(method = "slotsChanged", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 1), require = 0)
    private int legacyconnector$coremodBannerLayers(List<?> layers, Operation<Integer> original) {
        int count = original.call(layers);
        return CoremodSites1122.BANNER_LAYERS.listed() ? CoremodSites1122.bannerLayers(count) : count;
    }
}
