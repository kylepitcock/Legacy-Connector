package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormat;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraftforge.client.model.pipeline.IVertexConsumer;

/** A baked face: four vertices in the legacy layout of its format (Forge's default, {@link DefaultVertexFormats#ITEM}). */
public class BakedQuad {
    protected final int[] vertexData;
    protected final int tintIndex;
    protected final EnumFacing face;
    protected final TextureAtlasSprite sprite;
    protected final VertexFormat format;
    protected final boolean applyDiffuseLighting;
    /** The modern quad this one was made from (kept so drawing it again needs no conversion), or null. */
    public net.minecraft.client.renderer.block.model.BakedQuad mcc$modern;

    @Deprecated
    public BakedQuad(int[] vertexDataIn, int tintIndexIn, EnumFacing faceIn, TextureAtlasSprite spriteIn) {
        this(vertexDataIn, tintIndexIn, faceIn, spriteIn, true, DefaultVertexFormats.ITEM);
    }

    public BakedQuad(int[] vertexDataIn, int tintIndexIn, EnumFacing faceIn, TextureAtlasSprite spriteIn, boolean applyDiffuseLighting, VertexFormat format) {
        this.format = format;
        this.applyDiffuseLighting = applyDiffuseLighting;
        vertexData = vertexDataIn;
        tintIndex = tintIndexIn;
        face = faceIn;
        sprite = spriteIn;
    }

    public TextureAtlasSprite getSprite() {
        return sprite;
    }

    public int[] getVertexData() {
        return vertexData;
    }

    public boolean hasTintIndex() {
        return tintIndex != -1;
    }

    public int getTintIndex() {
        return tintIndex;
    }

    public EnumFacing getFace() {
        return face;
    }

    public void pipe(IVertexConsumer consumer) {
        legacyconnector.v1_12_2.net.minecraftforge.client.model.pipeline.LightUtil.putBakedQuad(consumer, this);
    }

    public VertexFormat getFormat() {
        return format;
    }

    public boolean shouldApplyDiffuseLighting() {
        return applyDiffuseLighting;
    }
}
