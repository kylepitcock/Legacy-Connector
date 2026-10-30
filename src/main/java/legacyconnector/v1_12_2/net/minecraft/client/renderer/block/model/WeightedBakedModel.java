package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

/** 1.12.2 weighted model (a blockstate variant list): each position draws one of its models, picked by weight from the position's random. */
public class WeightedBakedModel implements IBakedModel {
    private final int totalWeight;
    private final List<WeightedModel> models;
    public final IBakedModel baseModel;

    public WeightedBakedModel(List<WeightedModel> modelsIn) {
        this.models = modelsIn;
        int total = 0;
        for (WeightedModel m : modelsIn) total += m.itemWeight;
        this.totalWeight = total;
        this.baseModel = modelsIn.get(0).model;
    }

    /** The model a random value picks (its upper bits by weight). */
    public IBakedModel getRandomModel(long seed) {
        if (totalWeight <= 0) return baseModel;
        int pick = Math.abs((int) seed >> 16) % totalWeight;
        for (WeightedModel m : models) {
            pick -= m.itemWeight;
            if (pick < 0) return m.model;
        }
        return baseModel;
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
        return getRandomModel(rand).getQuads(state, side, rand);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return baseModel.isAmbientOcclusion();
    }

    @Override
    public boolean isAmbientOcclusion(IBlockState state) {
        return baseModel.isAmbientOcclusion(state);
    }

    @Override
    public boolean isGui3d() {
        return baseModel.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return baseModel.isBuiltInRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return baseModel.getParticleTexture();
    }

    @Override
    @Deprecated
    public ItemCameraTransforms getItemCameraTransforms() {
        return baseModel.getItemCameraTransforms();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return baseModel.getOverrides();
    }

    @Override
    public org.apache.commons.lang3.tuple.Pair<? extends IBakedModel, javax.vecmath.Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType) {
        var pair = baseModel.handlePerspective(cameraTransformType);
        return org.apache.commons.lang3.tuple.Pair.of(this, pair == null ? null : pair.getRight());
    }

    public static class Builder {
        private final List<WeightedModel> listItems = new ArrayList<>();

        public Builder add(IBakedModel model, int weight) {
            listItems.add(new WeightedModel(model, weight));
            return this;
        }

        public WeightedBakedModel build() {
            java.util.Collections.sort(listItems);
            return new WeightedBakedModel(listItems);
        }

        public IBakedModel first() {
            return listItems.get(0).model;
        }
    }

    static class WeightedModel implements Comparable<WeightedModel> {
        protected final IBakedModel model;
        protected final int itemWeight;

        WeightedModel(IBakedModel modelIn, int itemWeightIn) {
            this.model = modelIn;
            this.itemWeight = itemWeightIn;
        }

        @Override
        public int compareTo(WeightedModel other) {
            return Integer.compare(other.itemWeight, itemWeight);
        }
    }
}
