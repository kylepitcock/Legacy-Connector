package legacyconnector.v1_12_2.net.minecraftforge.client.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import javax.vecmath.Matrix4f;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.BakedQuad;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.IBakedModel;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemOverrideList;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.TRSRTransformation;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Forge 1.12.2 baked generated item model: unculled quads, the display transforms per perspective and the item's overrides. In the GUI
 * an untransformed model shows only its front faces (the flat icon), as Forge's GUI variant did.
 */
public class BakedItemModel implements IBakedModel {
    protected final ImmutableList<BakedQuad> quads;
    protected final TextureAtlasSprite particle;
    protected final ImmutableMap<TransformType, TRSRTransformation> transforms;
    protected final ItemOverrideList overrides;
    protected final IBakedModel guiModel;

    public BakedItemModel(ImmutableList<BakedQuad> quads, TextureAtlasSprite particle, ImmutableMap<TransformType, TRSRTransformation> transforms, ItemOverrideList overrides,
            boolean untransformed) {
        this.quads = quads;
        this.particle = particle;
        this.transforms = transforms;
        this.overrides = overrides;
        this.guiModel = untransformed && hasGuiIdentity(transforms) ? new BakedGuiItemModel(this) : null;
    }

    private static boolean hasGuiIdentity(ImmutableMap<TransformType, TRSRTransformation> transforms) {
        TRSRTransformation guiTransform = transforms.get(TransformType.GUI);
        return guiTransform == null || guiTransform.isIdentity();
    }

    @Override
    public boolean isAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return particle;
    }

    @Override
    public ItemOverrideList getOverrides() {
        return overrides;
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
        return side == null ? quads : ImmutableList.of();
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType type) {
        if (type == TransformType.GUI && guiModel != null) return guiModel.handlePerspective(type);
        return PerspectiveMapWrapper.handlePerspective(this, transforms, type);
    }

    /** The GUI variant: the south-facing quads only. */
    public static class BakedGuiItemModel extends BakedModelWrapper<BakedItemModel> {
        private final ImmutableList<BakedQuad> quads;

        public BakedGuiItemModel(BakedItemModel originalModel) {
            super(originalModel);
            ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
            for (BakedQuad quad : originalModel.quads) if (quad.getFace() == EnumFacing.SOUTH) builder.add(quad);
            this.quads = builder.build();
        }

        @Override
        public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
            return side == null ? quads : ImmutableList.of();
        }

        @Override
        public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType type) {
            if (type == TransformType.GUI) return PerspectiveMapWrapper.handlePerspective(this, originalModel.transforms, type);
            return originalModel.handlePerspective(type);
        }
    }
}
