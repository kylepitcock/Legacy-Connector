package org.kylepitcock.legacyconnector.v1_7_10.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.InputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.LoggerFactory;

/**
 * The legacy {@code textures/gui/container/creative_inventory/tabs.png}: the tab shapes of the creative screen, 28x32 each, columns 0-6 at
 * x = 28 * column, top tabs unselected at y 0 and selected at y 32, bottom tabs at 64 and 96. Vanilla made them GUI sprites in 1.20.2;
 * mods still draw their own tabs from the sheet (the Galacticraft inventory tab API, shipped by Techguns, drew no tab background), so it is
 * composed once from the modern sprites at the old positions and registered under the old name.
 */
public final class LegacyCreativeTabs {
    public static final ResourceLocation LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/creative_inventory/tabs.png");
    private static boolean done;

    private LegacyCreativeTabs() {}

    /** Registers the sheet the first time anything binds it (render thread). */
    public static void ensure() {
        if (done || !RenderSystem.isOnRenderThread()) return;
        done = true;
        try {
            NativeImage sheet = new NativeImage(NativeImage.Format.RGBA, 256, 256, true);
            String[] rows = {"tab_top_unselected_", "tab_top_selected_", "tab_bottom_unselected_", "tab_bottom_selected_"};
            for (int row = 0; row < rows.length; row++) {
                for (int column = 0; column < 7; column++) put(sheet, "container/creative_inventory/" + rows[row] + (column + 1), column * 28, row * 32);
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
            // the modern tabs are 26 wide, the legacy cells 28: the sprite sits at the cell's left edge, as the modern screen draws it
            image.copyRect(sheet, 0, 0, x, y, Math.min(image.getWidth(), Math.min(28, 256 - x)), Math.min(image.getHeight(), Math.min(32, 256 - y)), false, false);
        } catch (java.io.IOException | RuntimeException e) {
            // a sprite this version of the game does not have leaves its place empty
        }
    }
}
