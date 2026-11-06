package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.kylepitcock.legacyconnector.v1_7_10.compat.am2.Am2Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(MouseHandler.class)
public abstract class Am2MouseMixin {
    @ModifyArgs(method = "turnPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void legacyconnector$orientAm2Mouse(Args args) {
        if (!Am2Camera.reverseMouse(Minecraft.getInstance().player)) return;
        args.set(0, -(double) args.get(0));
        args.set(1, -(double) args.get(1));
    }
}
