package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Coremod hook sites (generic, {@code CoremodSites1122.ENCHANT_FITS} and {@code DAMAGE_ENCHANT_FITS}): 1.12.2 coremods put a hook last in
 * {@code Enchantment.canApply} (and the damage enchantments' own) that lets an enchantment fit an item the game says it does not. NeoForge's
 * {@code supportsEnchantment} is that question on 1.21.1 (the anvil asks it, and the enchanting table through {@code isPrimaryItemFor}). A no-op without such hooks.
 */
@Mixin(IItemExtension.class)
public interface ItemSupportsEnchantmentHookMixin {
    @Inject(method = "supportsEnchantment", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$coremodEnchantmentFits(ItemStack stack, Holder<Enchantment> enchantment, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() || !(CoremodSites1122.ENCHANT_FITS.listed() || CoremodSites1122.DAMAGE_ENCHANT_FITS.listed())) return;
        if (CoremodSites1122.enchantmentFits(enchantment, stack)) cir.setReturnValue(true);
    }
}
