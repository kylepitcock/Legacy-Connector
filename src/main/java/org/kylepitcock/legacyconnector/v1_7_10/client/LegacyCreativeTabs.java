package org.kylepitcock.legacyconnector.v1_7_10.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.InputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.LoggerFactory;

/**
 * 1.7.10's {@code textures/gui/container/creative_inventory/tabs.png}, the sheet inventory-tab mods (Tinkers' Construct's TabRegistry,
 * Galacticraft's tabs, Thaumcraft's) blit their tabs from: seven 28x32 columns of top tabs (row 0 unselected, row 32 selected) and bottom
 * tabs (rows 64 and 96). Vanilla split it into sprites in 1.20.2, so it is composed once from those sprites at the old positions.
 */
final class LegacyCreativeTabs {
    static final ResourceLocation LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/creative_inventory/tabs.png");
    private static boolean done;

    private LegacyCreativeTabs() {}

    /** Registers the sheet the first time anything binds it (render thread). */
    static void ensure() {
        if (done || !RenderSystem.isOnRenderThread()) return;
        done = true;
        try {
            NativeImage sheet = new NativeImage(NativeImage.Format.RGBA, 256, 256, true);
            String[] rows = {"tab_top_unselected_", "tab_top_selected_", "tab_bottom_unselected_", "tab_bottom_selected_"};
            for (int row = 0; row < rows.length; row++) {
                for (int col = 0; col < 7; col++) put(sheet, "container/creative_inventory/" + rows[row] + (col + 1), col * 28, row * 32);
            }
            Minecraft.getInstance().getTextureManager().register(LOCATION, new DynamicTexture(sheet));
        } catch (RuntimeException e) {
            LoggerFactory.getLogger("LegacyConnector/Gui").warn("Could not compose the legacy creative tabs sheet: {}", e.toString());
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
