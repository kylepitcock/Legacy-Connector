package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureMap;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.org.lwjgl.util.vector.Vector3f;

/**
 * 1.12.2's generated item model (item/generated): per texture layer a flat front and back plus one thin side face along every edge
 * between transparent and opaque pixels of the sprite (read from the sprite's frames).
 */
public class ItemModelGenerator {
    public static final List<String> LAYERS = new ArrayList<>(List.of("layer0", "layer1", "layer2", "layer3", "layer4"));

    public ItemModelGenerator() {
    }

    public ModelBlock makeItemModel(TextureMap textureMapIn, ModelBlock blockModel) {
        Map<String, String> textures = new HashMap<>();
        List<BlockPart> parts = new ArrayList<>();
        for (int i = 0; i < LAYERS.size(); ++i) {
            String layer = LAYERS.get(i);
            if (!blockModel.isTexturePresent(layer)) break;
            String name = blockModel.resolveTextureName(layer);
            textures.put(layer, name);
            TextureAtlasSprite sprite = textureMapIn.getAtlasSprite(new ResourceLocation(name).toString());
            parts.addAll(getBlockParts(i, layer, sprite));
        }
        if (parts.isEmpty()) return null;
        textures.put("particle", blockModel.isTexturePresent("particle") ? blockModel.resolveTextureName("particle") : textures.get("layer0"));
        return new ModelBlock(null, parts, textures, false, false, blockModel.getAllTransforms(), blockModel.getOverrides());
    }

    public List<BlockPart> getBlockParts(int tintIndex, String textureKey, TextureAtlasSprite sprite) {
        Map<EnumFacing, BlockPartFace> faces = new HashMap<>();
        faces.put(EnumFacing.SOUTH, new BlockPartFace(null, tintIndex, textureKey, new BlockFaceUV(new float[] {0.0F, 0.0F, 16.0F, 16.0F}, 0)));
        faces.put(EnumFacing.NORTH, new BlockPartFace(null, tintIndex, textureKey, new BlockFaceUV(new float[] {16.0F, 0.0F, 0.0F, 16.0F}, 0)));
        List<BlockPart> parts = new ArrayList<>();
        parts.add(new BlockPart(new Vector3f(0.0F, 0.0F, 7.5F), new Vector3f(16.0F, 16.0F, 8.5F), faces, null, true));
        if (sprite != null) parts.addAll(getBlockParts(sprite, textureKey, tintIndex));
        return parts;
    }

    private List<BlockPart> getBlockParts(TextureAtlasSprite sprite, String textureKey, int tintIndex) {
        float width = (float) sprite.getIconWidth();
        float height = (float) sprite.getIconHeight();
        List<BlockPart> parts = new ArrayList<>();
        for (Span span : getSpans(sprite)) {
            float f2 = 0.0F, f3 = 0.0F, f4 = 0.0F, f5 = 0.0F, f6 = 0.0F, f7 = 0.0F, f8 = 0.0F, f9 = 0.0F, f10 = 0.0F, f11 = 0.0F;
            float min = (float) span.getMin();
            float max = (float) span.getMax();
            float anchor = (float) span.getAnchor();
            SpanFacing facing = span.getFacing();
            switch (facing) {
                case UP -> {
                    f6 = min;
                    f2 = min;
                    f4 = f7 = max + 1.0F;
                    f8 = anchor;
                    f3 = anchor;
                    f9 = anchor;
                    f5 = anchor;
                    f10 = 16.0F / width;
                    f11 = 16.0F / (height - 1.0F);
                }
                case DOWN -> {
                    f9 = anchor;
                    f8 = anchor;
                    f6 = min;
                    f2 = min;
                    f4 = f7 = max + 1.0F;
                    f3 = anchor + 1.0F;
                    f5 = anchor + 1.0F;
                    f10 = 16.0F / width;
                    f11 = 16.0F / (height - 1.0F);
                }
                case LEFT -> {
                    f6 = anchor;
                    f2 = anchor;
                    f4 = anchor;
                    f7 = anchor + 1.0F;
                    f9 = min;
                    f3 = min;
                    f5 = f8 = max + 1.0F;
                    f10 = 16.0F / (width - 1.0F);
                    f11 = 16.0F / height;
                }
                case RIGHT -> {
                    f6 = anchor;
                    f7 = anchor + 1.0F;
                    f2 = anchor + 1.0F;
                    f4 = anchor + 1.0F;
                    f9 = min;
                    f3 = min;
                    f5 = f8 = max + 1.0F;
                    f10 = 16.0F / (width - 1.0F);
                    f11 = 16.0F / height;
                }
            }
            float sx = 16.0F / width;
            float sy = 16.0F / height;
            f2 *= sx;
            f4 *= sx;
            f3 *= sy;
            f5 *= sy;
            f3 = 16.0F - f3;
            f5 = 16.0F - f5;
            f6 *= f10;
            f7 *= f10;
            f8 *= f11;
            f9 *= f11;
            Map<EnumFacing, BlockPartFace> faces = new HashMap<>();
            faces.put(facing.getFacing(), new BlockPartFace(null, tintIndex, textureKey, new BlockFaceUV(new float[] {f6, f8, f7, f9}, 0)));
            switch (facing) {
                case UP -> parts.add(new BlockPart(new Vector3f(f2, f3, 7.5F), new Vector3f(f4, f3, 8.5F), faces, null, true));
                case DOWN -> parts.add(new BlockPart(new Vector3f(f2, f5, 7.5F), new Vector3f(f4, f5, 8.5F), faces, null, true));
                case LEFT -> parts.add(new BlockPart(new Vector3f(f2, f3, 7.5F), new Vector3f(f2, f5, 8.5F), faces, null, true));
                case RIGHT -> parts.add(new BlockPart(new Vector3f(f4, f3, 7.5F), new Vector3f(f4, f5, 8.5F), faces, null, true));
            }
        }
        return parts;
    }

    private List<Span> getSpans(TextureAtlasSprite sprite) {
        int w = sprite.getIconWidth();
        int h = sprite.getIconHeight();
        List<Span> spans = new ArrayList<>();
        for (int k = 0; k < sprite.getFrameCount(); ++k) {
            int[][] frame = sprite.getFrameTextureData(k);
            if (frame == null || frame.length == 0 || frame[0] == null) continue;
            int[] pixels = frame[0];
            for (int y = 0; y < h; ++y) {
                for (int x = 0; x < w; ++x) {
                    boolean opaque = !isTransparent(pixels, x, y, w, h);
                    checkTransition(SpanFacing.UP, spans, pixels, x, y, w, h, opaque);
                    checkTransition(SpanFacing.DOWN, spans, pixels, x, y, w, h, opaque);
                    checkTransition(SpanFacing.LEFT, spans, pixels, x, y, w, h, opaque);
                    checkTransition(SpanFacing.RIGHT, spans, pixels, x, y, w, h, opaque);
                }
            }
        }
        return spans;
    }

    private void checkTransition(SpanFacing facing, List<Span> spans, int[] pixels, int x, int y, int w, int h, boolean opaque) {
        if (opaque && isTransparent(pixels, x + facing.getXOffset(), y + facing.getYOffset(), w, h)) buildSpan(spans, facing, x, y);
    }

    private void buildSpan(List<Span> spans, SpanFacing facing, int x, int y) {
        Span found = null;
        int anchor = facing.isHorizontal() ? y : x;
        for (Span span : spans) {
            if (span.getFacing() == facing && span.getAnchor() == anchor) {
                found = span;
                break;
            }
        }
        int along = facing.isHorizontal() ? x : y;
        if (found == null) spans.add(new Span(facing, along, anchor));
        else found.expand(along);
    }

    private boolean isTransparent(int[] pixels, int x, int y, int w, int h) {
        if (x >= 0 && y >= 0 && x < w && y < h && y * w + x < pixels.length) return (pixels[y * w + x] >> 24 & 255) == 0;
        return true;
    }

    static class Span {
        private final SpanFacing spanFacing;
        private int min;
        private int max;
        private final int anchor;

        Span(SpanFacing spanFacingIn, int minIn, int anchorIn) {
            spanFacing = spanFacingIn;
            min = minIn;
            max = minIn;
            anchor = anchorIn;
        }

        void expand(int pos) {
            if (pos < min) min = pos;
            else if (pos > max) max = pos;
        }

        SpanFacing getFacing() {
            return spanFacing;
        }

        int getMin() {
            return min;
        }

        int getMax() {
            return max;
        }

        int getAnchor() {
            return anchor;
        }
    }

    enum SpanFacing {
        UP(EnumFacing.UP, 0, -1),
        DOWN(EnumFacing.DOWN, 0, 1),
        LEFT(EnumFacing.EAST, -1, 0),
        RIGHT(EnumFacing.WEST, 1, 0);

        private final EnumFacing facing;
        private final int xOffset;
        private final int yOffset;

        SpanFacing(EnumFacing facing, int xOffset, int yOffset) {
            this.facing = facing;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
        }

        EnumFacing getFacing() {
            return facing;
        }

        int getXOffset() {
            return xOffset;
        }

        int getYOffset() {
            return yOffset;
        }

        boolean isHorizontal() {
            return this == DOWN || this == UP;
        }
    }
}
