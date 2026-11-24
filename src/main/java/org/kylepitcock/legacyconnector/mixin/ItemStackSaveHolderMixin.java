package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.kylepitcock.legacyconnector.core.StackHolders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A stack whose enchantment holders belong to another registry set (see {@link StackHolders}) failed the save of the player or chunk holding it
 * with "Element ... is not valid in current registry set" (issue 113). Before the stack is written its holders are looked up again by key; a
 * save that fails all the same is retried with the holders forced, and as a last resort written without the enchantments.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackSaveHolderMixin {
    @Inject(method = "save(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/Tag;)Lnet/minecraft/nbt/Tag;", at = @At("HEAD"), require = 0)
    private void legacyconnector$repairForeignHolders(HolderLookup.Provider provider, Tag tag, CallbackInfoReturnable<Tag> cir) {
        StackHolders.repair((ItemStack) (Object) this, provider);
    }

    @WrapOperation(method = "save(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/Tag;)Lnet/minecraft/nbt/Tag;", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/common/util/DataComponentUtil;wrapEncodingExceptions(Lnet/minecraft/core/component/DataComponentHolder;"
                    + "Lcom/mojang/serialization/Codec;Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/Tag;)Lnet/minecraft/nbt/Tag;"), require = 0)
    private Tag legacyconnector$recoverRegistrySetFailure(DataComponentHolder holder, Codec<?> codec, HolderLookup.Provider provider, Tag tag,
            Operation<Tag> original) {
        try {
            return original.call(holder, codec, provider, tag);
        } catch (IllegalStateException e) {
            if (e.getMessage() == null || !e.getMessage().contains("registry set")) throw e;
            return StackHolders.recover((ItemStack) (Object) this, provider, e, s -> original.call(s, codec, provider, tag));
        }
    }
}
