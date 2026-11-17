package org.kylepitcock.legacyconnector.core.problems;

import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.kylepitcock.legacyconnector.core.problems.LegacyProblems.Kind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client half of {@link LegacyProblems}, loaded only on the client. It reads results the game already produced, so nothing here changes how
 * anything renders: {@link ModelEvent.BakingCompleted} (the baked model of every blockstate and item, after all mods' replacements) tells
 * which models fell back to the missing model and which baked quads use the missing sprite (the texture atlas stitch result), and what
 * has no model at all; the display names are checked against the loaded language; the renderer census runs when the first world is joined.
 */
final class ProblemsClient {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Problems");
    /** Blockstates probed per block for a model (blocks with more states than this are sampled from the first ones). */
    private static final int STATES_PER_BLOCK = 64;
    private static boolean renderersCounted;

    private ProblemsClient() {}

    static void register(IEventBus modBus) {
        modBus.addListener(ProblemsClient::onBaked);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn e) -> onJoin());
    }

    private static void onBaked(ModelEvent.BakingCompleted event) {
        try {
            Map<ModelResourceLocation, BakedModel> models = event.getModels();
            BakedModel missing = models.get(ModelBakery.MISSING_MODEL_VARIANT);
            long t0 = System.nanoTime();
            scanModels(models, missing);
            scanBlocks(models, missing);
            scanItems(models, missing);
            LOG.info("problem scan after model baking took {} ms", (System.nanoTime() - t0) / 1_000_000);
            LegacyProblems.writeReport();
        } catch (Throwable t) { // a diagnostic must never break loading
            LOG.debug("problem scan after model baking failed: {}", t.toString());
        }
    }

    /** Models of legacy namespaces that are the missing model, or whose static quads and particle use the missing sprite. */
    private static void scanModels(Map<ModelResourceLocation, BakedModel> models, BakedModel missing) {
        ResourceLocation missingSprite = MissingTextureAtlasSprite.getLocation();
        RandomSource random = RandomSource.create(0);
        for (var e : models.entrySet()) {
            ModelResourceLocation key = e.getKey();
            String ns = key.id().getNamespace();
            if (!LegacyProblems.isLegacyNamespace(ns)) continue;
            BakedModel model = e.getValue();
            if (model == null) continue;
            if (model == missing) {
                LegacyProblems.record(Kind.MISSING_MODEL, ns, key.toString(), "baked as the missing model");
                continue;
            }
            // only the plain baked models of vanilla's loader: custom (legacy renderer, wrapper) models draw by code and are not asked here
            if (!model.getClass().getName().startsWith("net.minecraft.client.resources.model.")) continue;
            // one entry per block or item: the 16 meta states of a block share the finding
            String subject = key.variant().startsWith("meta=") ? key.id().toString() : key.toString();
            // "standalone" models (a model file another model loads, e.g. Mekanism's item/crusher) are not what any slot or block draws:
            // the item's own #inventory model is judged instead
            if (key.variant().equals("standalone")) continue;
            boolean itemModel = key.variant().equals("inventory");
            try {
                if (itemModel && !model.isCustomRenderer() && isEmpty(model, random)) {
                    LegacyProblems.record(Kind.NO_ITEM_MODEL, ns, subject, "the item model has no geometry (draws nothing)");
                    continue;
                }
                boolean faceMissing = usesMissingSprite(model, null, random, missingSprite);
                if (!faceMissing) {
                    for (Direction d : Direction.values()) {
                        if (usesMissingSprite(model, d, random, missingSprite)) {
                            faceMissing = true;
                            break;
                        }
                    }
                }
                if (faceMissing) {
                    LegacyProblems.record(Kind.MISSING_TEXTURE, ns, subject, "a face uses the missing texture");
                } else if (isMissing(model.getParticleIcon(), missingSprite) && !itemModel) {
                    // Only the particle sprite is missing. An item's particle is never seen in a slot or in the hand (Thermal bows and
                    // tools draw correctly with it), so only a block's break particles count.
                    LegacyProblems.record(Kind.MISSING_TEXTURE, ns, subject, "particle texture is the missing texture");
                }
            } catch (RuntimeException ex) {
                // a model that needs state to answer is not a plain one; leave it
            }
        }
    }

    private static boolean isEmpty(BakedModel model, RandomSource random) {
        if (!model.getQuads(null, null, random).isEmpty()) return false;
        for (Direction d : Direction.values()) if (!model.getQuads(null, d, random).isEmpty()) return false;
        return true;
    }

    private static boolean usesMissingSprite(BakedModel model, Direction side, RandomSource random, ResourceLocation missingSprite) {
        for (var quad : model.getQuads(null, side, random)) {
            if (isMissing(quad.getSprite(), missingSprite)) return true;
        }
        return false;
    }

    private static boolean isMissing(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, ResourceLocation missingSprite) {
        return sprite != null && sprite.contents().name().equals(missingSprite);
    }

    private static void scanBlocks(Map<ModelResourceLocation, BakedModel> models, BakedModel missing) {
        Map<String, long[]> perMod = new java.util.HashMap<>(); // blocks, with a model
        for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            if (!LegacyProblems.isLegacyNamespace(id.getNamespace())) continue;
            var block = entry.getValue();
            long[] c = perMod.computeIfAbsent(id.getNamespace(), k -> new long[2]);
            c[0]++;
            boolean has = true;
            try {
                int n = 0;
                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                    if (n++ >= STATES_PER_BLOCK) break;
                    if (state.getRenderShape() != RenderShape.MODEL) continue; // liquids, invisible blocks and block-entity-only blocks draw by other means
                    BakedModel m = models.get(BlockModelShaper.stateToModelLocation(state));
                    if (m == null || m == missing) {
                        has = false;
                        break;
                    }
                }
            } catch (RuntimeException ex) {
                // a state that cannot answer its render shape: not judged
            }
            if (has) c[1]++;
            else LegacyProblems.record(Kind.NO_BLOCK_MODEL, id, "a blockstate has no model or the missing model");
            // names are judged on the item (scanItems): legacy blocks with subtypes name the stack in getItemStackDisplayName, and the block's
            // own description is the key by design (Thermal Expansion, Mekanism: every subtype showed a "missing name" that the game never shows)
        }
        perMod.forEach((ns, c) -> {
            LegacyProblems.census(ns, "blocks", c[0]);
            LegacyProblems.census(ns, "blocks_with_model", c[1]);
        });
    }

    private static void scanItems(Map<ModelResourceLocation, BakedModel> models, BakedModel missing) {
        Map<String, long[]> perMod = new java.util.HashMap<>();
        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            if (!LegacyProblems.isLegacyNamespace(id.getNamespace())) continue;
            var item = entry.getValue();
            long[] c = perMod.computeIfAbsent(id.getNamespace(), k -> new long[2]);
            c[0]++;
            BakedModel m = models.get(ModelResourceLocation.inventory(id));
            if (m == null || m == missing) LegacyProblems.record(Kind.NO_ITEM_MODEL, id, "no item model, or the missing model");
            else c[1]++;
            try {
                if (new ItemStack(item).getHoverName().getString().equals(item.getDescriptionId())) {
                    LegacyProblems.record(Kind.NO_LANG_NAME, id, "item shows its translation key " + item.getDescriptionId());
                }
            } catch (RuntimeException ex) {
                // a name that throws is not a missing name
            }
        }
        perMod.forEach((ns, c) -> {
            LegacyProblems.census(ns, "items", c[0]);
            LegacyProblems.census(ns, "items_with_model", c[1]);
        });
    }


    /** First world join: renderers are registered by now. Counts the entity and block entity types that have one. */
    private static void onJoin() {
        try {
            LegacyProblems.registryCensus();
            if (!renderersCounted) {
                renderersCounted = true;
                Map<?, ?> entityRenderers = staticMap(EntityRenderers.class);
                Map<?, ?> blockEntityRenderers = staticMap(BlockEntityRenderers.class);
                if (entityRenderers != null) {
                    Map<String, long[]> perMod = new java.util.HashMap<>();
                    for (var entry : BuiltInRegistries.ENTITY_TYPE.entrySet()) {
                        String ns = entry.getKey().location().getNamespace();
                        if (!LegacyProblems.isLegacyNamespace(ns)) continue;
                        if (entityRenderers.containsKey(entry.getValue())) perMod.computeIfAbsent(ns, k -> new long[1])[0]++;
                    }
                    countRenderers("entities_with_renderer", "entities", perMod);
                }
                if (blockEntityRenderers != null) {
                    Map<String, long[]> perMod = new java.util.HashMap<>();
                    for (var entry : BuiltInRegistries.BLOCK_ENTITY_TYPE.entrySet()) {
                        String ns = entry.getKey().location().getNamespace();
                        if (!LegacyProblems.isLegacyNamespace(ns)) continue;
                        if (blockEntityRenderers.containsKey(entry.getValue())) perMod.computeIfAbsent(ns, k -> new long[1])[0]++;
                    }
                    countRenderers("block_entities_with_renderer", "block_entities", perMod);
                }
            }
        } catch (Throwable t) {
            LOG.debug("renderer census failed: {}", t.toString());
        }
        LegacyProblems.finish("joined a world");
    }

    private static void countRenderers(String key, String totalKey, Map<String, long[]> perMod) {
        // every legacy namespace that has entries of the kind gets a value, 0 included
        for (String ns : new java.util.TreeSet<>(perMod.keySet())) LegacyProblems.census(ns, key, perMod.get(ns)[0]);
        for (String ns : namespacesWith(totalKey)) if (!perMod.containsKey(ns)) LegacyProblems.census(ns, key, 0);
    }

    private static java.util.Set<String> namespacesWith(String totalKey) {
        java.util.Set<String> out = new java.util.TreeSet<>();
        var registry = totalKey.equals("entities") ? BuiltInRegistries.ENTITY_TYPE : BuiltInRegistries.BLOCK_ENTITY_TYPE;
        for (var id : registry.keySet()) if (LegacyProblems.isLegacyNamespace(id.getNamespace())) out.add(id.getNamespace());
        return out;
    }

    private static Map<?, ?> staticMap(Class<?> owner) {
        try {
            Field f = owner.getDeclaredField("PROVIDERS");
            f.setAccessible(true);
            return (Map<?, ?>) f.get(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
