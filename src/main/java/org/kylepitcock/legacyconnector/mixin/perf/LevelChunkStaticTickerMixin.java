package org.kylepitcock.legacyconnector.mixin.perf;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.kylepitcock.legacyconnector.core.perf.StaticTickers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * perf.staticTileEntities: a legacy tile entity host whose tile entity never ticks (see {@link StaticTickers}) gets no ticker, as
 * 1.7.10 and 1.12.2 never put such tile entities in their ticking list. Only Legacy Connector's own host block entities are
 * affected; every other block entity gets the ticker its block returns. Wrapped (not redirected), so other mods' changes to this
 * call (Lithium's block entity sleeping and the like) still apply.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkStaticTickerMixin {
    @WrapOperation(method = "updateBlockEntityTicker", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getTicker(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/BlockEntityType;)Lnet/minecraft/world/level/block/entity/BlockEntityTicker;"))
    private BlockEntityTicker<?> legacyconnector$noStaticTicker(BlockState state, Level level, BlockEntityType<?> type, Operation<BlockEntityTicker<?>> original,
            @Local(argsOnly = true) BlockEntity blockEntity) {
        if (StaticTickers.noTicker(blockEntity)) return null;
        return original.call(state, level, type);
    }
}
