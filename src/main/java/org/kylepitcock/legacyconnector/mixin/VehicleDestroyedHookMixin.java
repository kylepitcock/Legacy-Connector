package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook sites (generic, {@code CoremodSites1122.BOAT_DROPS}, {@code MINECART_DROPS}): 1.12.2 coremods put a hook after the item a destroyed boat
 * or minecart dropped (to drop what was attached to it as well). No-ops without such hooks.
 */
@CoremodHook
@Mixin(VehicleEntity.class)
public abstract class VehicleDestroyedHookMixin {
    @Inject(method = "destroy(Lnet/minecraft/world/item/Item;)V", at = @At("TAIL"), require = 0)
    private void legacyconnector$coremodVehicleDestroyed(Item dropItem, CallbackInfo ci) {
        if (CoremodSites1122.BOAT_DROPS.listed() || CoremodSites1122.MINECART_DROPS.listed()) CoremodSites1122.vehicleDestroyed((VehicleEntity) (Object) this);
    }
}
