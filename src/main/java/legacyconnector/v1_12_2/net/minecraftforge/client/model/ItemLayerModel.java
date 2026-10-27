package legacyconnector.v1_12_2.net.minecraftforge.client.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.vecmath.Vector4f;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.BakedQuad;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.IBakedModel;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemOverrideList;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormat;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormatElement;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.IModelState;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.TRSRTransformation;

/**
 * Forge 1.12.2 generated item model: one flat layer per texture ("layer0", "layer1"...), baked as a front and a back quad of the sprite
 * plus a one-pixel side face wherever an opaque pixel borders a transparent one (the extruded look of held items). Each layer's quads
 * take its index as tint index.
 */
public final class ItemLayerModel implements IModel {
    public static final ItemLayerModel INSTANCE = new ItemLayerModel(ImmutableList.of());
    private static final float NORTH_Z = 7.5F / 16.0F;
    private static final float SOUTH_Z = 8.5F / 16.0F;
    private final ImmutableList<ResourceLocation> textures;
    private final ItemOverrideList overrides;

    public ItemLayerModel(ImmutableList<ResourceLocation> textures) {
        this(textures, ItemOverrideList.NONE);
    }

    public ItemLayerModel(ImmutableList<ResourceLocation> textures, ItemOverrideList overrides) {
        this.textures = textures;
        this.overrides = overrides;
    }

    @Override
    public Collection<ResourceLocation> getTextures() {
        return textures;
    }

    @Override
    public ItemLayerModel retexture(ImmutableMap<String, String> textures) {
        ImmutableList.Builder<ResourceLocation> builder = ImmutableList.builder();
        for (int i = 0; i < textures.size() + this.textures.size(); i++) {
            if (textures.containsKey("layer" + i)) builder.add(new ResourceLocation(textures.get("layer" + i)));
            else if (i < this.textures.size()) builder.add(this.textures.get(i));
        }
        return new ItemLayerModel(builder.build(), overrides);
    }

    @Override
    public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
        Optional<TRSRTransformation> transform = state.apply(Optional.empty());
        for (int i = 0; i < textures.size(); i++) {
            TextureAtlasSprite sprite = bakedTextureGetter.apply(textures.get(i));
            if (sprite != null) builder.addAll(getQuadsForSprite(i, sprite, format, transform));
        }
        TextureAtlasSprite particle = bakedTextureGetter.apply(textures.isEmpty() ? new ResourceLocation("missingno") : textures.get(0));
        ImmutableMap<TransformType, TRSRTransformation> map = PerspectiveMapWrapper.getTransforms(state);
        return new BakedItemModel(builder.build(), particle, map, overrides, !transform.isPresent() || transform.get().isIdentity());
    }

    public static ImmutableList<BakedQuad> getQuadsForSprite(int tint, TextureAtlasSprite sprite, VertexFormat format, Optional<TRSRTransformation> transform) {
        ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
        int w = Math.max(1, sprite.getIconWidth());
        int h = Math.max(1, sprite.getIconHeight());
        boolean[] opaque = opaque(sprite, w, h);
        // sides: a face per pixel edge between an opaque pixel and a transparent one (or the sprite's border)
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!opaque[y * w + x]) continue;
                if (y == 0 || !opaque[(y - 1) * w + x]) builder.add(side(format, transform, EnumFacing.UP, tint, sprite, x, y, w, h));
                if (y == h - 1 || !opaque[(y + 1) * w + x]) builder.add(side(format, transform, EnumFacing.DOWN, tint, sprite, x, y, w, h));
                if (x == 0 || !opaque[y * w + x - 1]) builder.add(side(format, transform, EnumFacing.WEST, tint, sprite, x, y, w, h));
                if (x == w - 1 || !opaque[y * w + x + 1]) builder.add(side(format, transform, EnumFacing.EAST, tint, sprite, x, y, w, h));
            }
        }
        // front and back
        builder.add(quad(format, transform, EnumFacing.NORTH, sprite, tint, 0, 0, NORTH_Z, sprite.getMinU(), sprite.getMaxV(), 0, 1, NORTH_Z, sprite.getMinU(), sprite.getMinV(),
                1, 1, NORTH_Z, sprite.getMaxU(), sprite.getMinV(), 1, 0, NORTH_Z, sprite.getMaxU(), sprite.getMaxV()));
        builder.add(quad(format, transform, EnumFacing.SOUTH, sprite, tint, 0, 0, SOUTH_Z, sprite.getMinU(), sprite.getMaxV(), 1, 0, SOUTH_Z, sprite.getMaxU(), sprite.getMaxV(),
                1, 1, SOUTH_Z, sprite.getMaxU(), sprite.getMinV(), 0, 1, SOUTH_Z, sprite.getMinU(), sprite.getMinV()));
        return builder.build();
    }

    private static boolean[] opaque(TextureAtlasSprite sprite, int w, int h) {
        boolean[] out = new boolean[w * h];
        int[][] frame = null;
        try {
            frame = sprite.getFrameCount() > 0 ? sprite.getFrameTextureData(0) : null;
        } catch (RuntimeException e) {
            // no pixel data: every pixel counts as opaque
        }
        int[] pixels = frame != null && frame.length > 0 ? frame[0] : null;
        for (int i = 0; i < out.length; i++) out[i] = pixels == null || i >= pixels.length || (pixels[i] >>> 24) != 0;
        return out;
    }

    /** The one-pixel face on the given edge of pixel (x, y), between the front and back planes, showing that pixel. */
    private static BakedQuad side(VertexFormat format, Optional<TRSRTransformation> transform, EnumFacing facing, int tint, TextureAtlasSprite sprite, int x, int y, int w, int h) {
        float x0 = (float) x / w, x1 = (float) (x + 1) / w;
        float y0 = 1.0F - (float) (y + 1) / h, y1 = 1.0F - (float) y / h;
        float u0 = sprite.getInterpolatedU(16.0D * x / w), u1 = sprite.getInterpolatedU(16.0D * (x + 1) / w);
        float v0 = sprite.getInterpolatedV(16.0D * y / h), v1 = sprite.getInterpolatedV(16.0D * (y + 1) / h);
        return switch (facing) {
            case UP -> quad(format, transform, facing, sprite, tint, x0, y1, NORTH_Z, u0, v0, x0, y1, SOUTH_Z, u0, v1, x1, y1, SOUTH_Z, u1, v1, x1, y1, NORTH_Z, u1, v0);
            case DOWN -> quad(format, transform, facing, sprite, tint, x0, y0, SOUTH_Z, u0, v1, x0, y0, NORTH_Z, u0, v0, x1, y0, NORTH_Z, u1, v0, x1, y0, SOUTH_Z, u1, v1);
            case WEST -> quad(format, transform, facing, sprite, tint, x0, y0, NORTH_Z, u0, v1, x0, y0, SOUTH_Z, u1, v1, x0, y1, SOUTH_Z, u1, v0, x0, y1, NORTH_Z, u0, v0);
            default -> quad(format, transform, facing, sprite, tint, x1, y0, SOUTH_Z, u1, v1, x1, y0, NORTH_Z, u0, v1, x1, y1, NORTH_Z, u0, v0, x1, y1, SOUTH_Z, u1, v0);
        };
    }

    static BakedQuad quad(VertexFormat format, Optional<TRSRTransformation> transform, EnumFacing side, TextureAtlasSprite sprite, int tint, float x0, float y0, float z0,
            float u0, float v0, float x1, float y1, float z1, float u1, float v1, float x2, float y2, float z2, float u2, float v2, float x3, float y3, float z3, float u3,
            float v3) {
        UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(format);
        builder.setQuadTint(tint);
        builder.setQuadOrientation(side);
        builder.setTexture(sprite);
        put(builder, format, transform, side, x0, y0, z0, u0, v0);
        put(builder, format, transform, side, x1, y1, z1, u1, v1);
        put(builder, format, transform, side, x2, y2, z2, u2, v2);
        put(builder, format, transform, side, x3, y3, z3, u3, v3);
        return builder.build();
    }

    private static void put(UnpackedBakedQuad.Builder builder, VertexFormat format, Optional<TRSRTransformation> transform, EnumFacing side, float x, float y, float z,
            float u, float v) {
        Vector4f pos = new Vector4f(x, y, z, 1.0F);
        if (transform.isPresent() && !transform.get().isIdentity()) transform.get().transformPosition(pos);
        List<VertexFormatElement> elements = format.getElements();
        for (int e = 0; e < elements.size(); e++) {
            VertexFormatElement el = elements.get(e);
            switch (el.getUsage()) {
                case POSITION -> builder.put(e, pos.x, pos.y, pos.z, 1.0F);
                case COLOR -> builder.put(e, 1.0F, 1.0F, 1.0F, 1.0F);
                case UV -> {
                    if (el.getIndex() == 0) builder.put(e, u, v, 0.0F, 1.0F);
                    else builder.put(e, 0.0F, 0.0F);
                }
                case NORMAL -> builder.put(e, (float) side.getXOffset(), (float) side.getYOffset(), (float) side.getZOffset(), 0.0F);
                default -> builder.put(e);
            }
        }
    }
}
