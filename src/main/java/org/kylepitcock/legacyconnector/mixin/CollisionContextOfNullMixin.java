package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.20.1's {@code CollisionContext.of(@Nullable Entity)} answered {@link CollisionContext#empty()} for a null entity, and {@code ClipContext}'s
 * entity constructor passed its {@code @Nullable} entity straight through to it. 1.21.1 dropped the null check, so any legacy mod that ray-traces
 * without an entity (for example a block entity's own line-of-sight check, issue #350) now dies with an NPE inside {@code EntityCollisionContext}.
 * Restores the old contract: a null entity is the empty context.
 */
@Mixin(CollisionContext.class)
public interface CollisionContextOfNullMixin {
    @Inject(method = "of(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/shapes/CollisionContext;",
            at = @At("HEAD"), cancellable = true, require = 0)
    private static void legacyconnector$nullEntityIsEmpty(Entity entity, CallbackInfoReturnable<CollisionContext> cir) {
        if (entity == null) cir.setReturnValue(CollisionContext.empty());
    }
}
