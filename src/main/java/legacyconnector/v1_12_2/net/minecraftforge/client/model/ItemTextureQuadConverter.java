package legacyconnector.v1_12_2.net.minecraftforge.client.model;

import java.util.ArrayList;
import java.util.List;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormat;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormatElement;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.TRSRTransformation;

/**
 * Forge: turns the visible pixels of a template sprite into flat quads textured with another sprite (the fluid inside a dynamic bucket):
 * runs of visible pixels merged into rectangles, row-wise or column-wise, whichever gives fewer quads.
 */
public final class ItemTextureQuadConverter {
    private ItemTextureQuadConverter() {
    }

    public static List<UnpackedBakedQuad> convertTexture(VertexFormat format, TRSRTransformation transform, TextureAtlasSprite template, TextureAtlasSprite sprite, float z,
            EnumFacing facing, int color) {
        return convertTexture(format, transform, template, sprite, z, facing, color, -1);
    }

    public static List<UnpackedBakedQuad> convertTexture(VertexFormat format, TRSRTransformation transform, TextureAtlasSprite template, TextureAtlasSprite sprite, float z,
            EnumFacing facing, int color, int tint) {
        List<UnpackedBakedQuad> horizontal = convertTextureHorizontal(format, transform, template, sprite, z, facing, color, tint);
        List<UnpackedBakedQuad> vertical = convertTextureVertical(format, transform, template, sprite, z, facing, color, tint);
        return horizontal.size() <= vertical.size() ? horizontal : vertical;
    }

    private static int[] pixels(TextureAtlasSprite template) {
        int[][] frame = template == null ? null : template.getFrameTextureData(0);
        return frame == null || frame.length == 0 ? null : frame[0];
    }

    /** Rows of visible pixels; identical rows below are merged into one taller quad. */
    public static List<UnpackedBakedQuad> convertTextureHorizontal(VertexFormat format, TRSRTransformation transform, TextureAtlasSprite template, TextureAtlasSprite sprite,
            float z, EnumFacing facing, int color, int tint) {
        List<UnpackedBakedQuad> quads = new ArrayList<>();
        int[] data = pixels(template);
        if (data == null) return quads;
        int w = template.getIconWidth(), h = template.getIconHeight();
        if (w <= 0 || h <= 0 || data.length < w * h) return quads;
        float wScale = 16.0F / w, hScale = 16.0F / h;
        int y = 0;
        while (y < h) {
            int endY = y + 1;
            while (endY < h && sameRow(data, w, y, endY)) endY++;
            int start = -1;
            for (int x = 0; x <= w; x++) {
                boolean visible = x < w && isVisible(data[y * w + x]);
                if (start < 0 && visible) start = x;
                else if (start >= 0 && !visible) {
                    quads.add(genQuad(format, transform, start * wScale, y * hScale, x * wScale, endY * hScale, z, sprite, facing, color, tint));
                    start = -1;
                }
            }
            y = endY;
        }
        return quads;
    }

    /** Columns of visible pixels; identical columns to the right are merged into one wider quad. */
    public static List<UnpackedBakedQuad> convertTextureVertical(VertexFormat format, TRSRTransformation transform, TextureAtlasSprite template, TextureAtlasSprite sprite,
            float z, EnumFacing facing, int color, int tint) {
        List<UnpackedBakedQuad> quads = new ArrayList<>();
        int[] data = pixels(template);
        if (data == null) return quads;
        int w = template.getIconWidth(), h = template.getIconHeight();
        if (w <= 0 || h <= 0 || data.length < w * h) return quads;
        float wScale = 16.0F / w, hScale = 16.0F / h;
        int x = 0;
        while (x < w) {
            int endX = x + 1;
            while (endX < w && sameColumn(data, w, h, x, endX)) endX++;
            int start = -1;
            for (int y = 0; y <= h; y++) {
                boolean visible = y < h && isVisible(data[y * w + x]);
                if (start < 0 && visible) start = y;
                else if (start >= 0 && !visible) {
                    quads.add(genQuad(format, transform, x * wScale, start * hScale, endX * wScale, y * hScale, z, sprite, facing, color, tint));
                    start = -1;
                }
            }
            x = endX;
        }
        return quads;
    }

    private static boolean sameRow(int[] data, int w, int a, int b) {
        for (int i = 0; i < w; i++) if (isVisible(data[a * w + i]) != isVisible(data[b * w + i])) return false;
        return true;
    }

    private static boolean sameColumn(int[] data, int w, int h, int a, int b) {
        for (int i = 0; i < h; i++) if (isVisible(data[i * w + a]) != isVisible(data[i * w + b])) return false;
        return true;
    }

    private static boolean isVisible(int color) {
        return (color >> 24 & 255) / 255.0F > 0.1F;
    }

    /** One quad from (x1, y1) to (x2, y2) in sprite pixels (0-16, y down), at depth z, textured with the same area of the sprite. */
    public static UnpackedBakedQuad genQuad(VertexFormat format, TRSRTransformation transform, float x1, float y1, float x2, float y2, float z, TextureAtlasSprite sprite,
            EnumFacing facing, int color, int tint) {
        float u1 = sprite.getInterpolatedU(x1);
        float v1 = sprite.getInterpolatedV(y1);
        float u2 = sprite.getInterpolatedU(x2);
        float v2 = sprite.getInterpolatedV(y2);
        x1 /= 16.0F;
        y1 /= 16.0F;
        x2 /= 16.0F;
        y2 /= 16.0F;
        float tmp = y1;
        y1 = 1.0F - y2;
        y2 = 1.0F - tmp;
        return putQuad(format, transform, facing, sprite, color, tint, x1, y1, x2, y2, z, u1, v1, u2, v2);
    }

    private static UnpackedBakedQuad putQuad(VertexFormat format, TRSRTransformation transform, EnumFacing side, TextureAtlasSprite sprite, int color, int tint, float x1,
            float y1, float x2, float y2, float z, float u1, float v1, float u2, float v2) {
        UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(format);
        builder.setQuadTint(tint);
        builder.setQuadOrientation(side);
        builder.setTexture(sprite);
        if (side == EnumFacing.SOUTH) {
            putVertex(builder, format, transform, side, x1, y1, z, u1, v2, color);
            putVertex(builder, format, transform, side, x2, y1, z, u2, v2, color);
            putVertex(builder, format, transform, side, x2, y2, z, u2, v1, color);
            putVertex(builder, format, transform, side, x1, y2, z, u1, v1, color);
        } else {
            putVertex(builder, format, transform, side, x1, y1, z, u1, v2, color);
            putVertex(builder, format, transform, side, x1, y2, z, u1, v1, color);
            putVertex(builder, format, transform, side, x2, y2, z, u2, v1, color);
            putVertex(builder, format, transform, side, x2, y1, z, u2, v2, color);
        }
        return builder.build();
    }

    private static void putVertex(UnpackedBakedQuad.Builder builder, VertexFormat format, TRSRTransformation transform, EnumFacing side, float x, float y, float z, float u,
            float v, int color) {
        for (int e = 0; e < format.getElementCount(); e++) {
            VertexFormatElement element = format.getElement(e);
            switch (element.getUsage()) {
                case POSITION -> {
                    Vector4f vec = new Vector4f(x, y, z, 1.0F);
                    if (transform != null) transform.transformPosition(vec);
                    builder.put(e, vec.x, vec.y, vec.z, vec.w);
                }
                case COLOR -> builder.put(e, (color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, (color >> 24 & 255) / 255.0F);
                case NORMAL -> {
                    Vector3f normal = new Vector3f(side.getFrontOffsetX(), side.getFrontOffsetY(), side.getFrontOffsetZ());
                    if (transform != null) transform.transformNormal(normal);
                    builder.put(e, normal.x, normal.y, normal.z, 0.0F);
                }
                case UV -> {
                    if (element.getIndex() == 0) builder.put(e, u, v, 0.0F, 1.0F);
                    else builder.put(e);
                }
                default -> builder.put(e);
            }
        }
    }
}
