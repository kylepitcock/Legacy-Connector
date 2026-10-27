package legacyconnector.v1_12_2.net.minecraftforge.client.model;

import java.util.List;
import javax.vecmath.Matrix4f;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.BakedQuad;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.IBakedModel;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemOverrideList;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import org.apache.commons.lang3.tuple.Pair;

/** Forge 1.12.2: a baked model that hands everything to another (mods override the parts they change). */
public abstract class BakedModelWrapper<T extends IBakedModel> implements IBakedModel {
    protected final T originalModel;

    public BakedModelWrapper(T originalModel) {
        this.originalModel = originalModel;
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
        return originalModel.getQuads(state, side, rand);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return originalModel.isAmbientOcclusion();
    }

    @Override
    public boolean isAmbientOcclusion(IBlockState state) {
        return originalModel.isAmbientOcclusion(state);
    }

    @Override
    public boolean isGui3d() {
        return originalModel.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return originalModel.isBuiltInRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return originalModel.getParticleTexture();
    }

    @Override
    @Deprecated
    public ItemCameraTransforms getItemCameraTransforms() {
        return originalModel.getItemCameraTransforms();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return originalModel.getOverrides();
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType) {
        Pair<? extends IBakedModel, Matrix4f> pair = originalModel.handlePerspective(cameraTransformType);
        // the wrapper stays the model drawn when the original returns itself
        return pair != null && pair.getLeft() == originalModel ? Pair.of(this, pair.getRight()) : pair;
    }
}
