package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.kylepitcock.legacyconnector.vb1_7_3.PlayerClassOverridesB173;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A Beta mod's own subclass of the client player overrides {@code moveEntityWithHeading}. NeoForge has no event or hook that replaces a
 * living entity's travel (the jump, fall and tick of the same subclass go through the native events), so the call is taken here; without
 * such a subclass it does nothing.
 */
@Mixin(LivingEntity.class)
public abstract class PlayerClassTravelB173Mixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void legacyconnector$betaPlayerClassTravel(Vec3 travel, CallbackInfo ci) {
        if ((Object) this instanceof Player player && PlayerClassOverridesB173.travel(player, travel)) ci.cancel();
    }
}
