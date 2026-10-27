package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model;

import java.util.List;
import javax.vecmath.Matrix4f;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A baked model. Models the game made are seen through {@link org.kylepitcock.legacyconnector.v1_12_2.client.Models1122} (their quads
 * converted to the legacy layout); a mod's own implementation that replaces a model in ModelBakeEvent is drawn through a modern model
 * that asks it for quads.
 */
public interface IBakedModel {
    List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand);

    boolean isAmbientOcclusion();

    boolean isGui3d();

    boolean isBuiltInRenderer();

    TextureAtlasSprite getParticleTexture();

    @Deprecated
    default ItemCameraTransforms getItemCameraTransforms() {
        return ItemCameraTransforms.DEFAULT;
    }

    ItemOverrideList getOverrides();

    default boolean isAmbientOcclusion(IBlockState state) {
        return isAmbientOcclusion();
    }

    /** The model to draw for a display type and the matrix to apply (null: none), from the deprecated camera transforms by default. */
    default Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType) {
        return legacyconnector.v1_12_2.net.minecraftforge.client.ForgeHooksClient.handlePerspective(this, cameraTransformType);
    }
}
