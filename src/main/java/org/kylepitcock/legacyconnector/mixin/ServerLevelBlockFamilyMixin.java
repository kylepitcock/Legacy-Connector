package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockAdapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Scheduled ticks and block events are queued for a block and run only while that block is still there. A legacy block whose
 * subtypes are flattened lives in several modern blocks (see {@code BlockFlattening1710}); legacy code schedules for the legacy
 * block, and a metadata change can move the state to another of them before the tick runs. 1.7.10 compared the legacy block
 * ({@code Block.isEqualTo}), so any block of the same legacy block counts. Other blocks are unaffected.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelBlockFamilyMixin {
    @WrapOperation(method = "tickBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    private boolean legacyconnector$tickFamily(BlockState state, Block block, Operation<Boolean> original) {
        return original.call(state, block) || LegacyBlockAdapter.sameFamily(state.getBlock(), block);
    }

    @WrapOperation(method = "doBlockEvent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    private boolean legacyconnector$eventFamily(BlockState state, Block block, Operation<Boolean> original) {
        return original.call(state, block) || LegacyBlockAdapter.sameFamily(state.getBlock(), block);
    }
}
