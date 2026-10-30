package legacyconnector.v1_12_2.net.minecraftforge.client.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.BakedQuad;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.IBakedModel;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemOverrideList;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormat;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.IModelState;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.TRSRTransformation;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidRegistry;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidUtil;

/**
 * Forge's dynamic bucket model: a base sprite, the fluid's still texture cut to the shape of a liquid mask, and a cover sprite on top.
 * Baked with no fluid, the item's stacks show the fluid they hold (each fluid's model baked once).
 */
public final class ModelDynBucket implements IModel {
    public static final IModel MODEL = new ModelDynBucket();

    private static final float NORTH_Z_COVER = 7.496F / 16.0F;
    private static final float SOUTH_Z_COVER = 8.504F / 16.0F;
    private static final float NORTH_Z_FLUID = 7.498F / 16.0F;
    private static final float SOUTH_Z_FLUID = 8.502F / 16.0F;

    private final ResourceLocation baseLocation;
    private final ResourceLocation liquidLocation;
    private final ResourceLocation coverLocation;
    private final Fluid fluid;
    private final boolean flipGas;
    private final boolean tint;

    public ModelDynBucket() {
        this(null, null, null, null, false, true);
    }

    public ModelDynBucket(ResourceLocation baseLocation, ResourceLocation liquidLocation, ResourceLocation coverLocation, Fluid fluid, boolean flipGas) {
        this(baseLocation, liquidLocation, coverLocation, fluid, flipGas, true);
    }

    public ModelDynBucket(ResourceLocation baseLocation, ResourceLocation liquidLocation, ResourceLocation coverLocation, Fluid fluid, boolean flipGas, boolean tint) {
        this.baseLocation = baseLocation;
        this.liquidLocation = liquidLocation;
        this.coverLocation = coverLocation;
        this.fluid = fluid;
        this.flipGas = flipGas;
        this.tint = tint;
    }

    @Override
    public Collection<ResourceLocation> getTextures() {
        ImmutableList.Builder<ResourceLocation> builder = ImmutableList.builder();
        if (baseLocation != null) builder.add(baseLocation);
        if (liquidLocation != null) builder.add(liquidLocation);
        if (coverLocation != null) builder.add(coverLocation);
        if (fluid != null && fluid.getStill() != null) builder.add(fluid.getStill());
        return builder.build();
    }

    @Override
    public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        ImmutableMap<TransformType, TRSRTransformation> transformMap = PerspectiveMapWrapper.getTransforms(state);
        TRSRTransformation transform = state.apply(Optional.empty()).orElse(TRSRTransformation.identity());
        if (flipGas && fluid != null && fluid.isGaseous()) {
            transform = transform.compose(TRSRTransformation.blockCenterToCorner(new TRSRTransformation(null, new javax.vecmath.Quat4f(0, 0, 1, 0), null, null)));
        }
        TextureAtlasSprite fluidSprite = fluid != null && fluid.getStill() != null ? bakedTextureGetter.apply(fluid.getStill()) : null;
        ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
        TextureAtlasSprite particle = fluidSprite;
        if (baseLocation != null) {
            TextureAtlasSprite base = bakedTextureGetter.apply(baseLocation);
            builder.addAll(ItemLayerModel.getQuadsForSprite(-1, base, format, Optional.of(transform)));
            particle = base;
        }
        if (liquidLocation != null && fluidSprite != null) {
            TextureAtlasSprite liquid = bakedTextureGetter.apply(liquidLocation);
            int color = tint ? fluid.getColor() : 0xFFFFFFFF;
            builder.addAll(ItemTextureQuadConverter.convertTexture(format, transform, liquid, fluidSprite, NORTH_Z_FLUID, EnumFacing.NORTH, color, -1));
            builder.addAll(ItemTextureQuadConverter.convertTexture(format, transform, liquid, fluidSprite, SOUTH_Z_FLUID, EnumFacing.SOUTH, color, -1));
            particle = fluidSprite;
        }
        if (coverLocation != null) {
            TextureAtlasSprite cover = bakedTextureGetter.apply(coverLocation);
            builder.add(ItemTextureQuadConverter.genQuad(format, transform, 0, 0, 16, 16, NORTH_Z_COVER, cover, EnumFacing.NORTH, 0xFFFFFFFF, -1));
            builder.add(ItemTextureQuadConverter.genQuad(format, transform, 0, 0, 16, 16, SOUTH_Z_COVER, cover, EnumFacing.SOUTH, 0xFFFFFFFF, -1));
            if (particle == null) particle = cover;
        }
        return new legacyconnector.v1_12_2.net.minecraftforge.client.model.BakedItemModel(builder.build(), particle, transformMap,
                new FluidOverrides(this, state, format, bakedTextureGetter), transform.isIdentity());
    }

    /** {@code "fluid"}, {@code "flipGas"} and {@code "applyTint"} from the blockstate's custom data. */
    @Override
    public ModelDynBucket process(ImmutableMap<String, String> customData) {
        Fluid f = fluid;
        if (customData.containsKey("fluid")) f = FluidRegistry.getFluid(customData.get("fluid").replace("\"", ""));
        boolean flip = customData.containsKey("flipGas") ? Boolean.parseBoolean(customData.get("flipGas")) : flipGas;
        boolean t = customData.containsKey("applyTint") ? Boolean.parseBoolean(customData.get("applyTint")) : tint;
        return new ModelDynBucket(baseLocation, liquidLocation, coverLocation, f, flip, t);
    }

    /** {@code "base"}, {@code "fluid"} and {@code "cover"} sprites (an empty name removes one). */
    @Override
    public ModelDynBucket retexture(ImmutableMap<String, String> textures) {
        ResourceLocation base = baseLocation, liquid = liquidLocation, cover = coverLocation;
        if (textures.containsKey("base")) base = textures.get("base").isEmpty() ? null : new ResourceLocation(textures.get("base"));
        if (textures.containsKey("fluid")) liquid = textures.get("fluid").isEmpty() ? null : new ResourceLocation(textures.get("fluid"));
        if (textures.containsKey("cover")) cover = textures.get("cover").isEmpty() ? null : new ResourceLocation(textures.get("cover"));
        return new ModelDynBucket(base, liquid, cover, fluid, flipGas, tint);
    }

    /** Shows each stack with the fluid it contains: the model of that fluid, baked on first use. */
    private static final class FluidOverrides extends ItemOverrideList {
        private final ModelDynBucket parent;
        private final IModelState state;
        private final VertexFormat format;
        private final Function<ResourceLocation, TextureAtlasSprite> textures;
        private final Map<String, IBakedModel> cache = new java.util.concurrent.ConcurrentHashMap<>();

        FluidOverrides(ModelDynBucket parent, IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> textures) {
            super(ImmutableList.of());
            this.parent = parent;
            this.state = state;
            this.format = format;
            this.textures = textures;
        }

        @Override
        public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, World world, EntityLivingBase entity) {
            FluidStack contained = stack == null ? null : FluidUtil.getFluidContained(stack);
            if (contained == null || contained.getFluid() == null) return originalModel;
            Fluid f = contained.getFluid();
            if (f == parent.fluid) return originalModel;
            return cache.computeIfAbsent(f.getName(), name -> new ModelDynBucket(parent.baseLocation, parent.liquidLocation, parent.coverLocation, f, parent.flipGas,
                    parent.tint).bake(state, format, textures));
        }
    }
}
