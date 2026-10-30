package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model;

import com.google.common.base.Predicate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

/** 1.12.2 multipart model (fences, walls): the quads of every part whose condition the block state meets; flags come from the first part. */
public class MultipartBakedModel implements IBakedModel {
    private final Map<Predicate<IBlockState>, IBakedModel> selectors;
    protected final boolean ambientOcclusion;
    protected final boolean gui3d;
    protected final TextureAtlasSprite particleTexture;
    protected final ItemCameraTransforms cameraTransforms;
    protected final ItemOverrideList overrides;

    public MultipartBakedModel(Map<Predicate<IBlockState>, IBakedModel> selectorsIn) {
        this.selectors = selectorsIn;
        IBakedModel first = selectorsIn.values().iterator().next();
        this.ambientOcclusion = first.isAmbientOcclusion();
        this.gui3d = first.isGui3d();
        this.particleTexture = first.getParticleTexture();
        this.cameraTransforms = first.getItemCameraTransforms();
        this.overrides = first.getOverrides();
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
        List<BakedQuad> list = new ArrayList<>();
        if (state != null) {
            for (Map.Entry<Predicate<IBlockState>, IBakedModel> e : selectors.entrySet()) {
                if (e.getKey().apply(state)) list.addAll(e.getValue().getQuads(state, side, rand++));
            }
        }
        return list;
    }

    @Override
    public boolean isAmbientOcclusion() {
        return ambientOcclusion;
    }

    @Override
    public boolean isGui3d() {
        return gui3d;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return particleTexture;
    }

    @Override
    @Deprecated
    public ItemCameraTransforms getItemCameraTransforms() {
        return cameraTransforms;
    }

    @Override
    public ItemOverrideList getOverrides() {
        return overrides;
    }

    public static class Builder {
        private final Map<Predicate<IBlockState>, IBakedModel> builderSelectors = new LinkedHashMap<>();

        public void putModel(Predicate<IBlockState> predicate, IBakedModel model) {
            builderSelectors.put(predicate, model);
        }

        public IBakedModel makeMultipartModel() {
            return new MultipartBakedModel(builderSelectors);
        }
    }
}
