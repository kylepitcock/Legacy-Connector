package org.kylepitcock.legacyconnector.v1_12_2.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.vecmath.Matrix4f;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.BakedQuad;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.IBakedModel;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemOverrideList;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;

/**
 * Baked models across the two games: a modern model seen by legacy code ({@link LegacyView}: quads converted to the legacy layout) and
 * a mod's own legacy model drawn by the game ({@link ModernView}: asks it for quads per state, side and render layer), plus the names
 * of legacy model locations in the modern model registry (a block variant is one meta of the modern block; an inventory model is the
 * model of each item subtype registered with it).
 */
public final class Models1122 {
    private static final Map<BakedModel, IBakedModel> LEGACY = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<IBakedModel, BakedModel> MODERN = Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile Map<String, List<net.minecraft.client.resources.model.ModelResourceLocation>> locations;

    private Models1122() {}

    public static IBakedModel legacy(BakedModel modern) {
        if (modern == null) return null;
        if (modern instanceof ModernView view) return view.legacy;
        return LEGACY.computeIfAbsent(modern, LegacyView::new);
    }

    public static BakedModel modern(IBakedModel legacy) {
        if (legacy == null) return null;
        if (legacy instanceof LegacyView view) return view.modern;
        return MODERN.computeIfAbsent(legacy, ModernView::new);
    }

    /** The model the game has under a legacy location (the missing model if none). */
    public static IBakedModel legacyModel(ModelResourceLocation location) {
        var manager = Minecraft.getInstance().getModelManager();
        for (var modern : modernLocations(location)) {
            BakedModel model = manager.getModel(modern);
            if (model != manager.getMissingModel()) return legacy(model);
        }
        return legacy(manager.getMissingModel());
    }

    /** Forgets the location map (models were reloaded; registrations may have changed). */
    public static void invalidate() {
        locations = null;
    }

    /** The modern locations a legacy model location stands for (several item subtypes may share an inventory model). */
    public static List<net.minecraft.client.resources.model.ModelResourceLocation> modernLocations(ModelResourceLocation location) {
        Map<String, List<net.minecraft.client.resources.model.ModelResourceLocation>> map = locations;
        if (map == null) locations = map = buildLocations();
        List<net.minecraft.client.resources.model.ModelResourceLocation> found = map.get(location.toString());
        if (found != null) return found;
        var id = location.mcc$modern();
        String variant = location.getVariant();
        return List.of(new net.minecraft.client.resources.model.ModelResourceLocation(id, variant.equals("normal") ? "" : variant));
    }

    private static Map<String, List<net.minecraft.client.resources.model.ModelResourceLocation>> buildLocations() {
        Map<String, List<net.minecraft.client.resources.model.ModelResourceLocation>> map = new HashMap<>();
        for (Block block : Registries1122.BLOCKS.getValues()) {
            if (!(block.mcc$modern instanceof Legacy1122Block modern)) continue;
            try {
                var mapper = legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.mcc$stateMapper(block);
                if (mapper == null) mapper = new legacyconnector.v1_12_2.net.minecraft.client.renderer.block.statemap.DefaultStateMapper();
                for (var e : mapper.putStateModelLocations(block).entrySet()) {
                    int meta = block.getMetaFromState(e.getKey()) & 15;
                    BlockState state = modern.defaultBlockState().setValue(Legacy1122Block.META, meta);
                    map.computeIfAbsent(e.getValue().toString(), k -> new ArrayList<>()).add(BlockModelShaper.stateToModelLocation(state));
                }
            } catch (RuntimeException | LinkageError e) {
                // a mapper reaching an API the layer lacks: this block's variants keep their plain names
            }
        }
        for (var item : ItemBridge1122.modItems()) {
            for (int meta = 0; meta < 16; meta++) {
                var modern = ItemBridge1122.modern(item, meta);
                if (modern == null) continue;
                var mrl = legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.mcc$model(item, meta);
                String key = mrl != null ? mrl.toString() : item.getRegistryName() == null ? null : item.getRegistryName() + "#inventory";
                if (key == null) continue;
                var target = net.minecraft.client.resources.model.ModelResourceLocation.inventory(BuiltInRegistries.ITEM.getKey(modern));
                List<net.minecraft.client.resources.model.ModelResourceLocation> list = map.computeIfAbsent(key, k -> new ArrayList<>());
                if (!list.contains(target)) list.add(target);
            }
        }
        return map;
    }

    /** Legacy names of the modern models of legacy mods (the keys ModelBakeEvent's registry shows). */
    public static Map<String, List<net.minecraft.client.resources.model.ModelResourceLocation>> allLocations() {
        Map<String, List<net.minecraft.client.resources.model.ModelResourceLocation>> map = locations;
        if (map == null) locations = map = buildLocations();
        return map;
    }

    /** A modern model as a legacy one. */
    public static final class LegacyView implements IBakedModel {
        final BakedModel modern;
        private ItemCameraTransforms transforms;

        LegacyView(BakedModel modern) {
            this.modern = modern;
        }

        public BakedModel mcc$modern() {
            return modern;
        }

        @Override
        public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
            BlockState modernState = state == null ? null : StatesBridge1122.toModern(state);
            List<net.minecraft.client.renderer.block.model.BakedQuad> quads;
            try {
                quads = modern.getQuads(modernState, Quads1122.direction(side), RandomSource.create(rand));
            } catch (RuntimeException e) {
                RenderErrors1122.report("reading a model's quads", e);
                return List.of();
            }
            List<BakedQuad> out = new ArrayList<>(quads.size());
            for (var q : quads) out.add(Quads1122.toLegacy(q));
            return out;
        }

        @Override
        public boolean isAmbientOcclusion() {
            return modern.useAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return modern.isGui3d();
        }

        @Override
        public boolean isBuiltInRenderer() {
            return modern.isCustomRenderer();
        }

        @Override
        public TextureAtlasSprite getParticleTexture() {
            return Quads1122.legacySprite(modern.getParticleIcon());
        }

        @Override
        public ItemCameraTransforms getItemCameraTransforms() {
            if (transforms == null) transforms = ItemCameraTransforms.mcc$of(modern.getTransforms());
            return transforms;
        }

        @Override
        public ItemOverrideList getOverrides() {
            return new ItemOverrideList(List.of()) {
                @Override
                public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, World world, EntityLivingBase entity) {
                    var level = Minecraft.getInstance().level;
                    var living = entity != null && entity.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l ? l : null;
                    BakedModel resolved = modern.getOverrides().resolve(modern, Stacks1122.toModern(stack), level, living, 0);
                    return resolved == null || resolved == modern ? originalModel : legacy(resolved);
                }
            };
        }
    }

    /** The legacy render layer a modern render type is drawn in (MinecraftForgeClient.getRenderLayer while a model is asked). */
    public static legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer layer(net.minecraft.client.renderer.RenderType type) {
        if (type == null) return null;
        if (type == net.minecraft.client.renderer.RenderType.translucent()) return legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer.TRANSLUCENT;
        if (type == net.minecraft.client.renderer.RenderType.cutout()) return legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer.CUTOUT;
        if (type == net.minecraft.client.renderer.RenderType.cutoutMipped()) return legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer.CUTOUT_MIPPED;
        return legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer.SOLID;
    }

    /** A mod's legacy model as a modern one. */
    public static final class ModernView implements BakedModel {
        final IBakedModel legacy;
        private net.minecraft.client.renderer.block.model.ItemTransforms transforms;

        ModernView(IBakedModel legacy) {
            this.legacy = legacy;
        }

        public IBakedModel mcc$legacy() {
            return legacy;
        }

        @Override
        public List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(BlockState state, net.minecraft.core.Direction side, RandomSource rand) {
            return quads(state, side, rand, null);
        }

        @Override
        public List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(BlockState state, net.minecraft.core.Direction side, RandomSource rand,
                net.neoforged.neoforge.client.model.data.ModelData data, net.minecraft.client.renderer.RenderType renderType) {
            return quads(state, side, rand, renderType);
        }

        private List<net.minecraft.client.renderer.block.model.BakedQuad> quads(BlockState state, net.minecraft.core.Direction side, RandomSource rand,
                net.minecraft.client.renderer.RenderType renderType) {
            var previous = legacyconnector.v1_12_2.net.minecraftforge.client.MinecraftForgeClient.getRenderLayer();
            legacyconnector.v1_12_2.net.minecraftforge.client.ForgeHooksClient.setRenderLayer(layer(renderType));
            try {
                IBlockState legacyState = state == null ? null : StatesBridge1122.toLegacy(state);
                List<BakedQuad> quads = legacy.getQuads(legacyState, Quads1122.facing(side), rand.nextLong());
                if (quads == null || quads.isEmpty()) return List.of();
                List<net.minecraft.client.renderer.block.model.BakedQuad> out = new ArrayList<>(quads.size());
                for (BakedQuad q : quads) if (q != null) out.add(Quads1122.toModern(q));
                return out;
            } catch (RuntimeException | LinkageError e) {
                RenderErrors1122.report("asking a legacy model " + legacy.getClass().getName() + " for quads", e);
                return List.of();
            } finally {
                legacyconnector.v1_12_2.net.minecraftforge.client.ForgeHooksClient.setRenderLayer(previous);
            }
        }

        @Override
        public boolean useAmbientOcclusion() {
            try {
                return legacy.isAmbientOcclusion();
            } catch (RuntimeException e) {
                return true;
            }
        }

        @Override
        public boolean isGui3d() {
            try {
                return legacy.isGui3d();
            } catch (RuntimeException e) {
                return true;
            }
        }

        @Override
        public boolean usesBlockLight() {
            return isGui3d();
        }

        @Override
        public boolean isCustomRenderer() {
            try {
                return legacy.isBuiltInRenderer();
            } catch (RuntimeException e) {
                return false;
            }
        }

        @Override
        public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon() {
            net.minecraft.client.renderer.texture.TextureAtlasSprite sprite = null;
            try {
                TextureAtlasSprite legacySprite = legacy.getParticleTexture();
                if (legacySprite != null) sprite = legacySprite.mcc$modern();
            } catch (RuntimeException e) {
                // the missing sprite
            }
            return sprite != null ? sprite : Quads1122.missingSprite();
        }

        @Override
        public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() {
            if (transforms == null) {
                try {
                    transforms = legacy.getItemCameraTransforms().mcc$modern();
                } catch (RuntimeException e) {
                    transforms = net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS;
                }
            }
            return transforms;
        }

        @Override
        public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() {
            return new net.minecraft.client.renderer.block.model.ItemOverrides() {
                @Override
                public BakedModel resolve(BakedModel model, net.minecraft.world.item.ItemStack stack, net.minecraft.client.multiplayer.ClientLevel level,
                        net.minecraft.world.entity.LivingEntity entity, int seed) {
                    try {
                        ItemOverrideList overrides = legacy.getOverrides();
                        if (overrides == null) return model;
                        World world = level == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.wrap(level);
                        EntityLivingBase living = entity == null ? null
                                : org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(entity) instanceof EntityLivingBase l ? l : null;
                        IBakedModel result = overrides.handleItemState(legacy, Stacks1122.fromModern(stack), world, living);
                        return result == null || result == legacy ? model : modern(result);
                    } catch (RuntimeException | LinkageError e) {
                        RenderErrors1122.report("resolving a legacy model's item overrides", e);
                        return model;
                    }
                }
            };
        }

        /** The legacy perspective handler picks the model and the matrix of a display type (Forge 1.12's handlePerspective). */
        @Override
        public BakedModel applyTransform(net.minecraft.world.item.ItemDisplayContext context, com.mojang.blaze3d.vertex.PoseStack pose, boolean leftHand) {
            Pair<? extends IBakedModel, Matrix4f> pair;
            try {
                pair = legacy.handlePerspective(ItemCameraTransforms.TransformType.mcc$of(context));
            } catch (RuntimeException | LinkageError e) {
                RenderErrors1122.report("a legacy model's perspective transform", e);
                return this;
            }
            if (pair == null) return this;
            Matrix4f m = pair.getRight();
            if (m != null) {
                org.joml.Matrix4f matrix = new legacyconnector.v1_12_2.net.minecraftforge.common.model.TRSRTransformation(m).mcc$joml();
                if (leftHand) {
                    org.joml.Matrix4f flip = new org.joml.Matrix4f().scaling(-1, 1, 1);
                    matrix = flip.mul(matrix).mul(flip);
                }
                pose.last().pose().mul(matrix);
                org.joml.Matrix3f normal = new org.joml.Matrix3f(matrix);
                if (Math.abs(normal.determinant()) > 1.0E-12F) pose.last().normal().mul(normal.invert().transpose());
            }
            IBakedModel chosen = pair.getLeft();
            return chosen == null || chosen == legacy ? this : modern(chosen);
        }
    }
}
