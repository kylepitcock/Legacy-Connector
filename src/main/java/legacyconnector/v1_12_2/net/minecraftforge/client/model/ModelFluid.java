package legacyconnector.v1_12_2.net.minecraftforge.client.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.function.Function;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.IBakedModel;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemOverrideList;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.SimpleBakedModel;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormat;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.IModelState;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.TRSRTransformation;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidRegistry;

/**
 * Forge's fluid block model ({@code forge:fluid}). The fluid's surface, flow and level are drawn by the modern fluid renderer from the
 * bridged fluid's textures, so the baked model carries no quads: only the still texture as its particle (breaking and landing particles).
 */
public final class ModelFluid implements IModel {
    public static final ModelFluid WATER = new ModelFluid(FluidRegistry.WATER);
    public static final ModelFluid LAVA = new ModelFluid(FluidRegistry.LAVA);

    private final Fluid fluid;

    public ModelFluid(Fluid fluid) {
        this.fluid = fluid;
    }

    @Override
    public Collection<ResourceLocation> getTextures() {
        if (fluid == null) return ImmutableList.of();
        ImmutableList.Builder<ResourceLocation> builder = ImmutableList.builder();
        if (fluid.getStill() != null) builder.add(fluid.getStill());
        if (fluid.getFlowing() != null) builder.add(fluid.getFlowing());
        return builder.build();
    }

    @Override
    public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        TextureAtlasSprite still = fluid != null && fluid.getStill() != null ? bakedTextureGetter.apply(fluid.getStill()) : null;
        if (still == null) still = bakedTextureGetter.apply(new ResourceLocation("missingno"));
        java.util.Map<EnumFacing, java.util.List<legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.BakedQuad>> faces = new java.util.EnumMap<>(EnumFacing.class);
        return new SimpleBakedModel(ImmutableList.of(), faces, true, false, still, ItemCameraTransforms.DEFAULT, ItemOverrideList.NONE);
    }

    @Override
    public IModelState getDefaultState() {
        return TRSRTransformation.identity();
    }

    /** {@code "fluid"} from the blockstate's custom data picks the fluid. */
    @Override
    public ModelFluid process(ImmutableMap<String, String> customData) {
        if (!customData.containsKey("fluid")) return this;
        Fluid f = FluidRegistry.getFluid(customData.get("fluid").replace("\"", ""));
        return f == null ? this : new ModelFluid(f);
    }
}
