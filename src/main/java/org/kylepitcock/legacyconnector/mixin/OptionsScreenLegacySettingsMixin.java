package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Places GuiAPI settings in the native Options grid, including its resize and focus order. */
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenLegacySettingsMixin {
    @Inject(method="init",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;visitWidgets(Ljava/util/function/Consumer;)V"))
    private void legacyconnector$settingsRow(CallbackInfo callback,@Local GridLayout.RowHelper rows){
        if(legacyconnector.vb1_7_3.net.minecraft.src.ModSettingScreen.modscreens.isEmpty())return;
        rows.addChild(Button.builder(Component.translatable("legacyconnector.options.legacy_mod_settings"),
                button->legacyconnector.vb1_7_3.net.minecraft.src.GuiModScreen.showSettings()).width(200).build(),2);
    }
}
