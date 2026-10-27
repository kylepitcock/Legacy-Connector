package legacyconnector.v1_12_2.net.minecraftforge.client.model;

import com.google.common.collect.ImmutableMap;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import javax.vecmath.Matrix4f;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.BakedQuad;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.IBakedModel;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemOverrideList;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.IModelState;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.TRSRTransformation;
import org.apache.commons.lang3.tuple.Pair;

/** Forge 1.12.2: a baked model with a transform per display type (the static helpers turn model states into such maps). */
public class PerspectiveMapWrapper implements IBakedModel {
    private final IBakedModel parent;
    private final ImmutableMap<TransformType, TRSRTransformation> transforms;

    public PerspectiveMapWrapper(IBakedModel parent, ImmutableMap<TransformType, TRSRTransformation> transforms) {
        this.parent = parent;
        this.transforms = transforms;
    }

    public PerspectiveMapWrapper(IBakedModel parent, IModelState state) {
        this(parent, getTransforms(state));
    }

    public static ImmutableMap<TransformType, TRSRTransformation> getTransforms(IModelState state) {
        EnumMap<TransformType, TRSRTransformation> map = new EnumMap<>(TransformType.class);
        for (TransformType type : TransformType.values()) {
            Optional<TRSRTransformation> tr = state.apply(Optional.of(type));
            if (tr.isPresent()) map.put(type, tr.get());
        }
        return ImmutableMap.copyOf(map);
    }

    @SuppressWarnings("deprecation")
    public static ImmutableMap<TransformType, TRSRTransformation> getTransforms(ItemCameraTransforms transforms) {
        EnumMap<TransformType, TRSRTransformation> map = new EnumMap<>(TransformType.class);
        for (TransformType type : TransformType.values()) {
            if (transforms.hasCustomTransform(type)) map.put(type, TRSRTransformation.blockCenterToCorner(TRSRTransformation.from(transforms.getTransform(type))));
        }
        return ImmutableMap.copyOf(map);
    }

    public static Pair<? extends IBakedModel, Matrix4f> handlePerspective(IBakedModel model, ImmutableMap<TransformType, TRSRTransformation> transforms, TransformType cameraTransformType) {
        TRSRTransformation tr = transforms.get(cameraTransformType);
        if (tr != null && !tr.isIdentity()) return Pair.of(model, TRSRTransformation.blockCornerToCenter(tr).getMatrix());
        return Pair.of(model, null);
    }

    public static Pair<? extends IBakedModel, Matrix4f> handlePerspective(IBakedModel model, IModelState state, TransformType cameraTransformType) {
        TRSRTransformation tr = state.apply(Optional.of(cameraTransformType)).orElse(TRSRTransformation.identity());
        if (!tr.isIdentity()) return Pair.of(model, TRSRTransformation.blockCornerToCenter(tr).getMatrix());
        return Pair.of(model, null);
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
        return parent.getQuads(state, side, rand);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return parent.isAmbientOcclusion();
    }

    @Override
    public boolean isAmbientOcclusion(IBlockState state) {
        return parent.isAmbientOcclusion(state);
    }

    @Override
    public boolean isGui3d() {
        return parent.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return parent.isBuiltInRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return parent.getParticleTexture();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return parent.getOverrides();
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType cameraTransformType) {
        return handlePerspective(this, transforms, cameraTransformType);
    }
}
