package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.ItemStackNbt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Restores the complete 1.20.1 ItemStack root tag at MobBucketItem's post-spawn Bucketable callback.
 *
 * <p>1.21.1 passes only BUCKET_ENTITY_DATA here. The 1.20.1 call passed getOrCreateTag(), including residual mod NBT,
 * componentized vanilla keys, and EntityTag. Build that legacy view without mutating the stack, then retain the original
 * virtual call so modded Bucketable implementations continue to receive their own data.</p>
 */
@Mixin(MobBucketItem.class)
public abstract class MobBucketItemLegacyNbtMixin {
    @WrapOperation(
            method = "spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/BlockPos;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/animal/Bucketable;loadFromBucketTag(Lnet/minecraft/nbt/CompoundTag;)V")
    )
    private void legacyconnector$restoreLegacyBucketTag(
            Bucketable bucketable,
            CompoundTag modernBucketData,
            Operation<Void> original,
            ServerLevel level,
            ItemStack stack,
            BlockPos pos
    ) {
        original.call(bucketable, ItemStackNbt.getBucketTag(stack, level.registryAccess()));
    }
}
