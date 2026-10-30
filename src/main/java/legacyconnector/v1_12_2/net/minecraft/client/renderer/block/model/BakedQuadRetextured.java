package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;

/** 1.12.2: a copy of a quad drawn with another sprite (each vertex's UV moved from the old sprite's area to the same spot of the new one). */
public class BakedQuadRetextured extends BakedQuad {
    private final TextureAtlasSprite texture;

    public BakedQuadRetextured(BakedQuad quad, TextureAtlasSprite textureIn) {
        super(java.util.Arrays.copyOf(quad.getVertexData(), quad.getVertexData().length), quad.tintIndex, quad.getFace() != null ? quad.getFace()
                : FaceBakery.getFacingFromVertexData(quad.getVertexData()), quad.getSprite(), quad.applyDiffuseLighting, quad.format);
        this.texture = textureIn;
        remapQuad();
    }

    private void remapQuad() {
        if (sprite == null || texture == null || format == null) return;
        int stride = format.getIntegerSize();
        int uv = format.getUvOffsetById(0) / 4;
        if (stride <= 0 || uv < 0) return;
        for (int i = 0; i < 4; ++i) {
            int j = stride * i + uv;
            if (j + 1 >= vertexData.length) break;
            vertexData[j] = Float.floatToRawIntBits(texture.getInterpolatedU(sprite.getUnInterpolatedU(Float.intBitsToFloat(vertexData[j]))));
            vertexData[j + 1] = Float.floatToRawIntBits(texture.getInterpolatedV(sprite.getUnInterpolatedV(Float.intBitsToFloat(vertexData[j + 1]))));
        }
    }

    @Override
    public TextureAtlasSprite getSprite() {
        return texture;
    }
}
