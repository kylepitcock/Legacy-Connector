package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.kylepitcock.legacyconnector.vb1_7_3.client.OriginalInventoryScreenB173;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Original player inventory presentation, retaining the native screen and native menu. */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenLegacyLayoutMixin {
    @Inject(method="renderBg",at=@At("HEAD"),cancellable=true)
    private void legacyconnector$originalBackground(GuiGraphics graphics,float partial,int mouseX,int mouseY,CallbackInfo callback){
        if(OriginalInventoryScreenB173.render((InventoryScreen)(Object)this,graphics,partial,mouseX,mouseY))callback.cancel();
    }
    @Inject(method="renderLabels",at=@At("HEAD"),cancellable=true)
    private void legacyconnector$originalLabels(GuiGraphics graphics,int mouseX,int mouseY,CallbackInfo callback){
        if(OriginalInventoryScreenB173.hideLabels((InventoryScreen)(Object)this))callback.cancel();
    }
    @Inject(method="hasClickedOutside",at=@At("HEAD"),cancellable=true)
    private void legacyconnector$modernControls(double x,double y,int left,int top,int button,CallbackInfoReturnable<Boolean> callback){
        if(OriginalInventoryScreenB173.controlsContain((InventoryScreen)(Object)this,x,y))callback.setReturnValue(false);
    }
}
