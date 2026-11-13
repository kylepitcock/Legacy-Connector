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

    private static final ThreadLocal<java.util.Set<Object>> BLOCK_ONLY_ACTIVE = ThreadLocal.withInitial(java.util.HashSet::new);
    private static final Map<IBakedModel, IBakedModel> BLOCK_ONLY = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * A mod's own model as the model of a block state its metadata cannot hold, with no item overrides. In 1.12 such a state (a
     * property only the mod's renderer uses, e.g. EnderIO's {@code render=} part) had its own blockstate variant with a plain JSON model;
     * here a state is one modern {@code meta} state, so it gets the model of the whole block, and that model's item overrides may ask
     * for the same state's model again: EnderIO's item override list collects the quads of its machine's render-part states, got its
     * own override list back for each and recursed until the creative tab crashed with a StackOverflowError.
     */
    public static IBakedModel withoutOverrides(IBakedModel model) {
        if (model == null || model instanceof LegacyView) return model;
        return BLOCK_ONLY.computeIfAbsent(model, BlockOnlyView::new);
    }

    /** A mod model whose item overrides are none (see {@link #withoutOverrides}). */
    private record BlockOnlyView(IBakedModel model) implements IBakedModel {
        @Override
        public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
            // asked again for its own quads while it is collecting them (EnderIO's QuadCollector gathers the "friendly" states' models, which are
            // all this one model here): the nested ask draws nothing instead of recursing until the stack is gone
            var active = BLOCK_ONLY_ACTIVE.get();
            if (!active.add(this)) return List.of();
            try {
                return model.getQuads(state, side, rand);
            } finally {
                active.remove(this);
            }
        }

        @Override
        public boolean isAmbientOcclusion() {
            return model.isAmbientOcclusion();
        }

        @Override
        public boolean isAmbientOcclusion(IBlockState state) {
            return model.isAmbientOcclusion(state);
        }

        @Override
        public boolean isGui3d() {
            return model.isGui3d();
        }

        @Override
        public boolean isBuiltInRenderer() {
            return model.isBuiltInRenderer();
        }

        @Override
        public TextureAtlasSprite getParticleTexture() {
            return model.getParticleTexture();
        }

        @Override
        @SuppressWarnings("deprecation")
        public ItemCameraTransforms getItemCameraTransforms() {
            return model.getItemCameraTransforms();
        }

        @Override
        public ItemOverrideList getOverrides() {
            return ItemOverrideList.NONE;
        }

        @Override
        public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType type) {
            return model.handlePerspective(type);
        }
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

    /** Whether a baked model is a copy of the missing model (a cube of missing-texture faces, baked for a location whose json failed to load). */
    static boolean looksMissing(BakedModel model, BakedModel missing) {
        if (model == missing) return true;
        if (model instanceof ModernView || !(model instanceof net.minecraft.client.resources.model.SimpleBakedModel)) return false;
        try {
            var sprite = missing.getParticleIcon();
            if (model.getParticleIcon() != sprite) return false;
            var random = RandomSource.create(0);
            int mine = 0;
            int theirs = 0;
            for (var side : new net.minecraft.core.Direction[] {null, net.minecraft.core.Direction.DOWN, net.minecraft.core.Direction.UP, net.minecraft.core.Direction.NORTH,
                    net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.WEST, net.minecraft.core.Direction.EAST}) {
                for (var q : model.getQuads(null, side, random)) {
                    if (q.getSprite() != sprite) return false;
                    mine++;
                }
                theirs += missing.getQuads(null, side, random).size();
            }
            return mine == theirs;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * The modern state locations of a block variant name given by a state mapper registered after the location map was built (Extra Utilities 2
     * registers its mappers inside ModelBakeEvent): the block's current mapper is run again, for that block only.
     */
    private static List<net.minecraft.client.resources.model.ModelResourceLocation> viaCurrentMapper(ModelResourceLocation location) {
        String variant = location.getVariant();
        if (variant.equals("inventory")) return null;
        try {
            var block = Registries1122.BLOCKS.getValue(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(location.getNamespace(), location.getPath()));
            if (block == null || !(block.mcc$modern instanceof Legacy1122Block modern)) return null;
            var mapper = legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.mcc$stateMapper(block);
            if (mapper == null) return null;
            List<net.minecraft.client.resources.model.ModelResourceLocation> found = new ArrayList<>();
            for (var e : mapper.putStateModelLocations(block).entrySet()) {
                var mrl = e.getValue();
                if (!mrl.getNamespace().equals(location.getNamespace()) || !mrl.getPath().equals(location.getPath()) || !mrl.getVariant().equals(variant)) continue;
                BlockState state = modern.defaultBlockState().setValue(Legacy1122Block.META, block.getMetaFromState(e.getKey()) & 15);
                var target = BlockModelShaper.stateToModelLocation(state);
                if (!found.contains(target)) found.add(target);
            }
            return found.isEmpty() ? null : found;
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
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
        List<net.minecraft.client.resources.model.ModelResourceLocation> viaMapper = viaCurrentMapper(location);
        if (viaMapper != null) return viaMapper;
        return List.of(new net.minecraft.client.resources.model.ModelResourceLocation(id, variant.equals("normal") ? "" : variant));
    }

    private static Map<String, List<net.minecraft.client.resources.model.ModelResourceLocation>> buildLocations() {
        Map<String, List<net.minecraft.client.resources.model.ModelResourceLocation>> map = new HashMap<>();
        Map<IBlockState, net.minecraft.client.resources.model.ModelResourceLocation> byState = new java.util.IdentityHashMap<>();
        for (Block block : Registries1122.BLOCKS.getValues()) {
            if (!(block.mcc$modern instanceof Legacy1122Block modern)) continue;
            try {
                var mapper = legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.mcc$stateMapper(block);
                if (mapper == null) mapper = new legacyconnector.v1_12_2.net.minecraft.client.renderer.block.statemap.DefaultStateMapper();
                for (var e : mapper.putStateModelLocations(block).entrySet()) {
                    String name = e.getValue().toString();
                    var own = StatesBridge1122.metaHolds(e.getKey()) ? null : org.kylepitcock.legacyconnector.v1_12_2.Assets1122.stateModel(name);
                    if (own != null) {
                        // a state beyond the metadata with a model of its own (see Assets1122.stateModel)
                        var target = net.minecraft.client.resources.model.ModelResourceLocation.standalone(own);
                        map.put(name, new ArrayList<>(List.of(target)));
                        byState.put(e.getKey(), target);
                        continue;
                    }
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
        // models an item registered beside its own: found under their inventory location, baked as standalone models
        for (var e : legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.mcc$allItemVariantModels().entrySet()) {
            map.putIfAbsent(e.getKey().contains("#") ? e.getKey() : e.getKey() + "#inventory", new ArrayList<>(List.of(net.minecraft.client.resources.model.ModelResourceLocation.standalone(e.getValue()))));
        }
        stateLocations = byState;
        return map;
    }

    /** The standalone model location of each block state beyond its metadata that has a model of its own (built with the location map). */
    private static volatile Map<IBlockState, net.minecraft.client.resources.model.ModelResourceLocation> stateLocations = Map.of();

    /** The standalone model locations of the block states beyond their metadata that have models of their own. */
    static Map<IBlockState, net.minecraft.client.resources.model.ModelResourceLocation> stateLocations() {
        if (locations == null) locations = buildLocations();
        return stateLocations;
    }

    /**
     * The model of a block state the metadata does not hold, as 1.12 baked it from the state's own variant (see Assets1122.stateModel),
     * or null when it has none.
     */
    public static IBakedModel stateModel(IBlockState state) {
        if (locations == null) locations = buildLocations();
        var target = stateLocations.get(StatesBridge1122.canonical(state));
        if (target == null) return null;
        var manager = Minecraft.getInstance().getModelManager();
        BakedModel model = manager.getModel(target);
        return model == null || model == manager.getMissingModel() ? null : legacy(model);
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

    /** The legacy (actual, extended) state of a block position, computed in {@code getModelData}. */
    static final net.neoforged.neoforge.client.model.data.ModelProperty<IBlockState> LEGACY_STATE = new net.neoforged.neoforge.client.model.data.ModelProperty<>();

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
            return quads(state, side, rand, null, null);
        }

        @Override
        public List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(BlockState state, net.minecraft.core.Direction side, RandomSource rand,
                net.neoforged.neoforge.client.model.data.ModelData data, net.minecraft.client.renderer.RenderType renderType) {
            return quads(state, side, rand, renderType, data == null ? null : data.get(LEGACY_STATE));
        }

        /**
         * Forge 1.12 asked the block for the state a model draws: {@code getActualState} (connections...) and {@code getExtendedState}
         * (unlisted render properties, which custom models such as Thaumcraft's crystals cast to IExtendedBlockState). The modern renderer gives the
         * model only the stored state, so the legacy state is worked out here, where the position is known, and travels as model data.
         */
        @Override
        public net.neoforged.neoforge.client.model.data.ModelData getModelData(net.minecraft.world.level.BlockAndTintGetter level,
                net.minecraft.core.BlockPos pos, BlockState state, net.neoforged.neoforge.client.model.data.ModelData modelData) {
            if (state == null || !(state.getBlock() instanceof Legacy1122Block)) return modelData;
            try {
                IBlockState base = StatesBridge1122.toLegacy(state);
                if (base == null) return modelData;
                var access = org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.access(level);
                var legacyPos = new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos(pos.getX(), pos.getY(), pos.getZ());
                IBlockState actual = base.getBlock().getActualState(base, access, legacyPos);
                IBlockState extended = base.getBlock().getExtendedState(actual == null ? base : actual, access, legacyPos);
                if (extended == null || extended == base) return modelData;
                return modelData.derive().with(LEGACY_STATE, extended).build();
            } catch (RuntimeException | LinkageError e) {
                RenderErrors1122.report("asking a legacy block for its actual/extended state", e);
                return modelData;
            }
        }

        private List<net.minecraft.client.renderer.block.model.BakedQuad> quads(BlockState state, net.minecraft.core.Direction side, RandomSource rand,
                net.minecraft.client.renderer.RenderType renderType, IBlockState given) {
            var previous = legacyconnector.v1_12_2.net.minecraftforge.client.MinecraftForgeClient.getRenderLayer();
            legacyconnector.v1_12_2.net.minecraftforge.client.ForgeHooksClient.setRenderLayer(layer(renderType));
            try {
                IBlockState legacyState = given != null ? given : state == null ? null : StatesBridge1122.toLegacy(state);
                List<BakedQuad> quads = legacy.getQuads(legacyState, Quads1122.facing(side), rand.nextLong());
                if (quads == null || quads.isEmpty()) return List.of();
                List<net.minecraft.client.renderer.block.model.BakedQuad> out = new ArrayList<>(quads.size());
                for (BakedQuad q : quads) if (q != null) out.add(Quads1122.toModern(q));
                return out;
            } catch (RuntimeException | LinkageError | StackOverflowError e) {
                // a model that relays to "its" model of another state (EnderIO's RelayingBakedModel: its render= states are one meta state here) can
                // ask itself forever; that must cost the block its drawing, not the client
                RenderErrors1122.report("asking a legacy model " + legacy.getClass().getName() + " for quads", e instanceof StackOverflowError so ? new RuntimeException(so.toString()) : e);
                return List.of();
            } finally {
                legacyconnector.v1_12_2.net.minecraftforge.client.ForgeHooksClient.setRenderLayer(previous);
            }
        }

        /**
         * 1.12 drew a block in every terrain layer its {@code canRenderInLayer} accepted, asking the model for that layer's quads (a CodeChickenLib
         * bakery model bakes cutout quads only). The modern default takes the layer from the model's own json, which a model a mod made in code lacks.
         */
        @Override
        public net.neoforged.neoforge.client.ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand,
                net.neoforged.neoforge.client.model.data.ModelData data) {
            if (state != null && state.getBlock() instanceof Legacy1122Block) {
                try {
                    IBlockState given = data == null ? null : data.get(LEGACY_STATE);
                    IBlockState legacyState = given != null ? given : StatesBridge1122.toLegacy(state);
                    if (legacyState != null) {
                        List<net.minecraft.client.renderer.RenderType> types = new ArrayList<>(4);
                        var block = legacyState.getBlock();
                        for (var layer : legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer.values()) {
                            if (!block.canRenderInLayer(legacyState, layer)) continue;
                            types.add(switch (layer) {
                                case SOLID -> net.minecraft.client.renderer.RenderType.solid();
                                case CUTOUT_MIPPED -> net.minecraft.client.renderer.RenderType.cutoutMipped();
                                case CUTOUT -> net.minecraft.client.renderer.RenderType.cutout();
                                case TRANSLUCENT -> net.minecraft.client.renderer.RenderType.translucent();
                            });
                        }
                        // only the solid layer (the default): the render type the block's own model json gave stays
                        if (!types.isEmpty() && !(types.size() == 1 && types.get(0) == net.minecraft.client.renderer.RenderType.solid())) {
                            return net.neoforged.neoforge.client.ChunkRenderTypeSet.of(types.toArray(new net.minecraft.client.renderer.RenderType[0]));
                        }
                    }
                } catch (RuntimeException | LinkageError e) {
                    // a layer check that needs a world: the default layers
                }
            }
            return BakedModel.super.getRenderTypes(state, rand, data);
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
