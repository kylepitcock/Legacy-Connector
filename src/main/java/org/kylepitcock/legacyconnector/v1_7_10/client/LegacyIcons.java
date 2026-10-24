package org.kylepitcock.legacyconnector.v1_7_10.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.InputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.LoggerFactory;

/**
 * 1.7.10's {@code textures/gui/icons.png} (crosshair, hearts, armor, air, food, experience bar, jump bar). Vanilla replaced the
 * sheet with separate HUD sprites in 1.20.2, but mods that draw their own health bar or read these icons still blit from it, so
 * it is composed once from the modern sprites at the old positions and registered under the old name.
 */
final class LegacyIcons {
    static final ResourceLocation LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/icons.png");
    private static boolean done;

    private LegacyIcons() {}

    /** Registers the sheet the first time anything binds it (render thread). */
    static void ensure() {
        if (done || !RenderSystem.isOnRenderThread()) return;
        done = true;
        try {
            NativeImage sheet = new NativeImage(NativeImage.Format.RGBA, 256, 256, true);
            // crosshair, health containers and hearts (normal, blinking, poisoned, withered), hardcore row
            put(sheet, "crosshair", 0, 0);
            put(sheet, "heart/container", 16, 0);
            put(sheet, "heart/container_blinking", 25, 0);
            put(sheet, "heart/full", 52, 0);
            put(sheet, "heart/half", 61, 0);
            put(sheet, "heart/full_blinking", 70, 0);
            put(sheet, "heart/half_blinking", 79, 0);
            put(sheet, "heart/poisoned_full", 88, 0);
            put(sheet, "heart/poisoned_half", 97, 0);
            put(sheet, "heart/poisoned_full_blinking", 106, 0);
            put(sheet, "heart/poisoned_half_blinking", 115, 0);
            put(sheet, "heart/withered_full", 124, 0);
            put(sheet, "heart/withered_half", 133, 0);
            put(sheet, "heart/withered_full_blinking", 142, 0);
            put(sheet, "heart/withered_half_blinking", 151, 0);
            put(sheet, "heart/hardcore_full", 52, 45);
            put(sheet, "heart/hardcore_half", 61, 45);
            put(sheet, "heart/container_hardcore", 16, 45);
            // armor, air, food
            put(sheet, "armor_empty", 16, 9);
            put(sheet, "armor_half", 25, 9);
            put(sheet, "armor_full", 34, 9);
            put(sheet, "air", 16, 18);
            put(sheet, "air_bursting", 25, 18);
            put(sheet, "food_empty", 16, 27);
            put(sheet, "food_empty", 25, 27);
            put(sheet, "food_full", 52, 27);
            put(sheet, "food_half", 61, 27);
            put(sheet, "food_empty_hunger", 88, 27);
            put(sheet, "food_full_hunger", 88, 27);
            put(sheet, "food_half_hunger", 97, 27);
            // experience and jump bars
            put(sheet, "experience_bar_background", 0, 64);
            put(sheet, "experience_bar_progress", 0, 69);
            put(sheet, "jump_bar_background", 0, 84);
            put(sheet, "jump_bar_progress", 0, 89);
            Minecraft.getInstance().getTextureManager().register(LOCATION, new DynamicTexture(sheet));
        } catch (RuntimeException e) {
            LoggerFactory.getLogger("LegacyConnector/Hud").warn("Could not compose the legacy icons sheet: {}", e.toString());
        }
    }

    private static void put(NativeImage sheet, String hudSprite, int x, int y) {
        ResourceLocation file = ResourceLocation.withDefaultNamespace("textures/gui/sprites/hud/" + hudSprite + ".png");
        var resource = Minecraft.getInstance().getResourceManager().getResource(file);
        if (resource.isEmpty()) return;
        try (InputStream in = resource.get().open(); NativeImage sprite = NativeImage.read(in)) {
            sprite.copyRect(sheet, 0, 0, x, y, Math.min(sprite.getWidth(), 256 - x), Math.min(sprite.getHeight(), 256 - y), false, false);
        } catch (java.io.IOException | RuntimeException e) {
            // a sprite this version of the game does not have leaves its place empty
        }
    }
}
