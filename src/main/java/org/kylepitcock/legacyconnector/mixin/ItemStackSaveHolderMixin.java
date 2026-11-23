package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.kylepitcock.legacyconnector.core.StackHolders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A stack whose enchantment holders belong to another registry set (see {@link StackHolders}) failed the save of the player or chunk holding it
 * with "Element ... is not valid in current registry set" (issue 113). Before the stack is written its holders are looked up again by key.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackSaveHolderMixin {
    @Inject(method = "save(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/Tag;)Lnet/minecraft/nbt/Tag;", at = @At("HEAD"), require = 0)
    private void legacyconnector$repairForeignHolders(HolderLookup.Provider provider, Tag tag, CallbackInfoReturnable<Tag> cir) {
        StackHolders.repair((ItemStack) (Object) this, provider);
    }
}
