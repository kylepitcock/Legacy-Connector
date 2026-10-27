package legacyconnector.v1_12_2.net.minecraftforge.client.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.function.Function;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.IBakedModel;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormat;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.IModelState;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.TRSRTransformation;

/**
 * Forge 1.12.2 unbaked model: its dependencies and textures (stitched into the block atlas), and {@link #bake} into a legacy baked
 * model with a state, vertex format and sprite getter. Models from mods' custom loaders bake themselves; see {@link ModelLoaderRegistry}.
 */
public interface IModel {
    default Collection<ResourceLocation> getDependencies() {
        return ImmutableList.of();
    }

    default Collection<ResourceLocation> getTextures() {
        return ImmutableList.of();
    }

    default IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        return ModelLoaderRegistry.mcc$missingBaked();
    }

    default IModelState getDefaultState() {
        return TRSRTransformation.identity();
    }

    default IModel process(ImmutableMap<String, String> customData) {
        return this;
    }

    default IModel smoothLighting(boolean value) {
        return this;
    }

    default IModel gui3d(boolean value) {
        return this;
    }

    default IModel uvlock(boolean value) {
        return this;
    }

    default IModel retexture(ImmutableMap<String, String> textures) {
        return this;
    }
}
