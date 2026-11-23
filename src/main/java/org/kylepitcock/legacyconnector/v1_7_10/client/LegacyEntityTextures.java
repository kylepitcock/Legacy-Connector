package org.kylepitcock.legacyconnector.v1_7_10.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.InputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.LoggerFactory;

/**
 * Vanilla entity textures legacy renderers bind by path that 1.16 replaced by a texture of another layout. {@code textures/entity/zombie_pigman.png}
 * was a biped skin; the zombified piglin's texture has the same body, arms and legs, but a 10 wide head and the snout as a box of its own. Legacy
 * renderers draw a biped model with it (Techguns' zombie pigman soldier), so the old sheet is composed once from the modern one: the head cropped
 * to 8 wide with the snout painted on the face, the hat layer (the piglin's ears and snout) left empty. 1.12's sheet is 64x64, 1.7.10's 64x32.
 */
public final class LegacyEntityTextures {
    private static final ResourceLocation PIGLIN = ResourceLocation.withDefaultNamespace("textures/entity/piglin/zombified_piglin.png");
    /** The composed sheets (in the vanilla namespace, so the location is never cached as a plain mod texture). */
    public static final ResourceLocation PIGMAN_64 = ResourceLocation.withDefaultNamespace("textures/entity/legacy/zombie_pigman_64.png");
    public static final ResourceLocation PIGMAN_32 = ResourceLocation.withDefaultNamespace("textures/entity/legacy/zombie_pigman_32.png");
    private static boolean done64;
    private static boolean done32;

    private LegacyEntityTextures() {}

    /** The legacy zombie pigman sheet of the given height (64 for 1.12, 32 for 1.7.10), composed the first time it is asked for on the render thread. */
    public static ResourceLocation pigman(int height) {
        boolean tall = height >= 64;
        ResourceLocation location = tall ? PIGMAN_64 : PIGMAN_32;
        if ((tall ? done64 : done32) || !RenderSystem.isOnRenderThread()) return location;
        if (tall) done64 = true;
        else done32 = true;
        var resource = Minecraft.getInstance().getResourceManager().getResource(PIGLIN);
        if (resource.isEmpty()) return location;
        try (InputStream in = resource.get().open(); NativeImage piglin = NativeImage.read(in)) {
            int s = piglin.getWidth() / 64; // a resource pack's larger texture keeps its scale
            NativeImage sheet = new NativeImage(NativeImage.Format.RGBA, 64 * s, (tall ? 64 : 32) * s, true);
            // body, arms and legs (rows 16-32 on both layouts; 1.12's lower half holds nothing a biped reads but keeps the second layers)
            copy(piglin, sheet, 0, 16, 0, 16, 64, (tall ? 64 : 32) - 16, s);
            // the head: piglin box 10x8x8 at (0, 0), biped 8x8x8 at (0, 0); the 10 wide faces lose a column on each side
            copy(piglin, sheet, 9, 0, 8, 0, 8, 8, s);   // top
            copy(piglin, sheet, 19, 0, 16, 0, 8, 8, s); // bottom
            copy(piglin, sheet, 0, 8, 0, 8, 8, 8, s);   // right
            copy(piglin, sheet, 9, 8, 8, 8, 8, 8, s);   // front
            copy(piglin, sheet, 18, 8, 16, 8, 8, 8, s); // left
            copy(piglin, sheet, 27, 8, 24, 8, 8, 8, s); // back
            // the snout's front face (box 4x4x1 at (31, 1)) on the lower middle of the face
            copy(piglin, sheet, 32, 2, 10, 12, 4, 4, s);
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(sheet));
        } catch (java.io.IOException | RuntimeException e) {
            LoggerFactory.getLogger("LegacyConnector/Textures").warn("Could not compose the legacy zombie pigman texture: {}", e.toString());
        }
        return location;
    }

    private static void copy(NativeImage from, NativeImage to, int fx, int fy, int tx, int ty, int w, int h, int s) {
        for (int y = 0; y < h * s; y++) {
            for (int x = 0; x < w * s; x++) {
                int sx = fx * s + x, sy = fy * s + y, dx = tx * s + x, dy = ty * s + y;
                if (sx < from.getWidth() && sy < from.getHeight() && dx < to.getWidth() && dy < to.getHeight()) to.setPixelRGBA(dx, dy, from.getPixelRGBA(sx, sy));
            }
        }
    }
}
