package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockAdapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * On the client a block change drops the block entity when the block changes ({@code !old.is(newBlock)}); the server asks the old
 * block's {@code onRemove}, which keeps it. A metadata change of a legacy block whose subtypes are flattened (see
 * {@code BlockFlattening1710}) can move the state between two modern blocks of the same legacy block: the tile entity stays, as it
 * did in 1.7.10 and as it does on the server. Only the first {@code is} call (that removal check) is wrapped.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkBlockFamilyMixin {
    @WrapOperation(method = "setBlockState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z", ordinal = 0))
    private boolean legacyconnector$keepFamilyBlockEntity(BlockState state, Block block, Operation<Boolean> original) {
        return original.call(state, block) || LegacyBlockAdapter.sameFamily(state.getBlock(), block);
    }
}
