package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites.ENCHANTABILITY}): 1.7.10 coremods pass the item's enchantability through a hook where the enchanting table
 * works out its levels. A no-op without such a hook.
 */
@CoremodHook
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperHookMixin {
    @ModifyExpressionValue(method = "getEnchantmentCost", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getEnchantmentValue()I"),
            require = 0)
    private static int legacyconnector$coremodEnchantability(int value, RandomSource random, int slot, int power, ItemStack stack) {
        return CoremodSites.ENCHANTABILITY.listed() ? CoremodSites.enchantability(stack, value) : value;
    }
}
