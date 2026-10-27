package legacyconnector.v1_12_2.net.minecraftforge.client.model.pipeline;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormat;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

/** A consumer that passes everything on to another one (subclasses change what passes). */
public class VertexTransformer implements IVertexConsumer {
    protected final IVertexConsumer parent;

    public VertexTransformer(IVertexConsumer parent) {
        this.parent = parent;
    }

    @Override
    public VertexFormat getVertexFormat() {
        return parent.getVertexFormat();
    }

    @Override
    public void setQuadTint(int tint) {
        parent.setQuadTint(tint);
    }

    @Override
    public void setTexture(TextureAtlasSprite texture) {
        parent.setTexture(texture);
    }

    @Override
    public void setQuadOrientation(EnumFacing orientation) {
        parent.setQuadOrientation(orientation);
    }

    @Override
    public void setApplyDiffuseLighting(boolean diffuse) {
        parent.setApplyDiffuseLighting(diffuse);
    }

    @Override
    public void put(int element, float... data) {
        parent.put(element, data);
    }
}
