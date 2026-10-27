package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model;

import java.util.Collections;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

/** 1.12.2 built-in model: no quads; the item is drawn by its tile entity item renderer (chests, banners, shields). */
public class BuiltInModel implements IBakedModel {
    private final ItemCameraTransforms cameraTransforms;
    private final ItemOverrideList overrideList;

    public BuiltInModel(ItemCameraTransforms p_i46537_1_, ItemOverrideList p_i46537_2_) {
        this.cameraTransforms = p_i46537_1_;
        this.overrideList = p_i46537_2_;
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
        return Collections.emptyList();
    }

    @Override
    public boolean isAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return true;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return null;
    }

    @Override
    @Deprecated
    public ItemCameraTransforms getItemCameraTransforms() {
        return cameraTransforms;
    }

    @Override
    public ItemOverrideList getOverrides() {
        return overrideList;
    }
}
