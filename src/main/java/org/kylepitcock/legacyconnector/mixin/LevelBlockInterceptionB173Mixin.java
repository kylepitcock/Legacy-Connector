package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.legacyconnector.vb1_7_3.BlockInterceptionB173;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native placement events occur after storage changes and do not cover every world write. */
@Mixin(Level.class)
public abstract class LevelBlockInterceptionB173Mixin {
    @Inject(method="setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",at=@At("HEAD"),cancellable=true)
    private void legacyconnector$originalBlockWrite(BlockPos position,BlockState state,int flags,int recursion,CallbackInfoReturnable<Boolean> callback){
        Level level=(Level)(Object)this;
        if(level.isOutsideBuildHeight(position)||!BlockInterceptionB173.hasInterceptors())return;
        var previous=level.getBlockState(position);
        var replacement=BlockInterceptionB173.replacement(level,position,state);
        if(replacement!=null){
            boolean changed=BlockInterceptionB173.writeReplacement(level,position,replacement,flags,recursion);
            // An interceptor may build the accepted state itself. Native item use still completed that write.
            callback.setReturnValue(changed||(previous!=replacement&&level.getBlockState(position)==replacement));
        }
    }
}
