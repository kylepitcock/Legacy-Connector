package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.20.1 block entity constructors took a null state without checking it (mods probe a block entity type with {@code type.create(pos, null)} to learn
 * its class or default values); 1.21.1 validates the state in the constructor and throws a NullPointerException. A null state has nothing to validate.
 */
@Mixin(BlockEntity.class)
public abstract class BlockEntityNullStateMixin {
    @Inject(method = "validateBlockState", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$allowNullState(BlockState state, CallbackInfo ci) {
        if (state == null) ci.cancel();
    }
}
