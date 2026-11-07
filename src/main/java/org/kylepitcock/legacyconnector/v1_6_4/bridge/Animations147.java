package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import legacyconnector.v1_6_4.net.minecraft.client.renderer.texturefx.TextureFX;
import legacyconnector.v1_7_10.cpw.mods.fml.common.FMLCommonHandler;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.SubscribeEvent;
import legacyconnector.v1_7_10.cpw.mods.fml.common.gameevent.TickEvent;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/**
 * Minecraft 1.4.7's animated tiles ({@code TextureFX}: a mod's water, lava, fire, portal or machine animation) on the cut sprite sheets. The game called
 * {@code onTick()} every tick and copied {@code imageData} (RGBA) over the tile {@code iconIndex} of the sheet {@code bindImage} bound; here the sheet is found by
 * letting {@code bindImage} run against the shim's {@code RenderEngine} (it records the path), the tile becomes a sprite of the block atlas through
 * {@link SpriteSheets147}, and the pixels are uploaded into that sprite's place in the atlas (all mip levels) on the client thread.
 */
public final class Animations147 {
    private static final System.Logger LOG = System.getLogger("LegacyConnector/Animations147");
    private static final List<Entry> ENTRIES = new CopyOnWriteArrayList<>();
    private static final AtomicBoolean LISTENING = new AtomicBoolean();

    private Animations147() {}

    private static final class Entry {
        final TextureFX fx;
        String sheet;
        ResourceLocation sprite;
        int lastIcon = Integer.MIN_VALUE;
        int errors;
        long frames;

        Entry(TextureFX fx) {
            this.fx = fx;
        }
    }

    public static void register(Object fx) {
        if (!(fx instanceof TextureFX t)) return;
        for (Entry e : ENTRIES) if (e.fx == t) return;
        if (t instanceof legacyconnector.v1_6_4.cpw.mods.fml.client.FMLTextureFX f) {
            try {
                f.setup(); // FML's texture manager did this at registration
            } catch (RuntimeException | LinkageError e) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Textures").warn("Texture animation {} failed its setup: {}", t.getClass().getName(), e.toString());
                return;
            }
        }
        ENTRIES.add(new Entry(t));
        if (LISTENING.compareAndSet(false, true)) FMLCommonHandler.instance().bus().register(new Listener());
    }

    public static void unregister(Object fx) {
        ENTRIES.removeIf(e -> e.fx == fx);
    }

    public static int count() {
        return ENTRIES.size();
    }

    public static final class Listener {
        @SubscribeEvent
        public void onTick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END || ENTRIES.isEmpty()) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null || mc.isPaused()) return;
            for (Entry entry : ENTRIES) {
                try {
                    tick(mc, entry);
                } catch (RuntimeException | LinkageError ex) {
                    if (entry.errors++ < 3) LOG.log(System.Logger.Level.WARNING, "Texture animation " + entry.fx.getClass().getName() + " failed", ex);
                    if (entry.errors > 20) ENTRIES.remove(entry);
                }
            }
        }
    }

    private static void tick(Minecraft mc, Entry e) {
        TextureFX fx = e.fx;
        if (fx.imageData == null) fx.imageData = new byte[1024];
        fx.onTick();
        if (e.sheet == null || e.lastIcon != fx.iconIndex) {
            e.sheet = sheetOf(fx);
            e.lastIcon = fx.iconIndex;
            boolean items = SpriteSheets147.ITEMS.equals(SpriteSheets147.normalize(e.sheet));
            // a block that never set blockIndexInTexture had tile 0 in 1.4.7 (the shim keeps -1 for blocks that register icons)
            String id = SpriteSheets147.spriteId(e.sheet, Math.max(0, fx.iconIndex), !items);
            e.sprite = ResourceLocation.tryParse(id);
        }
        if (e.sprite == null) return;
        TextureAtlas atlas = mc.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
        TextureAtlasSprite sprite = atlas.getSprite(e.sprite);
        if (sprite == null || sprite.contents().name().getPath().equals("missingno")) {
            if (e.frames == 0 && e.errors++ == 0) LOG.log(System.Logger.Level.INFO, "Texture animation " + fx.getClass().getName() + ": tile " + fx.iconIndex + " of " + e.sheet + " has no sprite (" + e.sprite + ")");
            return;
        }
        upload(atlas, sprite, fx.imageData, mc.options.mipmapLevels().get());
        if (e.frames++ == 0) LOG.log(System.Logger.Level.INFO, "Texture animation " + fx.getClass().getName() + " animates " + e.sprite + " (tile " + fx.iconIndex + " of " + e.sheet + ")");
    }

    /** The sheet the tile is on: whatever {@code bindImage} binds or asks the engine for ({@code /terrain.png} when it does neither). */
    private static String sheetOf(TextureFX fx) {
        TextureManager.mcc$lastPath = null;
        try {
            fx.bindImage(TextureManager.INSTANCE);
        } catch (RuntimeException | LinkageError ex) {
            // a mod that binds through OpenGL directly
        }
        String path = TextureManager.mcc$lastPath;
        return path == null ? SpriteSheets147.TERRAIN : path;
    }

    private static void upload(TextureAtlas atlas, TextureAtlasSprite sprite, byte[] rgba, int mipLevels) {
        int size = (int) Math.round(Math.sqrt(rgba.length / 4.0));
        if (size <= 0 || size * size * 4 != rgba.length) return;
        int w = sprite.contents().width();
        int h = sprite.contents().height();
        int[] argb = new int[size * size];
        for (int i = 0; i < argb.length; i++) {
            int r = rgba[i * 4] & 255, g = rgba[i * 4 + 1] & 255, b = rgba[i * 4 + 2] & 255, a = rgba[i * 4 + 3] & 255;
            argb[i] = a << 24 | r << 16 | g << 8 | b;
        }
        if (size != w || size != h) argb = scale(argb, size, size, w, h);
        RenderSystem.assertOnRenderThread();
        atlas.bind();
        int[] level = argb;
        int lw = w, lh = h;
        for (int l = 0; l <= mipLevels && lw > 0 && lh > 0; l++) {
            NativeImage image = new NativeImage(lw, lh, false);
            for (int y = 0; y < lh; y++) {
                for (int x = 0; x < lw; x++) {
                    int c = level[y * lw + x];
                    image.setPixelRGBA(x, y, (c & 0xFF00FF00) | (c >> 16 & 255) | (c & 255) << 16);
                }
            }
            image.upload(l, sprite.getX() >> l, sprite.getY() >> l, 0, 0, lw, lh, false, false, mipLevels > 0, true);
            if (lw == 1 || lh == 1) break;
            level = halve(level, lw, lh);
            lw >>= 1;
            lh >>= 1;
        }
    }

    private static int[] scale(int[] src, int sw, int sh, int dw, int dh) {
        int[] out = new int[dw * dh];
        for (int y = 0; y < dh; y++) for (int x = 0; x < dw; x++) out[y * dw + x] = src[(y * sh / dh) * sw + (x * sw / dw)];
        return out;
    }

    /** A 2x2 box filter on straight alpha (the mip levels of an animated tile). */
    private static int[] halve(int[] src, int w, int h) {
        int nw = w >> 1, nh = h >> 1;
        int[] out = new int[nw * nh];
        for (int y = 0; y < nh; y++) {
            for (int x = 0; x < nw; x++) {
                int a = 0, r = 0, g = 0, b = 0;
                for (int dy = 0; dy < 2; dy++) {
                    for (int dx = 0; dx < 2; dx++) {
                        int c = src[(y * 2 + dy) * w + x * 2 + dx];
                        a += c >>> 24;
                        r += c >> 16 & 255;
                        g += c >> 8 & 255;
                        b += c & 255;
                    }
                }
                out[y * nw + x] = (a / 4) << 24 | (r / 4) << 16 | (g / 4) << 8 | (b / 4);
            }
        }
        return out;
    }
}
