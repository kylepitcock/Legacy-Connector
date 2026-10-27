package legacyconnector.v1_12_2.net.minecraftforge.client.model.pipeline;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormat;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

/** Forge's quad pipeline: receives a quad element by element (normalized floats, see {@link LightUtil#unpack}). */
public interface IVertexConsumer {
    VertexFormat getVertexFormat();

    void setQuadTint(int tint);

    void setQuadOrientation(EnumFacing orientation);

    void setApplyDiffuseLighting(boolean diffuse);

    void setTexture(TextureAtlasSprite texture);

    void put(int element, float... data);
}
