package org.kylepitcock.legacyconnector.v1_7_10.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.InputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.LoggerFactory;

/**
 * 1.7.10's {@code textures/gui/widgets.png} (hotbar, hotbar selection, the three button states). Vanilla split the sheet into GUI
 * sprites in 1.20.2, but legacy buttons, sliders and check boxes ({@code GuiUtils.drawContinuousTexturedBox}) still blit from it,
 * so it is composed once from the modern sprites at the old positions and registered under the old name.
 */
final class LegacyWidgets {
    static final ResourceLocation LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/widgets.png");
    private static boolean done;

    private LegacyWidgets() {}

    /** Registers the sheet the first time anything binds it (render thread). */
    static void ensure() {
        if (done || !RenderSystem.isOnRenderThread()) return;
        done = true;
        try {
            NativeImage sheet = new NativeImage(NativeImage.Format.RGBA, 256, 256, true);
            put(sheet, "hud/hotbar", 0, 0);
            put(sheet, "hud/hotbar_selection", 0, 22);
            put(sheet, "widget/button_disabled", 0, 46);
            put(sheet, "widget/button", 0, 66);
            put(sheet, "widget/button_highlighted", 0, 86);
            Minecraft.getInstance().getTextureManager().register(LOCATION, new DynamicTexture(sheet));
        } catch (RuntimeException e) {
            LoggerFactory.getLogger("LegacyConnector/Gui").warn("Could not compose the legacy widgets sheet: {}", e.toString());
        }
    }

    private static void put(NativeImage sheet, String sprite, int x, int y) {
        ResourceLocation file = ResourceLocation.withDefaultNamespace("textures/gui/sprites/" + sprite + ".png");
        var resource = Minecraft.getInstance().getResourceManager().getResource(file);
        if (resource.isEmpty()) return;
        try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
            image.copyRect(sheet, 0, 0, x, y, Math.min(image.getWidth(), 256 - x), Math.min(image.getHeight(), 256 - y), false, false);
        } catch (java.io.IOException | RuntimeException e) {
            // a sprite this version of the game does not have leaves its place empty
        }
    }
}
