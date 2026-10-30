package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model;

import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

/** 1.12.2's plain baked model: unculled quads, quads culled per side, and the model's flags, particle and display transforms. */
public class SimpleBakedModel implements IBakedModel {
    protected final List<BakedQuad> generalQuads;
    protected final Map<EnumFacing, List<BakedQuad>> faceQuads;
    protected final boolean ambientOcclusion;
    protected final boolean gui3d;
    protected final TextureAtlasSprite texture;
    protected final ItemCameraTransforms cameraTransforms;
    protected final ItemOverrideList itemOverrideList;

    public SimpleBakedModel(List<BakedQuad> generalQuadsIn, Map<EnumFacing, List<BakedQuad>> faceQuadsIn, boolean ambientOcclusionIn, boolean gui3dIn,
            TextureAtlasSprite textureIn, ItemCameraTransforms cameraTransformsIn, ItemOverrideList itemOverrideListIn) {
        this.generalQuads = generalQuadsIn;
        this.faceQuads = faceQuadsIn;
        this.ambientOcclusion = ambientOcclusionIn;
        this.gui3d = gui3dIn;
        this.texture = textureIn;
        this.cameraTransforms = cameraTransformsIn;
        this.itemOverrideList = itemOverrideListIn;
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
        if (side == null) return generalQuads;
        List<BakedQuad> quads = faceQuads.get(side);
        return quads == null ? java.util.Collections.emptyList() : quads;
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
        return texture;
    }

    @Override
    @Deprecated
    public ItemCameraTransforms getItemCameraTransforms() {
        return cameraTransforms;
    }

    @Override
    public ItemOverrideList getOverrides() {
        return itemOverrideList;
    }

    /** Builds a simple model from a json model (its flags and transforms) or from another model retextured (the block breaking overlay). */
    public static class Builder {
        private final List<BakedQuad> builderGeneralQuads = new java.util.ArrayList<>();
        private final Map<EnumFacing, List<BakedQuad>> builderFaceQuads = new java.util.EnumMap<>(EnumFacing.class);
        private final ItemOverrideList builderItemOverrideList;
        private final boolean builderAmbientOcclusion;
        private TextureAtlasSprite builderTexture;
        private final boolean builderGui3d;
        private final ItemCameraTransforms builderCameraTransforms;

        public Builder(ModelBlock model, ItemOverrideList overrides) {
            this(model.isAmbientOcclusion(), model.isGui3d(), model.getAllTransforms(), overrides);
        }

        /** Every quad of {@code model} (at the position's random) with its texture swapped for {@code texture}. */
        public Builder(IBlockState state, IBakedModel model, TextureAtlasSprite texture, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
            this(model.isAmbientOcclusion(), model.isGui3d(), model.getItemCameraTransforms(), model.getOverrides());
            builderTexture = model.getParticleTexture();
            long rand = legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper.getPositionRandom(pos);
            for (EnumFacing facing : EnumFacing.values()) {
                for (BakedQuad quad : model.getQuads(state, facing, rand)) addFaceQuad(facing, new BakedQuadRetextured(quad, texture));
            }
            for (BakedQuad quad : model.getQuads(state, null, rand)) addGeneralQuad(new BakedQuadRetextured(quad, texture));
        }

        private Builder(boolean ambientOcclusion, boolean gui3d, ItemCameraTransforms transforms, ItemOverrideList overrides) {
            for (EnumFacing facing : EnumFacing.values()) builderFaceQuads.put(facing, new java.util.ArrayList<>());
            this.builderItemOverrideList = overrides;
            this.builderAmbientOcclusion = ambientOcclusion;
            this.builderGui3d = gui3d;
            this.builderCameraTransforms = transforms;
        }

        public Builder addFaceQuad(EnumFacing facing, BakedQuad quad) {
            builderFaceQuads.get(facing).add(quad);
            return this;
        }

        public Builder addGeneralQuad(BakedQuad quad) {
            builderGeneralQuads.add(quad);
            return this;
        }

        public Builder setTexture(TextureAtlasSprite texture) {
            builderTexture = texture;
            return this;
        }

        public IBakedModel makeBakedModel() {
            if (builderTexture == null) throw new RuntimeException("Missing particle!");
            return new SimpleBakedModel(builderGeneralQuads, builderFaceQuads, builderAmbientOcclusion, builderGui3d, builderTexture, builderCameraTransforms,
                    builderItemOverrideList);
        }
    }
}
