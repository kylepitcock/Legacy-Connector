package org.kylepitcock.legacyconnector.v1_12_2;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.kylepitcock.legacyconnector.core.Ids;
import org.kylepitcock.legacyconnector.core.pack.GeneratedPack;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generated client assets for 1.12.2 blocks: a modern block keeps its legacy state as one META property, so each block's own
 * blockstate file is rewritten to variants keyed by {@code meta=N}.
 *
 * <p>Every meta of every block gets a model that exists. The state of a meta is the one the block's {@code getMetaFromState} maps to it
 * (several states share a meta: the one closest to the block's default state is drawn). A state's variant is built the way Forge 1.12
 * read a blockstate file: the {@code defaults}, then {@code normal}, then the entry of each of the state's properties in file order
 * (a property name with a table of values), with textures and custom data merged, rotations added ({@code x}/{@code y} or a Forge
 * {@code transform} rotation) and the other keys replaced; {@code submodel}s and vanilla {@code multipart} files become modern multipart
 * on {@code meta}. Models in custom loaders ({@code forge:multi-layer} names the real variant in its custom data, {@code forge:fluid})
 * and models that do not exist fall back to a plain cube with the variant's first texture, never a missing model. The log prints how
 * many blockstates were translated and how many fell back.</p>
 */
public final class Assets1122 implements GeneratedPack.Contributor {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Assets");
    private static final Gson GSON = new Gson();

    private static final class Stats {
        int translated;
        int fellBack;
        int cutout;
        int translucent;
        int solid;
        final List<String> fallbacks = new ArrayList<>();
    }

    /** The variant of one state while it is built: the weighted alternatives, and the submodels that are drawn in addition. */
    private static final class Acc {
        List<JsonObject> alts = new ArrayList<>();
        final List<JsonObject> subs = new ArrayList<>();
    }

    /** The result for one meta: the variant JSON (object or weighted array, null when only parts are drawn) and the extra multipart models. */
    private static final class Res {
        JsonElement model;
        final List<JsonElement> parts = new ArrayList<>();
        String fallback;
        boolean empty;
    }

    @Override
    public void contribute(PackType type, Map<String, Supplier<byte[]>> out) {
        String selfTest = System.getProperty("legacyconnector.assetsSelfTest");
        if (type == PackType.SERVER_DATA && selfTest != null) {
            // a dedicated server never loads client assets: the self test runs the translation anyway and can write the result to a directory
            Map<String, Supplier<byte[]>> files = new TreeMap<>();
            translateAll(files);
            if (!selfTest.equals("true") && !selfTest.isEmpty()) {
                for (var e : files.entrySet()) {
                    try {
                        java.nio.file.Path path = java.nio.file.Path.of(selfTest, e.getKey());
                        java.nio.file.Files.createDirectories(path.getParent());
                        java.nio.file.Files.write(path, e.getValue().get());
                    } catch (IOException | RuntimeException ex) {
                        LOG.warn("Could not write {}: {}", e.getKey(), ex.toString());
                    }
                }
            }
            return;
        }
        if (type != PackType.CLIENT_RESOURCES) return;
        translateAll(out);
    }

    /**
     * perf.shareBlockModels: generated variant models (a model with the variant's textures) by namespace and content, so variants
     * that name the same model and textures (the metadata values of one look) share one file and one baked model.
     */
    private static final boolean SHARE_MODELS = org.kylepitcock.legacyconnector.core.LegacyConfig.perfShareBlockModels();
    private static final Map<String, String> GENERATED_MODELS = new java.util.concurrent.ConcurrentHashMap<>();

    /** A block variant whose model a mod's own ModelLoader loads ({@code ns:path} is the model below models/, with the block/ folder). */
    public record LoaderVariant(ResourceLocation block, int meta, String ns, String path, JsonObject source) {}

    private static final List<LoaderVariant> LOADER_VARIANTS = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** Modern state locations ("ns:block#meta=N") drawn by a stand-in cube because the legacy block had no usable model json (1.12 drew the missing model there). */
    private static final java.util.Set<String> FALLBACK_STATES = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Whether a state location has only a stand-in model (see {@link #FALLBACK_STATES}). */
    public static boolean isStandIn(net.minecraft.client.resources.model.ModelResourceLocation location) {
        return FALLBACK_STATES.contains(location.id() + "#" + location.variant());
    }

    /** The block variants whose models come from the mods' custom model loaders. */
    public static List<LoaderVariant> loaderVariants() {
        return List.copyOf(LOADER_VARIANTS);
    }

    private static boolean loaderAccepts(String ns, String path) {
        try {
            return legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoaderRegistry.mcc$loaderFor(
                    new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(ns, "models/" + path)) != null;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    private static void translateAll(Map<String, Supplier<byte[]>> out) {
        long t0 = System.nanoTime();
        LOADER_VARIANTS.clear();
        FALLBACK_STATES.clear();
        GENERATED_MODELS.clear();
        RESOURCE_EXISTS.clear();
        JSON_FILES.clear();
        STATE_MODELS_BUILDING.clear();
        clientProxies();
        vanillaModelAliases(out);
        itemModels(out);
        long itemsMs = (System.nanoTime() - t0) / 1_000_000;
        Stats stats = new Stats();
        for (Block block : Registries1122.BLOCKS.getValues()) {
            if (!(block.mcc$modern instanceof Legacy1122Block modern)) continue;
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(modern);
            try {
                translate(block, id, out, stats);
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Could not translate the blockstate of {}: {}", id, e.toString());
                stats.fellBack++;
                stats.fallbacks.add(id + " (" + e + ")");
                try {
                    fallbackBlockstate(id, null, out);
                } catch (RuntimeException ignored) {
                    // nothing more to do for this block
                }
            }
        }
        itemFallbackModels(out);
        LOG.info("[LegacyConnector/Perf] 1.12.2 assets: item models {} ms, blockstates {} ms", itemsMs, (System.nanoTime() - t0) / 1_000_000 - itemsMs);
        stateModels = Map.copyOf(STATE_MODELS_BUILDING);
        if (!stateModels.isEmpty()) LOG.info("1.12.2 state models: {} block state(s) beyond their metadata got a model of their own", stateModels.size());
        LOG.info("1.12.2 render types: {} cutout, {} translucent, {} solid", stats.cutout, stats.translucent, stats.solid);
        LOG.info("1.12.2 blockstates: {} translated, {} fell back to a plain model{}", stats.translated, stats.fellBack,
                stats.fallbacks.isEmpty() ? "" : " (" + String.join("; ", stats.fallbacks.subList(0, Math.min(60, stats.fallbacks.size())))
                        + (stats.fallbacks.size() > 60 ? "; ... " + (stats.fallbacks.size() - 60) + " more" : "") + ")");
    }

    /** Vanilla block models the 1.13 flattening renamed (1.12.2 name to 1.21.1 name): mods' models and blockstates still name the old ones as parents. */
    private static final String[][] VANILLA_MODEL_ALIASES = {
            {"lit_redstone_torch", "redstone_torch"}, {"lit_redstone_torch_wall", "redstone_wall_torch"},
            {"unlit_redstone_torch", "redstone_torch_off"}, {"unlit_redstone_torch_wall", "redstone_wall_torch_off"},
            {"comparator_unlit", "comparator"}, {"comparator_unlit_subtract", "comparator_subtract"},
            {"comparator_lit", "comparator_on"}, {"comparator_lit_subtract", "comparator_on_subtract"},
    };

    /** Writes a model file of the old name that only names the renamed vanilla model as its parent. */
    private static void vanillaModelAliases(Map<String, Supplier<byte[]>> out) {
        java.util.List<String[]> renames = new java.util.ArrayList<>();
        for (String[] r : VANILLA_MODEL_ALIASES) renames.add(r);
        for (int ticks = 1; ticks <= 4; ticks++) {
            renames.add(new String[] {"repeater_on_" + ticks + "tick", "repeater_" + ticks + "tick_on"});
            renames.add(new String[] {"repeater_on_" + ticks + "tick_locked", "repeater_" + ticks + "tick_on_locked"});
        }
        for (String[] r : renames) {
            byte[] bytes = ("{\"parent\":\"minecraft:block/" + r[1] + "\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8);
            out.putIfAbsent("assets/minecraft/models/block/" + r[0] + ".json", () -> bytes);
        }
    }

    /**
     * Development only ({@code -Dlegacyconnector.assetsClientProxies=mekanism.client.ClientProxy,...}, set it in the server self test with
     * {@code -PclientProxies=...}): a dedicated server never runs a mod's client proxy, so the state mappers and model registrations it makes
     * (ModelLoader.setCustomStateMapper, registerItemRenders) are missing from the self test; this runs {@code registerBlockRenders},
     * {@code registerItemRenders} and {@code registerModels} of the named client proxy classes.
     */
    private static void clientProxies() {
        String names = System.getProperty("legacyconnector.assetsClientProxies");
        if (names == null || names.isEmpty()) return;
        for (String name : names.split(",")) {
            try {
                Class<?> c = Class.forName(name.trim(), true, Thread.currentThread().getContextClassLoader());
                Object proxy = c.getDeclaredConstructor().newInstance();
                for (String method : new String[] {"registerBlockRenders", "registerItemRenders", "registerModels"}) {
                    try {
                        var m = c.getMethod(method);
                        m.invoke(proxy);
                    } catch (NoSuchMethodException ignored) {
                        // this proxy has no such registration
                    }
                }
                LOG.info("Ran the client registrations of {}", name);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                LOG.warn("Could not run the client proxy {}: {}", name, e.toString());
            }
        }
    }

    /** Item models registered in code (ModelLoader): each subtype's modern item points at the model file the mod named. */
    private static void itemModels(Map<String, Supplier<byte[]>> out) {
        meshIds = meshItemIds();
        for (legacyconnector.v1_12_2.net.minecraft.item.Item item : org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.modItems()) {
            if (item.getRegistryName() == null || item instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemBlock) continue;
            // every subtype the item registered (metadata goes beyond 15: AbyssalCraft's crystal shards run to 26 and more)
            for (int meta : org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.variants(item).keySet()) {
                var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.modern(item, meta);
                if (modern == null) continue;
                var mrl = legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.mcc$model(item, meta);
                // a subtype the mod gave no model of its own shows the base subtype's (instead of the missing-model cube)
                if (mrl == null && meta != 0) mrl = legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.mcc$model(item, 0);
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(modern);
                if (meshModel(item, mrl, id, out)) continue;
                if (mrl == null) continue;
                String target = itemModelTarget(mrl.getNamespace(), mrl.getPath());
                if (ObjAssets1122.isObj(mrl.getPath())) {
                    // an OBJ item model (models/item/<name>.obj): NeoForge's OBJ loader
                    target = ObjAssets1122.model(Ids.namespace(mrl.getNamespace()),
                            "item/" + Ids.normalizePath(mrl.getPath()), null, null, null, out);
                }
                // the mod's own models/item file already has the id's name (an item whose model is an .obj file of that name has none to write over)
                if (target.equals(id.getNamespace() + ":item/" + id.getPath()) && modelExists(id.getNamespace(), "item/" + id.getPath())) continue;
                JsonObject model = blockstateItemModel(mrl, target, out);
                if (model != null) {
                    // no models/item file: Forge took the item's model from the variant of the blockstate file of the same name
                    String json = GSON.toJson(model);
                    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                    out.put("assets/" + id.getNamespace() + "/models/item/" + id.getPath() + ".json", () -> bytes);
                    if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) org.kylepitcock.legacyconnector.v1_7_10.client.SharedItemModels.note(id, json);
                    continue;
                }
                // a model location with no file (a mesh definition naming a model a baked-model replacement stands behind: Botania's 3D lexicon):
                // the item's model is another one it registered, if one has a file
                if (!ObjAssets1122.isObj(mrl.getPath()) && !modelExists(Ids.namespace(mrl.getNamespace()), "item/" + Ids.normalizePath(mrl.getPath()))) {
                    for (var variant : legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.mcc$itemVariants(item)) {
                        String vNs = Ids.namespace(variant.getNamespace());
                        String vPath = Ids.normalizePath(variant.getPath());
                        if (modelExists(vNs, "item/" + vPath)) {
                            target = vNs + ":item/" + vPath;
                            break;
                        }
                    }
                }
                // still no file, and the name is the item's own: a model that is its own parent (Botania's water bowl) loops; leave the item to the default one
                if (target.equals(id.getNamespace() + ":item/" + id.getPath())) continue;
                // the model the mod named has no file anywhere (a model a baked-model replacement stands behind): ModelBakery would log
                // "Unable to load model" for the parent; the post pass (itemFallbackModels) gives the item a texture-based model instead
                if (!modelPresent(target, out)) continue;
                model = new JsonObject();
                model.addProperty("parent", target);
                String json = GSON.toJson(model);
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                out.put("assets/" + id.getNamespace() + "/models/item/" + id.getPath() + ".json", () -> bytes);
                if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) org.kylepitcock.legacyconnector.v1_7_10.client.SharedItemModels.note(id, json);
            }
        }
    }

    /**
     * The item model Forge built from a blockstate file ({@code ModelResourceLocation("mod:name", "variant")} with no {@code models/item/name.json}):
     * the variant's {@code forge:item-layer} (or a plain model) with its textures. Null when the item has a model file or the variant is not drawable this way.
     */
    private static JsonObject blockstateItemModel(legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelResourceLocation mrl, String target,
            Map<String, Supplier<byte[]>> out) {
        String ns = Ids.namespace(mrl.getNamespace());
        String path = Ids.normalizePath(mrl.getPath());
        if (modelExists(ns, "item/" + path)) return null;
        JsonObject file = readOriginal(ResourceLocation.fromNamespaceAndPath(ns, path));
        if (file == null) return null;
        String variant = mrl.getVariant() == null || mrl.getVariant().isEmpty() ? "normal" : mrl.getVariant();
        TreeMap<String, String> props = new TreeMap<>();
        for (String part : variant.split(",")) {
            int eq = part.indexOf('=');
            if (eq > 0) props.put(part.substring(0, eq), part.substring(eq + 1));
        }
        List<String> keys = new ArrayList<>();
        keys.add(variant);
        keys.add(keyOf(props));
        Acc acc = merge(file, props, keys);
        if (acc.alts.isEmpty()) return null;
        JsonObject alt = acc.alts.get(0);
        String m = alt.has("model") && alt.get("model").isJsonPrimitive() ? alt.get("model").getAsString().toLowerCase(java.util.Locale.ROOT) : "forge:item-layer";
        if (ObjAssets1122.isObj(m)) return objItemModel(ns, m, alt, path, out);
        if (!(alt.has("textures") && alt.get("textures").isJsonObject() && alt.getAsJsonObject("textures").size() > 0)) return null;
        String parent;
        if (m.equals("forge:item-layer") || m.equals("builtin/generated") || m.equals("item/generated") || m.equals("minecraft:item/generated")) {
            parent = "minecraft:item/generated";
        } else if (m.equals("item/handheld") || m.equals("minecraft:item/handheld")) {
            parent = "minecraft:item/handheld";
        } else if (m.startsWith("forge:")) {
            return null;
        } else {
            int colon = m.indexOf(':');
            String mNs = colon < 0 ? ns : Ids.namespace(m.substring(0, colon));
            String mPath = Ids.normalizePath(colon < 0 ? m : m.substring(colon + 1));
            if (mPath.startsWith("item/") || mPath.startsWith("block/")) parent = mNs + ":" + mPath;
            else parent = mNs + ":" + (modelExists(mNs, "item/" + mPath) ? "item/" : "block/") + mPath;
        }
        JsonObject model = new JsonObject();
        model.addProperty("parent", parent);
        JsonObject textures = new JsonObject();
        for (var t : alt.getAsJsonObject("textures").entrySet()) {
            if (t.getValue().isJsonPrimitive()) textures.addProperty(t.getKey().startsWith("#") ? t.getKey().substring(1) : t.getKey(), lowerRef(t.getValue().getAsString()));
        }
        model.add("textures", textures);
        return model;
    }

    /**
     * An item whose blockstate variant names an OBJ model (AbyssalCraft's staff, dread katana, cudgel: {@code "model": "mod:staff.obj"} with the file in
     * models/item): Forge looked the file up under models/, models/item/ and models/block/. The Forge hand/gui transforms of the variant become display.
     */
    private static JsonObject objItemModel(String ns, String m, JsonObject alt, String itemPath, Map<String, Supplier<byte[]>> out) {
        int colon = m.indexOf(':');
        String mNs = colon < 0 ? ns : Ids.namespace(m.substring(0, colon));
        String mPath = Ids.normalizePath(colon < 0 ? m : m.substring(colon + 1));
        String found = null;
        for (String prefix : new String[] {"", "item/", "block/"}) {
            if (resourceExists("assets/" + mNs + "/models/" + prefix + mPath)) {
                found = prefix + mPath;
                break;
            }
        }
        if (found == null) return null;
        JsonObject textures = alt.has("textures") && alt.get("textures").isJsonObject() ? alt.getAsJsonObject("textures") : null;
        String objModel = ObjAssets1122.model(mNs, found, textures, ObjAssets1122.custom(alt), "item_" + itemPath, out);
        JsonObject model = new JsonObject();
        model.addProperty("parent", objModel);
        if (alt.has("transform") && alt.get("transform").isJsonObject()) {
            JsonObject display = new JsonObject();
            for (var e : alt.getAsJsonObject("transform").entrySet()) {
                if (!e.getValue().isJsonObject()) continue;
                JsonObject t = e.getValue().getAsJsonObject();
                JsonObject d = new JsonObject();
                double[] rot = new double[3];
                if (t.has("rotation") && t.get("rotation").isJsonArray()) {
                    for (var r : t.getAsJsonArray("rotation")) {
                        if (!r.isJsonObject()) continue;
                        for (var axis : r.getAsJsonObject().entrySet()) {
                            int i = "xyz".indexOf(axis.getKey());
                            if (i >= 0 && axis.getValue().isJsonPrimitive()) rot[i] = axis.getValue().getAsDouble();
                        }
                    }
                }
                com.google.gson.JsonArray ra = new com.google.gson.JsonArray();
                for (double v : rot) ra.add(v);
                d.add("rotation", ra);
                if (t.has("translation") && t.get("translation").isJsonArray()) {
                    com.google.gson.JsonArray ta = new com.google.gson.JsonArray();
                    for (var v : t.getAsJsonArray("translation")) ta.add(Math.max(-80, Math.min(80, v.getAsDouble() * 16)));
                    d.add("translation", ta);
                }
                if (t.has("scale") && t.get("scale").isJsonPrimitive()) {
                    double sc = t.get("scale").getAsDouble();
                    com.google.gson.JsonArray sa = new com.google.gson.JsonArray();
                    for (int i = 0; i < 3; i++) sa.add(sc);
                    d.add("scale", sa);
                }
                display.add(e.getKey(), d);
            }
            if (display.size() > 0) model.add("display", display);
        }
        return model;
    }

    // ------------------------------------------------------------------------------------------------------------------------------
    // one block

    /**
     * Whether the blockstate file being translated is a Forge blockstate (forge_marker): only those read the "textures" of a variant or
     * multipart entry; a vanilla 1.12 blockstate ignored them (Scape and Run's fences name a texture file the mod does not have there, the
     * model's own texture is the real one).
     */
    private static final ThreadLocal<Boolean> FORGE_MARKER = ThreadLocal.withInitial(() -> Boolean.TRUE);

    private static void translate(Block block, ResourceLocation id, Map<String, Supplier<byte[]>> out, Stats stats) {
        boolean debug = System.getProperty("legacyconnector.debugAssets") != null && id.getPath().equals(System.getProperty("legacyconnector.debugAssets"));
        JsonObject original = readOriginal(id);
        FORGE_MARKER.set(original != null && original.has("forge_marker"));
        boolean multipart = original != null && original.has("multipart") && original.get("multipart").isJsonArray() && !original.has("variants");
        IBlockState def = null;
        try {
            def = block.getDefaultState();
        } catch (RuntimeException ignored) {
            // no default state: any state stands for its meta
        }
        Map<Integer, List<IBlockState>> byMeta = new TreeMap<>();
        for (IBlockState state : new ArrayList<>(block.getBlockState().getValidStates())) {
            int meta;
            try {
                meta = block.getMetaFromState(state) & 15;
            } catch (RuntimeException e) {
                meta = 0;
            }
            byMeta.computeIfAbsent(meta, m -> new ArrayList<>()).add(state);
        }
        if (byMeta.isEmpty()) byMeta.put(0, new ArrayList<>());
        Map<IBlockState, legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelResourceLocation> mapped = null;
        var mapper = legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.mcc$stateMapper(block);
        if (mapper != null) {
            try {
                mapped = mapper.putStateModelLocations(block);
            } catch (RuntimeException e) {
                LOG.debug("The state mapper of {} failed: {}", id, e.toString());
            }
        }
        Map<Integer, Res> results = new TreeMap<>();
        boolean translucentMeta = false;
        boolean cutoutMeta = false;
        List<String> reasons = new ArrayList<>();
        for (var entry : byMeta.entrySet()) {
            int meta = entry.getKey();
            List<IBlockState> candidates = new ArrayList<>(entry.getValue());
            if (def != null && candidates.size() > 1) {
                IBlockState d = def;
                candidates.sort((x, y) -> Integer.compare(agreement(y, d), agreement(x, d)));
            }
            Res best = null;
            IBlockState bestState = null;
            for (IBlockState state : candidates) {
                Res r;
                try {
                    r = multipart ? resolveMultipart(original, id, state, "meta_" + meta, out)
                            : resolveState(original, id, state, mapped == null ? null : mapped.get(state), "meta_" + meta, out);
                } catch (RuntimeException e) {
                    r = new Res();
                    r.fallback = e.toString();
                    r.model = fallbackModel(id, null, "meta_" + meta, out);
                }
                if (best == null) {
                    best = r;
                    bestState = state;
                }
                if (!r.empty) {
                    best = r;
                    bestState = state;
                    break;
                }
            }
            if (best == null) {
                best = new Res();
                best.fallback = "no states";
                best.model = fallbackModel(id, null, "meta_" + meta, out);
            }
            String renderType = renderTypeOf(block, bestState != null ? bestState : def, best, out);
            if (renderType != null) {
                best.model = withRenderType(best.model, renderType, id, out);
                for (int i = 0; i < best.parts.size(); i++) best.parts.set(i, withRenderType(best.parts.get(i), renderType, id, out));
                if (renderType.endsWith("translucent")) translucentMeta = true;
                else cutoutMeta = true;
            }
            results.put(meta, best);
            if (best.fallback != null) FALLBACK_STATES.add(id + "#meta=" + meta);
            collectModelTextures(best, out);
            if (best.fallback != null) reasons.add("meta " + meta + ": " + best.fallback);
            if (debug) LOG.info("meta {} of {}: {} parts={} fallback={}", meta, id, best.model, best.parts, best.fallback);
        }
        try {
            stateModels(block, id, original, multipart, mapped, results, out);
        } catch (RuntimeException | LinkageError e) {
            LOG.debug("No models for the states the metadata of {} does not hold: {}", id, e.toString());
        }
        boolean anyParts = false;
        for (Res r : results.values()) anyParts |= !r.parts.isEmpty();
        JsonObject result = new JsonObject();
        Map<Integer, String> itemModels = new TreeMap<>();
        if (multipart || anyParts) {
            JsonArray mp = new JsonArray();
            Res first = results.values().iterator().next();
            for (int meta = 0; meta < 16; meta++) {
                Res r = results.getOrDefault(meta, first);
                List<JsonElement> all = new ArrayList<>();
                if (r.model != null) all.add(r.model);
                all.addAll(r.parts);
                for (JsonElement apply : all) {
                    JsonObject when = new JsonObject();
                    when.addProperty("meta", String.valueOf(meta));
                    JsonObject part = new JsonObject();
                    part.add("when", when);
                    part.add("apply", apply);
                    mp.add(part);
                }
                String m = modelOf(all.isEmpty() ? null : all.get(0));
                if (m != null) itemModels.put(meta, m);
            }
            result.add("multipart", mp);
        } else {
            JsonObject variants = new JsonObject();
            Res first = results.containsKey(0) ? results.get(0) : results.values().iterator().next();
            for (int meta = 0; meta < 16; meta++) {
                Res r = results.getOrDefault(meta, first);
                variants.add("meta=" + meta, r.model);
                String m = modelOf(r.model);
                if (m != null) itemModels.put(meta, m);
            }
            result.add("variants", variants);
        }
        if (translucentMeta) stats.translucent++;
        else if (cutoutMeta) stats.cutout++;
        else stats.solid++;
        byte[] bytes = GSON.toJson(result).getBytes(StandardCharsets.UTF_8);
        out.put("assets/" + id.getNamespace() + "/blockstates/" + id.getPath() + ".json", () -> bytes);
        blockItemModels(block, id, itemModels, out);
        if (debug) LOG.info("blockstate {}: {}", id, GSON.toJson(result));
        if (reasons.isEmpty()) {
            stats.translated++;
        } else {
            stats.fellBack++;
            stats.fallbacks.add(id.toString() + " [" + reasons.get(0) + (reasons.size() > 1 ? " +" + (reasons.size() - 1) : "") + "]");
        }
    }

    /** A block that could not be translated at all: every meta draws a cube. */
    private static void fallbackBlockstate(ResourceLocation id, JsonObject hint, Map<String, Supplier<byte[]>> out) {
        JsonObject variants = new JsonObject();
        JsonElement model = fallbackModel(id, hint, "all", out);
        for (int meta = 0; meta < 16; meta++) {
            variants.add("meta=" + meta, model);
            FALLBACK_STATES.add(id + "#meta=" + meta);
        }
        JsonObject result = new JsonObject();
        result.add("variants", variants);
        byte[] bytes = GSON.toJson(result).getBytes(StandardCharsets.UTF_8);
        out.put("assets/" + id.getNamespace() + "/blockstates/" + id.getPath() + ".json", () -> bytes);
    }

    // ------------------------------------------------------------------------------------------------------------------------------
    // states the metadata does not hold

    /**
     * Most states beyond the metadata one block may have for them to get models of their own (more are connection states from
     * getActualState, drawn by the models of their metadata as before).
     */
    private static final int MAX_STATE_MODELS = 64;
    private static final Map<String, ResourceLocation> STATE_MODELS_BUILDING = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile Map<String, ResourceLocation> stateModels = Map.of();

    /**
     * The model generated for a legacy model location that only states beyond their block's metadata use (EnderIO's
     * {@code enderio:block_alloy_smelter#render=front}), or null. Such a state is not a modern state, so its variant cannot live in the
     * modern blockstate file: it becomes a standalone model (ModelEvent.RegisterAdditional) the legacy model lookups resolve to.
     */
    public static ResourceLocation stateModel(String legacyLocation) {
        return legacyLocation == null ? null : stateModels.get(legacyLocation);
    }

    /** All models of {@link #stateModel}, to be baked as standalone models. */
    public static java.util.Collection<ResourceLocation> stateModelIds() {
        return stateModels.values();
    }

    /**
     * In 1.12 every state of a block had its own blockstate variant, also states whose properties the metadata does not store (render
     * parts a mod's smart model collects, as EnderIO's machines do with {@code render=front_on_east}). Here a block state is one modern
     * {@code meta} state; these states get their variant built the same way, as a model file of its own (rotations as root transforms,
     * submodels as children of a composite model). States that look like their metadata's state keep that model.
     */
    private static void stateModels(Block block, ResourceLocation id, JsonObject original, boolean multipart,
            Map<IBlockState, legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelResourceLocation> mapped,
            Map<Integer, Res> results, Map<String, Supplier<byte[]>> out) {
        List<IBlockState> extra = new ArrayList<>();
        int held = 0;
        for (IBlockState state : block.getBlockState().getValidStates()) {
            if (StatesBridge1122.metaHolds(state)) held++;
            else extra.add(state);
        }
        // no state held at all: getStateFromMeta does not answer with the block's own states, so nothing tells the states apart
        if (held == 0 || extra.isEmpty() || extra.size() > MAX_STATE_MODELS) return;
        var locations = mapped != null ? mapped
                : new legacyconnector.v1_12_2.net.minecraft.client.renderer.block.statemap.DefaultStateMapper().putStateModelLocations(block);
        if (locations == null || locations.isEmpty()) return;
        // a location a state of the metadata also uses is that state's model
        java.util.Set<String> metaLocations = new java.util.HashSet<>();
        for (var e : locations.entrySet()) if (e.getValue() != null && StatesBridge1122.metaHolds(e.getKey())) metaLocations.add(e.getValue().toString());
        java.util.Set<String> done = new java.util.HashSet<>();
        int n = 0;
        for (IBlockState state : extra) {
            var location = locations.get(state);
            if (location == null) continue;
            String name = location.toString();
            if (metaLocations.contains(name) || !done.add(name)) continue;
            String key = "state_" + n++;
            Res r;
            try {
                r = multipart ? resolveMultipart(original, id, state, key, out) : resolveState(original, id, state, mapped == null ? null : mapped.get(state), key, out);
            } catch (RuntimeException e) {
                continue;
            }
            if (r.fallback != null) continue; // nothing better than the model of its metadata
            String renderType = renderTypeOf(block, state, r, out);
            List<JsonElement> all = new ArrayList<>();
            if (r.model != null) all.add(renderType == null ? r.model : withRenderType(r.model, renderType, id, out));
            for (JsonElement part : r.parts) all.add(renderType == null ? part : withRenderType(part, renderType, id, out));
            int meta;
            try {
                meta = block.getMetaFromState(state) & 15;
            } catch (RuntimeException e) {
                meta = 0;
            }
            Res metaRes = results.get(meta);
            if (metaRes != null) {
                List<JsonElement> metaAll = new ArrayList<>();
                if (metaRes.model != null) metaAll.add(metaRes.model);
                metaAll.addAll(metaRes.parts);
                if (GSON.toJson(metaAll).equals(GSON.toJson(all))) continue;
            }
            ResourceLocation model = stateModelFile(id, key, all, out);
            if (model != null) STATE_MODELS_BUILDING.put(name, model);
        }
    }

    /** One model of a state's variant: its model as it is, a wrapper with the rotation, or a composite of the variant and its submodels. */
    private static ResourceLocation stateModelFile(ResourceLocation blockId, String key, List<JsonElement> variants, Map<String, Supplier<byte[]>> out) {
        List<JsonObject> pieces = new ArrayList<>();
        for (JsonElement v : variants) {
            // a weighted variant: its first alternative (a standalone model has no random pick)
            while (v != null && v.isJsonArray()) v = v.getAsJsonArray().isEmpty() ? null : v.getAsJsonArray().get(0);
            if (v != null && v.isJsonObject() && v.getAsJsonObject().has("model") && v.getAsJsonObject().get("model").isJsonPrimitive()) pieces.add(v.getAsJsonObject());
        }
        if (pieces.isEmpty()) return null;
        String first = pieces.get(0).get("model").getAsString();
        if (pieces.size() == 1 && rotationOf(pieces.get(0)) == null) return ResourceLocation.tryParse(first);
        JsonObject file;
        if (pieces.size() == 1) {
            file = rotated(pieces.get(0));
        } else {
            file = new JsonObject();
            file.addProperty("parent", first); // the particle texture and the item transforms
            file.addProperty("loader", "neoforge:composite");
            JsonObject children = new JsonObject();
            for (int i = 0; i < pieces.size(); i++) children.add("part" + i, rotated(pieces.get(i)));
            file.add("children", children);
        }
        String path = "block/lc1122/state/" + blockId.getPath() + "_" + Ids.normalizePath(key);
        byte[] bytes = GSON.toJson(file).getBytes(StandardCharsets.UTF_8);
        out.put("assets/" + blockId.getNamespace() + "/models/" + path + ".json", () -> bytes);
        return ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), path);
    }

    /**
     * A model drawing the variant's model turned as a blockstate variant turns it: x/y as BlockModelRotation and, unlike a root transform,
     * uvlock (stairs corners, logs; see VariantModel1122). Only the client bakes models, so the server never reads the loader name.
     */
    private static JsonObject rotated(JsonObject variant) {
        int x = variant.has("x") && variant.get("x").isJsonPrimitive() ? variant.get("x").getAsInt() : 0;
        int y = variant.has("y") && variant.get("y").isJsonPrimitive() ? variant.get("y").getAsInt() : 0;
        if (x % 90 == 0 && y % 90 == 0) {
            JsonObject model = new JsonObject();
            model.addProperty("loader", "legacyconnector:variant1122");
            model.addProperty("model", variant.get("model").getAsString());
            if (x != 0) model.addProperty("x", x);
            if (y != 0) model.addProperty("y", y);
            if (variant.has("uvlock")) model.add("uvlock", variant.get("uvlock"));
            return model;
        }
        return transformed(variant);
    }

    /** A model with the variant's model as parent and its x/y rotation as root transform (a blockstate turns the model as BlockModelRotation does). */
    private static JsonObject transformed(JsonObject variant) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", variant.get("model").getAsString());
        JsonArray rotation = rotationOf(variant);
        if (rotation != null) {
            JsonObject transform = new JsonObject();
            transform.addProperty("origin", "center");
            transform.add("rotation", rotation);
            model.add("transform", transform);
        }
        return model;
    }

    /** The rotation of a variant as NeoForge's transform rotation (BlockModelRotation: y then x, both negated), or null for none. */
    private static JsonArray rotationOf(JsonObject variant) {
        int x = variant.has("x") && variant.get("x").isJsonPrimitive() ? variant.get("x").getAsInt() : 0;
        int y = variant.has("y") && variant.get("y").isJsonPrimitive() ? variant.get("y").getAsInt() : 0;
        if (x == 0 && y == 0) return null;
        JsonArray rotation = new JsonArray();
        if (y != 0) {
            JsonObject r = new JsonObject();
            r.addProperty("y", -y);
            rotation.add(r);
        }
        if (x != 0) {
            JsonObject r = new JsonObject();
            r.addProperty("x", -x);
            rotation.add(r);
        }
        return rotation;
    }

    private static String modelOf(JsonElement variant) {
        while (variant != null && variant.isJsonArray() && variant.getAsJsonArray().size() > 0) variant = variant.getAsJsonArray().get(0);
        return variant != null && variant.isJsonObject() && variant.getAsJsonObject().has("model") ? variant.getAsJsonObject().get("model").getAsString() : null;
    }

    private static int agreement(IBlockState a, IBlockState def) {
        int n = 0;
        for (Map.Entry<IProperty<?>, Comparable<?>> e : a.getProperties().entrySet()) {
            if (e.getValue().equals(def.getProperties().get(e.getKey()))) n++;
        }
        return n;
    }

    private static TreeMap<String, String> propsOf(IBlockState state) {
        TreeMap<String, String> props = new TreeMap<>();
        for (Map.Entry<IProperty<?>, Comparable<?>> e : state.getProperties().entrySet()) {
            props.put(e.getKey().getName(), nameOf(e.getKey(), e.getValue()));
        }
        return props;
    }

    private static String keyOf(Map<String, String> props) {
        StringBuilder key = new StringBuilder();
        for (var e : props.entrySet()) {
            if (key.length() > 0) key.append(',');
            key.append(e.getKey()).append('=').append(e.getValue());
        }
        return key.length() == 0 ? "normal" : key.toString();
    }

    private static Res resolveState(JsonObject original, ResourceLocation id, IBlockState state,
            legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelResourceLocation mapped, String key, Map<String, Supplier<byte[]>> out) {
        TreeMap<String, String> props = propsOf(state);
        String exact = keyOf(props);
        Acc acc = original == null ? null : merge(original, props, List.of(exact));
        String reason = original == null ? "no blockstate file" : null;
        Boolean markerOverride = null;
        ResourceLocation mappedTo = mapped == null ? null
                : ResourceLocation.fromNamespaceAndPath(Ids.namespace(mapped.getNamespace()), Ids.normalizePath(mapped.getPath()));
        // a mapper naming another blockstate file than the block's own decides the model (Forge looked up only the mapper's location)
        boolean otherFile = mappedTo != null && !mappedTo.equals(id) && readOriginal(mappedTo) != null;
        // the mapper names a variant of the block's own file that the state's property keys do not reach (Chisel: files keyed by the variation's name,
        // the defaults alone would draw a bare cube_all)
        boolean mappedVariant = mapped != null && !otherFile && original != null && original.has("variants") && original.get("variants").isJsonObject()
                && original.getAsJsonObject("variants").has(mapped.getVariant()) && !original.getAsJsonObject("variants").has(exact);
        if ((!usable(acc) || otherFile || mappedVariant) && mapped != null) {
            // a block whose states are mapped in code (ModelLoader.setCustomStateMapper): the mapper names the blockstate file and variant
            ResourceLocation file = mappedTo;
            JsonObject mappedFile = readOriginal(file);
            if (mappedFile != null) {
                // Forge looked the variant up by the mapper's variant string ("type=plastic" for a block whose state has no such property)
                TreeMap<String, String> viaProps = new TreeMap<>(props);
                for (String part : mapped.getVariant().split(",")) {
                    int eq = part.indexOf('=');
                    if (eq > 0) viaProps.put(part.substring(0, eq), part.substring(eq + 1));
                }
                Acc viaMapper = merge(mappedFile, viaProps, List.of(mapped.getVariant(), exact));
                if (usable(viaMapper) || acc == null) {
                    acc = viaMapper;
                    // the textures of the variant come from the mapper's file: whether they apply depends on that file's marker, not the block's own
                    markerOverride = mappedFile.has("forge_marker");
                }
            } else {
                reason = "state mapper file " + file + " not found";
            }
        }
        if (markerOverride == null) return finish(acc, id, key, reason, out);
        boolean before = FORGE_MARKER.get();
        FORGE_MARKER.set(markerOverride);
        try {
            return finish(acc, id, key, reason, out);
        } finally {
            FORGE_MARKER.set(before);
        }
    }

    private static boolean usable(Acc acc) {
        return acc != null && (!acc.subs.isEmpty() || (!acc.alts.isEmpty() && acc.alts.get(0).has("model")));
    }

    /** Builds the model JSON of a merged variant, or a plain cube when no part of it can be drawn. */
    private static Res finish(Acc acc, ResourceLocation id, String key, String reason, Map<String, Supplier<byte[]>> out) {
        Res r = new Res();
        if (acc == null || acc.alts.isEmpty()) {
            r.fallback = reason != null ? reason : "no variant";
            r.model = fallbackModel(id, null, key, out);
            return r;
        }
        JsonArray alts = new JsonArray();
        String why = null;
        int i = 0;
        for (JsonObject a : acc.alts) {
            JsonObject forge = a;
            if (a.has("model") && a.get("model").isJsonPrimitive() && a.get("model").getAsString().toLowerCase(java.util.Locale.ROOT).startsWith("forge:")) {
                forge = customLoader(a, id, 0);
                if (forge == null) why = "custom loader " + a.get("model").getAsString();
            }
            JsonObject m = forge == null || !forge.has("model") ? null : blockModels(forge, id, key + (acc.alts.size() > 1 ? "_" + i : ""), out);
            if (m != null) {
                alts.add(m);
            } else if (why == null) {
                why = forge != null && forge.has("model") ? "model " + forge.get("model").getAsString() + " not found" : "variant without a model";
            }
            i++;
        }
        for (JsonObject sub : acc.subs) {
            // a submodel draws with the textures of the variant it belongs to (RS' cable extensions use the file's "cable" and "border"), its own on top
            JsonObject base = acc.alts.get(0);
            if (base.has("textures") && base.get("textures").isJsonObject()) {
                JsonObject textures = base.getAsJsonObject("textures").deepCopy();
                if (sub.has("textures") && sub.get("textures").isJsonObject()) for (var t : sub.getAsJsonObject("textures").entrySet()) textures.add(t.getKey(), t.getValue());
                sub = sub.deepCopy();
                sub.add("textures", textures);
            }
            JsonObject m = sub.has("model") ? blockModels(sub, id, key + "_sub" + r.parts.size(), out) : null;
            if (m != null) r.parts.add(m);
        }
        if (alts.size() == acc.alts.size() && alts.size() > 0) {
            r.model = alts.size() == 1 ? alts.get(0) : alts;
        } else if (!r.parts.isEmpty() && !acc.alts.get(0).has("model")) {
            r.model = null; // only submodels
        } else {
            r.fallback = why != null ? why : reason != null ? reason : "no usable model";
            r.model = fallbackModel(id, acc.alts.get(0), key, out);
        }
        String m = modelOf(r.model);
        r.empty = m != null && java.util.regex.Pattern.compile("(^|[/_:])(empty|nothing|air)$").matcher(m).find();
        return r;
    }

    // ------------------------------------------------------------------------------------------------------------------------------
    // Forge blockstate v1: merging

    /** The variant of a state: defaults, "normal", the exact keys, then each property's table entry in file order. */
    private static Acc merge(JsonObject file, Map<String, String> props, List<String> exactKeys) {
        Acc acc = new Acc();
        JsonObject defaults = file.has("defaults") && file.get("defaults").isJsonObject() ? file.getAsJsonObject("defaults") : null;
        acc.alts.add(defaults == null ? new JsonObject() : defaults.deepCopy());
        JsonObject variants = file.has("variants") && file.get("variants").isJsonObject() ? file.getAsJsonObject("variants") : null;
        if (variants == null) return acc;
        boolean applied = false;
        String used = null;
        for (String key : exactKeys) {
            if (variants.has(key) && !key.contains("inventory")) {
                apply(acc, variants.get(key));
                applied = true;
                used = key;
                break;
            }
        }
        if (!applied && variants.has("normal")) {
            apply(acc, variants.get("normal"));
            applied = true;
        }
        for (var e : variants.entrySet()) {
            String k = e.getKey();
            if (k.equals("normal") || k.equals(used) || k.contains("inventory")) continue;
            if (k.contains("=")) {
                if (subsetMatches(k, props)) {
                    apply(acc, e.getValue());
                    applied = true;
                }
                continue;
            }
            String value = props.get(k);
            if (value == null || !e.getValue().isJsonObject()) continue;
            JsonElement sub = e.getValue().getAsJsonObject().get(value);
            if (sub != null) {
                apply(acc, sub);
                applied = true;
            }
        }
        if (!applied) {
            // Forge also took the bare value of a block's only varying property as the variant name ("variants": {"array": ...} for variation=array)
            for (String value : props.values()) {
                if (variants.has(value) && !props.containsKey(value) && !value.equals("normal")) {
                    apply(acc, variants.get(value));
                    break;
                }
            }
        }
        if (!applied && variants.size() == 1) {
            // a file with a single variant of another name (a fluid block's "yellorium"): that is the block's model
            var only = variants.entrySet().iterator().next();
            if (!props.containsKey(only.getKey()) || !only.getValue().isJsonObject() || only.getValue().getAsJsonObject().has("model")) apply(acc, only.getValue());
        }
        return acc;
    }

    private static boolean subsetMatches(String key, Map<String, String> props) {
        for (String part : key.split(",")) {
            int eq = part.indexOf('=');
            if (eq < 0) continue;
            if (!part.substring(eq + 1).equals(props.get(part.substring(0, eq)))) return false;
        }
        return true;
    }

    private static void apply(Acc acc, JsonElement over) {
        if (over.isJsonArray()) {
            JsonArray arr = over.getAsJsonArray();
            if (arr.size() == 0) return;
            if (arr.size() == 1) {
                applyObject(acc, arr.get(0));
            } else if (acc.alts.size() == 1) {
                JsonObject base = acc.alts.get(0);
                acc.alts = new ArrayList<>();
                for (JsonElement e : arr) {
                    JsonObject copy = base.deepCopy();
                    if (e.isJsonObject()) mergeInto(copy, e.getAsJsonObject(), acc, acc.alts.isEmpty());
                    acc.alts.add(copy);
                }
            } else {
                for (int i = 0; i < acc.alts.size(); i++) {
                    JsonElement e = arr.get(i % arr.size());
                    if (e.isJsonObject()) mergeInto(acc.alts.get(i), e.getAsJsonObject(), acc, i == 0);
                }
            }
        } else {
            applyObject(acc, over);
        }
    }

    private static void applyObject(Acc acc, JsonElement over) {
        if (!over.isJsonObject()) return;
        for (int i = 0; i < acc.alts.size(); i++) mergeInto(acc.alts.get(i), over.getAsJsonObject(), acc, i == 0);
    }

    /** Textures and custom data merge, rotations add, submodels accumulate, every other key replaces. */
    private static void mergeInto(JsonObject into, JsonObject over, Acc acc, boolean collectSubs) {
        boolean sub = over.has("submodel");
        if (sub && collectSubs) {
            JsonElement sm = over.get("submodel");
            if (sm.isJsonPrimitive()) {
                JsonObject entry = new JsonObject();
                entry.addProperty("model", sm.getAsString());
                copyRotation(entry, over);
                if (over.has("uvlock")) entry.add("uvlock", over.get("uvlock"));
                if (over.has("textures")) entry.add("textures", over.get("textures").deepCopy());
                acc.subs.add(entry);
            } else if (sm.isJsonObject()) {
                for (var e : sm.getAsJsonObject().entrySet()) {
                    if (!e.getValue().isJsonObject()) continue;
                    JsonObject entry = e.getValue().getAsJsonObject().deepCopy();
                    acc.subs.add(entry);
                }
            }
        }
        for (var e : over.entrySet()) {
            String k = e.getKey();
            JsonElement v = e.getValue();
            if (k.equals("submodel")) continue;
            if (sub && (k.equals("x") || k.equals("y") || k.equals("transform"))) continue; // they turn the submodel
            switch (k) {
                case "textures", "custom" -> {
                    if (!v.isJsonObject()) break;
                    JsonObject target = into.has(k) && into.get(k).isJsonObject() ? into.getAsJsonObject(k) : new JsonObject();
                    for (var t : v.getAsJsonObject().entrySet()) target.add(t.getKey(), t.getValue());
                    into.add(k, target);
                }
                case "x", "y" -> addAngle(into, k, v.isJsonPrimitive() ? v.getAsInt() : 0);
                case "transform" -> {
                    JsonObject rot = new JsonObject();
                    JsonObject only = new JsonObject();
                    only.add("transform", v);
                    copyRotation(rot, only);
                    for (var r : rot.entrySet()) addAngle(into, r.getKey(), r.getValue().getAsInt());
                }
                default -> into.add(k, v.deepCopy());
            }
        }
    }

    private static void addAngle(JsonObject into, String axis, int angle) {
        int sum = (into.has(axis) ? into.get(axis).getAsInt() : 0) + angle;
        into.addProperty(axis, sum);
    }

    /**
     * The x/y rotation of a variant as vanilla names it: its own "x"/"y", or a Forge transform {@code {"rotation": {"y": 90}}} (counter-clockwise,
     * so vanilla's y is its negative).
     */
    private static void copyRotation(JsonObject to, JsonObject from) {
        int x = from.has("x") && from.get("x").isJsonPrimitive() ? from.get("x").getAsInt() : 0;
        int y = from.has("y") && from.get("y").isJsonPrimitive() ? from.get("y").getAsInt() : 0;
        if (from.has("transform") && from.get("transform").isJsonObject() && from.getAsJsonObject("transform").has("rotation")
                && from.getAsJsonObject("transform").get("rotation").isJsonObject()) {
            JsonObject rot = from.getAsJsonObject("transform").getAsJsonObject("rotation");
            if (rot.has("x") && rot.get("x").isJsonPrimitive()) x += (int) Math.round(rot.get("x").getAsDouble());
            if (rot.has("y") && rot.get("y").isJsonPrimitive()) y -= (int) Math.round(rot.get("y").getAsDouble());
        }
        if (x != 0) to.addProperty("x", x);
        if (y != 0) to.addProperty("y", y);
    }

    // ------------------------------------------------------------------------------------------------------------------------------
    // custom loaders and multipart

    /** "forge:multi-layer" names the variant ("mod:file#variant") that its solid layer draws; "forge:fluid" is a water-like cube. */
    private static JsonObject customLoader(JsonObject a, ResourceLocation id, int depth) {
        String model = a.get("model").getAsString().toLowerCase(java.util.Locale.ROOT);
        JsonObject custom = a.has("custom") && a.get("custom").isJsonObject() ? a.getAsJsonObject("custom") : new JsonObject();
        if (model.equals("forge:fluid")) {
            JsonObject m = new JsonObject();
            m.addProperty("model", "minecraft:cube_all"); // blockModels puts the block/ folder in front
            JsonObject textures = new JsonObject();
            textures.addProperty("all", "minecraft:block/water_still");
            m.add("textures", textures);
            return m;
        }
        if (depth > 3 || !model.equals("forge:multi-layer")) return null;
        for (String layer : new String[] {"base", "Solid", "Cutout", "Cutout_Mipped", "Translucent"}) {
            if (!custom.has(layer) || !custom.get(layer).isJsonPrimitive()) continue;
            String ref = custom.get(layer).getAsString();
            int hash = ref.indexOf('#');
            String file = hash < 0 ? ref : ref.substring(0, hash);
            String variant = hash < 0 ? "normal" : ref.substring(hash + 1);
            int colon = file.indexOf(':');
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(colon < 0 ? id.getNamespace() : Ids.namespace(file.substring(0, colon)),
                    Ids.normalizePath(colon < 0 ? file : file.substring(colon + 1)));
            JsonObject other = readOriginal(rl);
            if (other == null) continue;
            Acc acc = merge(other, new TreeMap<>(), List.of(variant));
            if (acc.alts.isEmpty() || !acc.alts.get(0).has("model")) continue;
            JsonObject b = acc.alts.get(0);
            if (b.get("model").getAsString().toLowerCase(java.util.Locale.ROOT).startsWith("forge:")) {
                b = customLoader(b, id, depth + 1);
                if (b == null) continue;
            }
            // the rotation chosen by the state (facing) turns the layer's model
            for (String axis : new String[] {"x", "y"}) {
                if (a.has(axis)) addAngle(b, axis, a.get(axis).getAsInt());
            }
            JsonObject rot = new JsonObject();
            copyRotation(rot, a);
            for (var r : rot.entrySet()) if (!a.has(r.getKey())) addAngle(b, r.getKey(), r.getValue().getAsInt());
            return b;
        }
        return null;
    }

    private static Res resolveMultipart(JsonObject original, ResourceLocation id, IBlockState state, String key, Map<String, Supplier<byte[]>> out) {
        TreeMap<String, String> props = propsOf(state);
        Res r = new Res();
        int n = 0;
        for (JsonElement e : original.getAsJsonArray("multipart")) {
            if (!e.isJsonObject()) continue;
            JsonObject part = e.getAsJsonObject();
            if (part.has("when") && part.get("when").isJsonObject() && !whenMatches(part.getAsJsonObject("when"), props)) continue;
            JsonElement apply = part.get("apply");
            if (apply == null) continue;
            JsonElement resolved;
            if (apply.isJsonArray()) {
                JsonArray arr = new JsonArray();
                for (JsonElement a : apply.getAsJsonArray()) {
                    JsonObject m = a.isJsonObject() ? blockModels(normalizeApply(a.getAsJsonObject()), id, key + "_p" + n, out) : null;
                    if (m != null) arr.add(m);
                }
                resolved = arr.size() == 0 ? null : arr;
            } else {
                resolved = apply.isJsonObject() ? blockModels(normalizeApply(apply.getAsJsonObject()), id, key + "_p" + n, out) : null;
            }
            n++;
            if (resolved == null) {
                r.fallback = "multipart model not found";
                continue;
            }
            r.parts.add(resolved);
        }
        if (r.parts.isEmpty() && r.fallback != null) r.parts.add(fallbackModel(id, null, key, out));
        // no part applies to this state (a wall or fence without connections draws nothing in a connection-keyed file): another state of the
        // metadata, one that draws something, stands for the meta (the wall's post)
        r.empty = r.parts.isEmpty();
        return r;
    }

    private static JsonObject normalizeApply(JsonObject apply) {
        JsonObject copy = new JsonObject();
        mergeInto(copy, apply, new Acc(), false);
        return copy;
    }

    private static boolean whenMatches(JsonObject when, Map<String, String> props) {
        if (when.has("OR") && when.get("OR").isJsonArray()) {
            for (JsonElement e : when.getAsJsonArray("OR")) if (e.isJsonObject() && whenMatches(e.getAsJsonObject(), props)) return true;
            return false;
        }
        if (when.has("AND") && when.get("AND").isJsonArray()) {
            for (JsonElement e : when.getAsJsonArray("AND")) if (e.isJsonObject() && !whenMatches(e.getAsJsonObject(), props)) return false;
            return true;
        }
        for (var e : when.entrySet()) {
            if (!e.getValue().isJsonPrimitive()) return false;
            String have = props.get(e.getKey());
            if (have == null || !Arrays.asList(e.getValue().getAsString().split("\\|")).contains(have)) return false;
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------------------------------------------
    // render types: 1.12 alpha-tested every terrain layer (mods picked a layer with canRenderInLayer); a modern model needs a render_type to
    // discard transparent texels, else they draw black on the solid layer

    private static final Map<String, Boolean> TRANSPARENT_TEXTURES = new java.util.concurrent.ConcurrentHashMap<>();

    /** What a model's parent chain tells: its textures, and whether it must be left alone (custom loader, builtin parent, a render type of its own). */
    private static final class Chain {
        final java.util.Set<String> textures = new java.util.LinkedHashSet<>();
        boolean blocked;
    }

    /** The render type the legacy block asks for in this state ({@code minecraft:cutout}, ...), or null for the solid layer without transparent texels. */
    private static String renderTypeOf(Block block, IBlockState state, Res res, Map<String, Supplier<byte[]>> out) {
        if (state == null) return null;
        boolean solid = false;
        boolean cutout = false;
        boolean mipped = false;
        boolean translucent = false;
        try {
            var layers = legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer.values();
            for (var layer : layers) {
                if (!block.canRenderInLayer(state, layer)) continue;
                switch (layer) {
                    case SOLID -> solid = true;
                    case CUTOUT -> cutout = true;
                    case CUTOUT_MIPPED -> mipped = true;
                    case TRANSLUCENT -> translucent = true;
                }
            }
        } catch (RuntimeException | LinkageError e) {
            solid = true; // a layer check that needs a world: the solid default
        }
        if (translucent && !solid && !cutout && !mipped) return "minecraft:translucent";
        if (cutout) return "minecraft:cutout";
        if (mipped) return "minecraft:cutout_mipped";
        if (translucent) return "minecraft:cutout";
        // the solid layer: a texel that is not opaque would draw black, so the model is cut out
        List<JsonElement> all = new ArrayList<>();
        if (res.model != null) all.add(res.model);
        all.addAll(res.parts);
        for (JsonElement e : all) {
            for (String model : modelsOf(e)) {
                Chain chain = new Chain();
                chainOf(model, out, chain, 0);
                if (chain.blocked) continue;
                for (String texture : chain.textures) if (hasTransparentTexels(texture)) return "minecraft:cutout";
            }
        }
        return null;
    }

    /**
     * Textures named by the models of a blockstate outside the folders the block atlas scans (a 1.12 model may name any texture file,
     * "mod:model/apothecary/side": Forge's ModelLoader stitched whatever the models used). The client adds them to the atlas.
     */
    public static final java.util.Set<String> MODEL_TEXTURES = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static void collectModelTextures(Res res, Map<String, Supplier<byte[]>> out) {
        List<JsonElement> all = new ArrayList<>();
        if (res.model != null) all.add(res.model);
        all.addAll(res.parts);
        for (JsonElement e : all) {
            for (String model : modelsOf(e)) {
                Chain chain = new Chain();
                chainOf(model, out, chain, 0);
                for (String texture : chain.textures) {
                    int colon = texture.indexOf(':');
                    if (colon < 0) continue;
                    String path = texture.substring(colon + 1);
                    if (texture.startsWith("minecraft:") || path.startsWith("blocks/") || path.startsWith("items/") || path.startsWith("block/") || path.startsWith("item/")) continue;
                    if (textureExists(texture)) MODEL_TEXTURES.add(texture);
                }
            }
        }
    }

    private static List<String> modelsOf(JsonElement variant) {
        List<String> models = new ArrayList<>();
        if (variant == null) return models;
        if (variant.isJsonArray()) {
            for (JsonElement e : variant.getAsJsonArray()) models.addAll(modelsOf(e));
        } else if (variant.isJsonObject() && variant.getAsJsonObject().has("model") && variant.getAsJsonObject().get("model").isJsonPrimitive()) {
            models.add(variant.getAsJsonObject().get("model").getAsString());
        }
        return models;
    }

    /** A model id as "namespace:path" (path under models/) to its JSON, from this pack or the mods; null when it does not exist. */
    private static JsonObject modelJson(String model, Map<String, Supplier<byte[]>> out) {
        int colon = model.indexOf(':');
        String ns = colon < 0 ? "minecraft" : model.substring(0, colon);
        String file = "assets/" + ns + "/models/" + (colon < 0 ? model : model.substring(colon + 1)) + ".json";
        Supplier<byte[]> generated = out.get(file);
        if (generated != null) {
            try {
                JsonElement root = JsonParser.parseString(new String(generated.get(), StandardCharsets.UTF_8));
                return root.isJsonObject() ? root.getAsJsonObject() : null;
            } catch (RuntimeException e) {
                return null;
            }
        }
        return readJson(file);
    }

    /** The property item models select a per-stack model on: its value is the 1-based position of the model the item's mesh definition returned. */
    public static final ResourceLocation MESH_PROPERTY = ResourceLocation.fromNamespaceAndPath("legacyconnector", "mesh");

    /** The modern model id of a legacy model location: vanilla's 1.12 item models that were renamed map to the new names. */
    public static String itemModelTarget(String namespace, String path) {
        String ns = Ids.namespace(namespace);
        String p = Ids.normalizePath(path);
        if (ns.equals("minecraft") && p.equals("spawn_egg")) return "minecraft:item/template_spawn_egg";
        return ns + ":item/" + p;
    }

    /**
     * An item whose mod gave it an {@code ItemMeshDefinition} (a model chosen per stack, from its tag or capabilities: TF2 weapons are one
     * item per weapon class showing the model of the weapon named in the stack) gets a model with one {@code overrides} entry per model
     * the mod registered as a variant, selected by {@link #MESH_PROPERTY}, which {@code ItemProperties1122} computes by asking the mesh
     * definition. Returns false for an item without a mesh definition or variants.
     */
    /** Ids ("namespace:path") of the modern items whose model {@link #meshModel} replaces with a wrapper. */
    private static java.util.Set<String> meshIds = java.util.Set.of();

    private static java.util.Set<String> meshItemIds() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (legacyconnector.v1_12_2.net.minecraft.item.Item item : org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.modItems()) {
            if (item.getRegistryName() == null || legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.mcc$mesh(item) == null
                    || legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.mcc$variantList(item).isEmpty()) continue;
            for (var modern : org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.variants(item).values()) {
                ResourceLocation key = modern == null ? null : BuiltInRegistries.ITEM.getKey(modern);
                if (key != null) ids.add(key.toString());
            }
        }
        return ids;
    }

    /**
     * The model a wrapper points at: when it is the model file of an item that a wrapper replaces (an item named like the model it shows:
     * the weapon class "minigun" shows models/item/minigun.json), the mod's own file is kept under another name.
     */
    private static String meshTarget(String target, Map<String, Supplier<byte[]>> out) {
        int colon = target.indexOf(':');
        String ns = target.substring(0, colon);
        String path = target.substring(colon + 1);
        if (!path.startsWith("item/") || !meshIds.contains(ns + ":" + path.substring(5)) || !modelExists(ns, path)) return target;
        String moved = "item/lc1122/mesh_" + path.substring(5);
        String file = "assets/" + ns + "/models/" + moved + ".json";
        if (!out.containsKey(file)) {
            JsonObject json = readJson("assets/" + ns + "/models/" + path + ".json");
            if (json == null) return target;
            byte[] bytes = GSON.toJson(json).getBytes(StandardCharsets.UTF_8);
            out.put(file, () -> bytes);
        }
        return ns + ":" + moved;
    }

    private static boolean meshModel(legacyconnector.v1_12_2.net.minecraft.item.Item item,
            legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelResourceLocation base, ResourceLocation id, Map<String, Supplier<byte[]>> out) {
        if (legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.mcc$mesh(item) == null) return false;
        var variants = legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.mcc$variantList(item);
        if (variants.isEmpty()) return false;
        JsonArray overrides = new JsonArray();
        String fallback = null;
        for (int i = 0; i < variants.size(); i++) {
            var v = variants.get(i);
            String target = meshTarget(itemModelTarget(v.getNamespace(), v.getPath()), out);
            int colon = target.indexOf(':');
            String ns = target.substring(0, colon);
            String path = target.substring(colon + 1);
            if (!ns.equals("minecraft") && !modelExists(ns, path) && !out.containsKey("assets/" + ns + "/models/" + path + ".json")) continue;
            if (fallback == null) fallback = target;
            JsonObject override = new JsonObject();
            JsonObject predicate = new JsonObject();
            predicate.addProperty(MESH_PROPERTY.toString(), i + 1);
            override.add("predicate", predicate);
            override.addProperty("model", target);
            overrides.add(override);
        }
        if (overrides.isEmpty()) return false;
        JsonObject model = new JsonObject();
        model.addProperty("parent", base != null && (base.getNamespace().equals("minecraft") || modelExists(Ids.namespace(base.getNamespace()), "item/" + Ids.normalizePath(base.getPath())))
                ? meshTarget(itemModelTarget(base.getNamespace(), base.getPath()), out) : fallback);
        model.add("overrides", overrides);
        byte[] bytes = GSON.toJson(model).getBytes(StandardCharsets.UTF_8);
        out.put("assets/" + id.getNamespace() + "/models/item/" + id.getPath() + ".json", () -> bytes);
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) org.kylepitcock.legacyconnector.v1_7_10.client.SharedItemModels.note(id, GSON.toJson(model));
        return true;
    }

    /**
     * 1.12 stitched every texture a model named, wherever the file lived ({@code "layer0": "mod:living/claymore"} is
     * {@code textures/living/claymore.png}); the modern block atlas only scans the folders it is told to. Walks the models of the pack's
     * blockstates and of every item (parents and item overrides too) and registers the textures outside {@code blocks/}, {@code items/},
     * {@code block/} and {@code item/} so the atlas gets a source for each.
     */
    public static void registerModelTextures(Map<String, Supplier<byte[]>> out) {
        java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (var e : out.entrySet()) {
            String key = e.getKey();
            if (!key.endsWith(".json") || !key.startsWith("assets/")) continue;
            int slash = key.indexOf('/', 7);
            if (slash < 0) continue;
            String ns = key.substring(7, slash);
            String rest = key.substring(slash + 1);
            if (ns.equals("minecraft")) continue;
            if (rest.startsWith("models/item/")) {
                queue.add(ns + ":" + rest.substring(7, rest.length() - 5));
            } else if (rest.startsWith("blockstates/")) {
                try {
                    collectModels(JsonParser.parseString(new String(e.getValue().get(), StandardCharsets.UTF_8)), ns, queue);
                } catch (RuntimeException ignored) {
                    // an unreadable blockstate has no textures to add
                }
            }
        }
        for (var item : org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.modItems()) {
            ResourceLocation name = item.getRegistryName() == null ? null : ResourceLocation.parse(Ids.namespace(item.getRegistryName().getNamespace()) + ":" + Ids.normalizePath(item.getRegistryName().getPath()));
            if (name != null && !name.getNamespace().equals("minecraft")) queue.add(name.getNamespace() + ":item/" + name.getPath());
        }
        int added = 0;
        while (!queue.isEmpty()) {
            String model = queue.poll();
            int colon = model.indexOf(':');
            String ns = colon < 0 ? "minecraft" : Ids.namespace(model.substring(0, colon));
            if (ns.equals("minecraft") || !seen.add(model)) continue;
            JsonObject json = modelJson(ns + ":" + Ids.normalizePath(colon < 0 ? model : model.substring(colon + 1)), out);
            if (json == null) continue;
            if (json.has("textures") && json.get("textures").isJsonObject()) {
                for (var t : json.getAsJsonObject("textures").entrySet()) {
                    if (!t.getValue().isJsonPrimitive()) continue;
                    String ref = t.getValue().getAsString();
                    int c = ref.indexOf(':');
                    if (c < 0 || ref.startsWith("#")) continue;
                    String tns = Ids.namespace(ref.substring(0, c));
                    String path = Ids.normalizePath(ref.substring(c + 1));
                    if (tns.equals("minecraft") || path.startsWith("blocks/") || path.startsWith("items/") || path.startsWith("block/") || path.startsWith("item/")) continue;
                    if (!resourceExists("assets/" + tns + "/textures/" + path + ".png")) continue;
                    org.kylepitcock.legacyconnector.v1_12_2.client.Sprites1122.register(tns + ":" + path);
                    added++;
                }
            }
            if (json.has("parent") && json.get("parent").isJsonPrimitive()) {
                String parent = json.get("parent").getAsString();
                int pc = parent.indexOf(':');
                String pns = pc < 0 ? ns : Ids.namespace(parent.substring(0, pc));
                String ppath = Ids.normalizePath(pc < 0 ? parent : parent.substring(pc + 1));
                // a bare parent is looked up in the model's own namespace first (item/generated and the like are vanilla's)
                if (pc < 0 && !modelExists(ns, ppath)) pns = "minecraft";
                queue.add(pns + ":" + ppath);
            }
            if (json.has("overrides") && json.get("overrides").isJsonArray()) {
                for (JsonElement o : json.getAsJsonArray("overrides")) {
                    if (o.isJsonObject() && o.getAsJsonObject().has("model") && o.getAsJsonObject().get("model").isJsonPrimitive()) {
                        String m = o.getAsJsonObject().get("model").getAsString();
                        queue.add((m.indexOf(':') < 0 ? ns + ":" : "") + m);
                    }
                }
            }
        }
        if (added > 0) LOG.info("1.12.2 model textures outside blocks/ and items/: {} added to the atlas", added);
    }

    /** Every model id a blockstate file names (variants, multipart), as "namespace:path". */
    private static void collectModels(JsonElement e, String ns, java.util.Collection<String> into) {
        if (e == null) return;
        if (e.isJsonArray()) {
            for (JsonElement x : e.getAsJsonArray()) collectModels(x, ns, into);
        } else if (e.isJsonObject()) {
            for (var entry : e.getAsJsonObject().entrySet()) {
                if (entry.getKey().equals("model") && entry.getValue().isJsonPrimitive()) {
                    String m = entry.getValue().getAsString();
                    into.add(m.indexOf(':') < 0 ? ns + ":" + m : m);
                } else {
                    collectModels(entry.getValue(), ns, into);
                }
            }
        }
    }

    private static void chainOf(String model, Map<String, Supplier<byte[]>> out, Chain chain, int depth) {
        if (depth > 12) return;
        int colon = model.indexOf(':');
        String ns = colon < 0 ? "minecraft" : model.substring(0, colon);
        if (ns.equals("minecraft")) return; // vanilla's parents (cube_all, block, ...) draw what their textures say
        JsonObject json = modelJson(model, out);
        if (json == null) return;
        if (json.has("loader") || json.has("render_type")) chain.blocked = true;
        if (json.has("textures") && json.get("textures").isJsonObject()) {
            for (var t : json.getAsJsonObject("textures").entrySet()) {
                if (t.getValue().isJsonPrimitive() && !t.getValue().getAsString().startsWith("#")) chain.textures.add(t.getValue().getAsString());
            }
        }
        if (json.has("parent") && json.get("parent").isJsonPrimitive()) {
            String parent = json.get("parent").getAsString().toLowerCase(java.util.Locale.ROOT);
            if (parent.startsWith("builtin/") || parent.startsWith("minecraft:builtin/")) {
                chain.blocked = true;
                return;
            }
            int pc = parent.indexOf(':');
            String pns = pc < 0 ? (modelExists(ns, Ids.normalizePath(parent)) ? ns : "minecraft") : Ids.namespace(parent.substring(0, pc));
            chainOf(pns + ":" + Ids.normalizePath(pc < 0 ? parent : parent.substring(pc + 1)), out, chain, depth + 1);
        }
    }

    /** True when the texture has any texel that is not fully opaque (cached per texture). */
    private static boolean hasTransparentTexels(String ref) {
        int colon = ref.indexOf(':');
        String ns = colon < 0 ? "minecraft" : Ids.namespace(ref.substring(0, colon));
        String path = Ids.normalizePath(colon < 0 ? ref : ref.substring(colon + 1));
        if (ns.equals("minecraft")) return false;
        return TRANSPARENT_TEXTURES.computeIfAbsent(ns + ":" + path, k -> {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            String[] candidates = path.startsWith("blocks/") ? new String[] {"assets/" + ns + "/textures/" + path + ".png", "assets/" + ns + "/textures/block/" + path.substring(7) + ".png"}
                    : new String[] {"assets/" + ns + "/textures/" + path + ".png"};
            for (String file : candidates) {
                try (var in = loader.getResourceAsStream(file)) {
                    if (in == null) continue;
                    java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(in);
                    if (image == null) return false;
                    if (!image.getColorModel().hasAlpha()) return false;
                    int[] row = new int[image.getWidth()];
                    for (int y = 0; y < image.getHeight(); y++) {
                        image.getRGB(0, y, image.getWidth(), 1, row, 0, image.getWidth());
                        for (int argb : row) if ((argb >>> 24) != 255) return true;
                    }
                    return false;
                } catch (IOException | RuntimeException e) {
                    return false;
                }
            }
            return false;
        });
    }

    /** A copy of the variant (object or weighted array) whose models are wrappers with the render type; models that must stay as they are are kept. */
    private static JsonElement withRenderType(JsonElement variant, String renderType, ResourceLocation blockId, Map<String, Supplier<byte[]>> out) {
        if (variant == null) return null;
        if (variant.isJsonArray()) {
            JsonArray array = new JsonArray();
            for (JsonElement e : variant.getAsJsonArray()) array.add(withRenderType(e, renderType, blockId, out));
            return array;
        }
        if (!variant.isJsonObject() || !variant.getAsJsonObject().has("model") || !variant.getAsJsonObject().get("model").isJsonPrimitive()) return variant;
        JsonObject copy = variant.getAsJsonObject().deepCopy();
        String model = copy.get("model").getAsString();
        Chain chain = new Chain();
        chainOf(model, out, chain, 0);
        int colon = model.indexOf(':');
        if (chain.blocked || colon < 0 || model.startsWith("minecraft:") || model.contains("builtin/")) return copy;
        String path = "block/lc1122/rt/" + model.substring(0, colon) + "_" + Ids.normalizePath(model.substring(colon + 1)).replace('/', '_') + "_" + renderType.substring(10);
        String file = "assets/" + blockId.getNamespace() + "/models/" + path + ".json";
        if (!out.containsKey(file)) {
            JsonObject wrapper = new JsonObject();
            wrapper.addProperty("parent", model);
            wrapper.addProperty("render_type", renderType);
            byte[] bytes = GSON.toJson(wrapper).getBytes(StandardCharsets.UTF_8);
            out.put(file, () -> bytes);
        }
        copy.addProperty("model", blockId.getNamespace() + ":" + path);
        return copy;
    }

    // ------------------------------------------------------------------------------------------------------------------------------
    // models

    /** Whether a resource of the mods exists, by path: the same few thousand model and texture paths are asked once per block state. */
    private static final Map<String, Boolean> RESOURCE_EXISTS = new java.util.concurrent.ConcurrentHashMap<>();

    private static boolean modelExists(String namespace, String path) {
        return resourceExists("assets/" + namespace + "/models/" + path + ".json");
    }

    private static boolean resourceExists(String path) {
        Boolean known = RESOURCE_EXISTS.get(path);
        if (known != null) return known;
        boolean exists = Thread.currentThread().getContextClassLoader().getResource(path) != null;
        RESOURCE_EXISTS.put(path, exists);
        return exists;
    }

    /** True when the model file exists (in the mods or this pack) and so does every non-vanilla parent up its chain. */
    private static boolean modelResolves(String ns, String path, Map<String, Supplier<byte[]>> out, int depth) {
        if (ns.equals("minecraft")) return true;
        String file = "assets/" + ns + "/models/" + path + ".json";
        if (out.containsKey(file)) return true;
        if (depth > 12) return false;
        JsonObject m = readJson(file);
        if (m == null) return false;
        if (!m.has("parent") || !m.get("parent").isJsonPrimitive()) return true;
        String parent = m.get("parent").getAsString().toLowerCase(java.util.Locale.ROOT);
        if (parent.startsWith("builtin/") || parent.startsWith("minecraft:")) return true;
        int colon = parent.indexOf(':');
        String pns = colon < 0 ? null : Ids.namespace(parent.substring(0, colon));
        String ppath = Ids.normalizePath(colon < 0 ? parent : parent.substring(colon + 1));
        if (pns == null) pns = modelExists(ns, ppath) ? ns : "minecraft";
        return modelResolves(pns, ppath, out, depth + 1);
    }

    /**
     * A 1.12 blockstate names its models relative to models/block/ ("model": "livingwood" is models/block/livingwood.json; names without a namespace
     * are vanilla's unless the mod has such a file); 1.13 and later name the full path. A variant that overrides textures gets its own generated
     * model file (modern blockstate variants have no texture overrides). Returns null when the model does not exist.
     */
    private static final Map<String, String> VANILLA_MODEL_RENAMES = Map.ofEntries(
            Map.entry("block/half_slab", "block/slab"), Map.entry("block/upper_slab", "block/slab_top"),
            Map.entry("block/grass_path", "block/dirt_path"), Map.entry("block/slime", "block/slime_block"), Map.entry("block/grass", "block/grass_block"),
            Map.entry("block/door_bottom", "block/door_bottom_left"), Map.entry("block/door_top", "block/door_top_left"),
            Map.entry("block/door_bottom_rh", "block/door_bottom_right"), Map.entry("block/door_top_rh", "block/door_top_right"),
            Map.entry("block/wall_post", "block/template_wall_post"), Map.entry("block/wall_side", "block/template_wall_side"),
            Map.entry("block/fence_gate_closed", "block/template_fence_gate"), Map.entry("block/fence_gate_open", "block/template_fence_gate_open"),
            Map.entry("block/wall_gate_closed", "block/template_fence_gate_wall"), Map.entry("block/wall_gate_open", "block/template_fence_gate_wall_open"),
            Map.entry("block/pane_post", "block/template_glass_pane_post"), Map.entry("block/pane_side", "block/template_glass_pane_side"),
            Map.entry("block/pane_side_alt", "block/template_glass_pane_side_alt"), Map.entry("block/pane_noside", "block/template_glass_pane_noside"),
            Map.entry("block/pane_noside_alt", "block/template_glass_pane_noside_alt"),
            Map.entry("block/wooden_trapdoor_top", "block/template_orientable_trapdoor_top"), Map.entry("block/wooden_trapdoor_bottom", "block/template_orientable_trapdoor_bottom"),
            Map.entry("block/wooden_trapdoor_open", "block/template_orientable_trapdoor_open"),
            Map.entry("block/trapdoor_top", "block/template_orientable_trapdoor_top"), Map.entry("block/trapdoor_bottom", "block/template_orientable_trapdoor_bottom"),
            Map.entry("block/trapdoor_open", "block/template_orientable_trapdoor_open"),
            Map.entry("block/orientable_trapdoor_top", "block/template_orientable_trapdoor_top"), Map.entry("block/orientable_trapdoor_bottom", "block/template_orientable_trapdoor_bottom"),
            Map.entry("block/orientable_trapdoor_open", "block/template_orientable_trapdoor_open"),
            Map.entry("block/normal_torch", "block/template_torch"), Map.entry("block/normal_torch_wall", "block/template_torch_wall"),
            Map.entry("block/tall_grass", "block/short_grass"),
            Map.entry("block/waterlily", "block/lily_pad"), Map.entry("block/torch_wall", "block/wall_torch"),
            // iron doors (EnderIO's dark steel door) and the plain rail (Tinkers' wooden rail); the slots are the same
            Map.entry("block/iron_door_bottom", "block/iron_door_bottom_left"), Map.entry("block/iron_door_top", "block/iron_door_top_left"),
            Map.entry("block/iron_door_bottom_rh", "block/iron_door_bottom_right"), Map.entry("block/iron_door_top_rh", "block/iron_door_top_right"),
            Map.entry("block/normal_rail_flat", "block/rail_flat"), Map.entry("block/normal_rail_curved", "block/rail_curved"),
            Map.entry("block/normal_rail_raised_ne", "block/template_rail_raised_ne"), Map.entry("block/normal_rail_raised_sw", "block/template_rail_raised_sw"));

    private static final java.util.regex.Pattern VINE_MODEL = java.util.regex.Pattern.compile("vine_([1-4])?(u)?(_opposite)?");

    /**
     * The 1.12 vine models ("block/vine_1", "vine_2u", "vine_2_opposite"...) no longer exist in 1.13+; the same geometry (a quad of the "vine" texture 0.8
     * pixels inside the block per side, a ceiling quad for the "u" ones) written into the mod's namespace. Null when the name is not a vine model.
     * The loader generates the same models into the minecraft namespace for model files that name them as parent.
     */
    private static String vineModelPath(String name, String namespace, Map<String, Supplier<byte[]>> out) {
        java.util.regex.Matcher m = VINE_MODEL.matcher(name);
        if (!m.matches() || (m.group(1) == null && m.group(2) == null)) return null;
        String path = "block/lc1122/" + name;
        String file = "assets/" + namespace + "/models/" + path + ".json";
        if (out.containsKey(file)) return path;
        int sides = m.group(1) == null ? 0 : Integer.parseInt(m.group(1));
        boolean[] on = new boolean[4]; // north, east, south, west; vine_2_opposite is north and south
        if (m.group(3) != null) {
            on[0] = on[2] = true;
        } else {
            for (int i = 0; i < sides; i++) on[i] = true;
        }
        double[][] from = {{0, 0, 0.8}, {15.2, 0, 0}, {0, 0, 15.2}, {0.8, 0, 0}};
        double[][] to = {{16, 16, 0.8}, {15.2, 16, 16}, {16, 16, 15.2}, {0.8, 16, 16}};
        String[][] faces = {{"north", "south"}, {"west", "east"}, {"north", "south"}, {"west", "east"}};
        JsonArray elements = new JsonArray();
        for (int i = 0; i < 4; i++) {
            if (on[i]) elements.add(vineElement(from[i], to[i], faces[i][0], faces[i][1], new int[] {16, 0, 0, 16}));
        }
        if (m.group(2) != null) elements.add(vineElement(new double[] {0, 15.2, 0}, new double[] {16, 15.2, 16}, "up", "down", new int[] {0, 0, 16, 16}));
        JsonObject model = new JsonObject();
        model.addProperty("ambientocclusion", false);
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", "#vine");
        textures.addProperty("vine", "minecraft:block/vine");
        model.add("textures", textures);
        model.add("elements", elements);
        byte[] bytes = GSON.toJson(model).getBytes(StandardCharsets.UTF_8);
        out.put(file, () -> bytes);
        return path;
    }

    private static JsonObject vineElement(double[] from, double[] to, String face1, String face2, int[] uv1) {
        JsonObject element = new JsonObject();
        JsonArray f = new JsonArray();
        JsonArray t = new JsonArray();
        for (int i = 0; i < 3; i++) {
            f.add(from[i]);
            t.add(to[i]);
        }
        element.add("from", f);
        element.add("to", t);
        element.addProperty("shade", false);
        JsonObject faces = new JsonObject();
        for (int k = 0; k < 2; k++) {
            JsonObject face = new JsonObject();
            JsonArray uv = new JsonArray();
            int[] u = k == 0 ? uv1 : new int[] {0, 0, 16, 16};
            for (int v : u) uv.add(v);
            face.add("uv", uv);
            face.addProperty("texture", "#vine");
            face.addProperty("tintindex", 0);
            faces.add(k == 0 ? face1 : face2, face);
        }
        element.add("faces", faces);
        return element;
    }

    private static JsonObject blockModels(JsonObject source, ResourceLocation blockId, String key, Map<String, Supplier<byte[]>> out) {
        String defaultNamespace = blockId.getNamespace();
        JsonObject copy = new JsonObject();
        String model = null;
        for (var e : source.entrySet()) {
            String k = e.getKey();
            if (k.equals("model") && e.getValue().isJsonPrimitive()) {
                String m = e.getValue().getAsString().toLowerCase(java.util.Locale.ROOT);
                int colon = m.indexOf(':');
                String path = colon >= 0 ? m.substring(colon + 1) : m;
                String normalized = Ids.normalizePath(path);
                String ns = colon >= 0 ? m.substring(0, colon) : modelExists(defaultNamespace, "block/" + normalized) ? defaultNamespace : "minecraft";
                // a name that already carries the folder ("block/cube_all") would be models/block/block/..., which only exists by mistake
                if (Ids.namespace(ns).equals("minecraft") && normalized.startsWith("block/") && !normalized.startsWith("block/builtin/")) normalized = normalized.substring(6);
                model = Ids.namespace(ns) + ":block/" + normalized;
            } else if (k.equals("x") || k.equals("y")) {
                int angle = e.getValue().isJsonPrimitive() ? Math.floorMod((int) Math.round(Math.round(e.getValue().getAsDouble() / 90.0) * 90.0), 360) : 0;
                if (angle != 0) copy.addProperty(k, angle);
            } else if (k.equals("uvlock") || k.equals("weight")) {
                copy.add(k, e.getValue());
            }
        }
        if (model == null) return null;
        int c = model.indexOf(':');
        String modelNs = model.substring(0, c);
        String modelPath = model.substring(c + 1);
        if (modelPath.startsWith("builtin/") || modelPath.startsWith("block/builtin/")) {
            // "builtin/generated", "builtin/entity": the block is drawn by code (a model replaced in ModelBakeEvent, a TESR); the game's own model
            // is an empty one, so nothing is drawn if the mod does not replace it and no missing-model warning is logged per variant
            String emptyPath = "block/lc1122/builtin_empty";
            if (GENERATED_MODELS.putIfAbsent(blockId.getNamespace() + "|builtin_empty", emptyPath) == null) {
                JsonObject empty = new JsonObject();
                empty.add("elements", new JsonArray());
                String emptyJson = GSON.toJson(empty);
                byte[] emptyBytes = emptyJson.getBytes(StandardCharsets.UTF_8);
                out.put("assets/" + blockId.getNamespace() + "/models/" + emptyPath + ".json", () -> emptyBytes);
            }
            copy.addProperty("model", blockId.getNamespace() + ":" + emptyPath);
            return copy;
        }
        if (modelNs.equals("minecraft") && modelPath.startsWith("block/")) {
            String vine = vineModelPath(modelPath.substring(6), blockId.getNamespace(), out);
            if (vine != null) {
                modelNs = blockId.getNamespace();
                modelPath = vine;
                model = modelNs + ":" + modelPath;
            }
        }
        if (modelNs.equals("minecraft") && VANILLA_MODEL_RENAMES.containsKey(modelPath)) {
            // a vanilla model of 1.12 that 1.13 renamed (the loader renames the same ones in the mod's own model files)
            modelPath = VANILLA_MODEL_RENAMES.get(modelPath);
            model = "minecraft:" + modelPath;
        }
        if (ObjAssets1122.isObj(model)) {
            // a Forge OBJ model: NeoForge's OBJ loader draws it (with the variant's texture overrides and flip-v)
            if (!modelNs.equals("minecraft") && !resourceExists("assets/" + modelNs + "/models/" + modelPath)) return null;
            JsonObject textures = FORGE_MARKER.get() && source.has("textures") && source.get("textures").isJsonObject() ? source.getAsJsonObject("textures") : null;
            copy.addProperty("model", ObjAssets1122.model(modelNs, modelPath, textures, ObjAssets1122.custom(source),
                    textures == null ? null : blockId.getPath() + "_" + key, out));
            return copy;
        }
        if (!modelResolves(modelNs, modelPath, out, 0)) {
            // a model no json file describes may belong to a mod's own ModelLoader (Immersive Engineering's "smartmodel/connector"): the
            // variant is baked from that loader at ModelEvent (CustomModels1122), the blockstate gets a plain cube until then
            java.util.regex.Matcher meta = java.util.regex.Pattern.compile("meta_(\\d+)(_\\d+)?").matcher(key);
            if (meta.matches() && loaderAccepts(modelNs, modelPath)) {
                int n = Integer.parseInt(meta.group(1));
                if (LOADER_VARIANTS.stream().noneMatch(v -> v.block().equals(blockId) && v.meta() == n)) {
                    LOADER_VARIANTS.add(new LoaderVariant(blockId, n, modelNs, modelPath, source.deepCopy()));
                }
                return fallbackModel(blockId, source, key, out) instanceof JsonObject placeholder ? placeholder : null;
            }
            return null;
        }
        if (FORGE_MARKER.get() && source.has("textures") && source.get("textures").isJsonObject() && source.getAsJsonObject("textures").size() > 0) {
            JsonObject generated = new JsonObject();
            generated.addProperty("parent", model);
            JsonObject textures = new JsonObject();
            for (var t : source.getAsJsonObject("textures").entrySet()) {
                if (t.getValue().isJsonPrimitive()) textures.addProperty(t.getKey().startsWith("#") ? t.getKey().substring(1) : t.getKey(), lowerRef(t.getValue().getAsString()));
            }
            generated.add("textures", textures);
            String path = "block/lc1122/" + blockId.getPath() + "_" + Ids.normalizePath(key);
            String json = GSON.toJson(generated);
            String same = SHARE_MODELS ? GENERATED_MODELS.putIfAbsent(blockId.getNamespace() + "|" + json, path) : null;
            if (same != null) {
                path = same;
            } else {
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                out.put("assets/" + blockId.getNamespace() + "/models/" + path + ".json", () -> bytes);
            }
            model = blockId.getNamespace() + ":" + path;
        }
        copy.addProperty("model", model);
        return copy;
    }

    private static String lowerRef(String ref) {
        if (ref.startsWith("#")) return ref;
        int colon = ref.indexOf(':');
        String ns = colon < 0 ? "minecraft" : ref.substring(0, colon);
        String path = colon < 0 ? ref : ref.substring(colon + 1);
        String normalized = Ids.normalizePath(path);
        // vanilla textures are named the 1.12 way in a mod's blockstate ("blocks/dirt", "minecraft:blocks/slime")
        if (Ids.namespace(ns).equals("minecraft")) normalized = VanillaTextures1122.modern(normalized);
        return Ids.namespace(ns) + ":" + normalized;
    }

    /** A plain cube textured with the first texture the variant names (or the block's own texture, or gray concrete): never a missing model. */
    private static JsonElement fallbackModel(ResourceLocation id, JsonObject hint, String key, Map<String, Supplier<byte[]>> out) {
        // a block model named like the block (models/block/<name>.json) is the best guess for a block nothing else describes
        if (hint == null && modelResolves(id.getNamespace(), "block/" + id.getPath(), out, 0) && modelExists(id.getNamespace(), "block/" + id.getPath())) {
            JsonObject named = new JsonObject();
            named.addProperty("model", id.getNamespace() + ":block/" + id.getPath());
            return named;
        }
        String texture = pickTexture(id, hint);
        JsonObject generated = new JsonObject();
        generated.addProperty("parent", "minecraft:block/cube_all");
        JsonObject textures = new JsonObject();
        textures.addProperty("all", texture);
        generated.add("textures", textures);
        String path = "block/lc1122/" + id.getPath() + "_fallback_" + Ids.normalizePath(key);
        byte[] bytes = GSON.toJson(generated).getBytes(StandardCharsets.UTF_8);
        out.put("assets/" + id.getNamespace() + "/models/" + path + ".json", () -> bytes);
        JsonObject variant = new JsonObject();
        variant.addProperty("model", id.getNamespace() + ":" + path);
        return variant;
    }

    private static String pickTexture(ResourceLocation id, JsonObject hint) {
        if (hint != null && hint.has("textures") && hint.get("textures").isJsonObject()) {
            JsonObject textures = hint.getAsJsonObject("textures");
            List<String> order = new ArrayList<>(List.of("all", "texture", "side", "front", "top", "bottom", "particle"));
            for (String k : textures.keySet()) if (!order.contains(k)) order.add(k);
            for (String k : order) {
                if (!textures.has(k) || !textures.get(k).isJsonPrimitive()) continue;
                String ref = lowerRef(textures.get(k).getAsString());
                if (ref.startsWith("#") || !textureExists(ref)) continue;
                return ref;
            }
        }
        for (String candidate : new String[] {id.getNamespace() + ":blocks/" + id.getPath(), id.getNamespace() + ":blocks/" + id.getPath() + "_0",
                id.getNamespace() + ":block/" + id.getPath()}) {
            if (textureExists(candidate)) return candidate;
        }
        return "minecraft:block/gray_concrete";
    }

    private static boolean textureExists(String ref) {
        int colon = ref.indexOf(':');
        String ns = colon < 0 ? "minecraft" : ref.substring(0, colon);
        String path = colon < 0 ? ref : ref.substring(colon + 1);
        if (ns.equals("minecraft")) return !path.startsWith("blocks/") && !path.startsWith("items/");
        return resourceExists("assets/" + ns + "/textures/" + path + ".png")
                || (path.startsWith("blocks/") && resourceExists("assets/" + ns + "/textures/block/" + path.substring(7) + ".png"));
    }

    /** Whether the model {@code ns:path} (a path under models/) is in the mods or this pack; vanilla's models are assumed present. */
    private static boolean modelPresent(String ref, Map<String, Supplier<byte[]>> out) {
        int colon = ref.indexOf(':');
        String ns = colon < 0 ? "minecraft" : ref.substring(0, colon);
        String path = colon < 0 ? ref : ref.substring(colon + 1);
        if (ns.equals("minecraft")) return true;
        return out.containsKey("assets/" + ns + "/models/" + path + ".json") || modelExists(ns, path);
    }

    /**
     * Items that ended with no model file (the mod's item model json is missing, the model the mod registered is a custom-loader one nobody backs, or
     * the item was never registered with ModelLoader): vanilla's loader logs "Unable to load model" for each and shows the missing-model cube. Such an
     * item gets a flat model of its texture ({@code textures/items/<name>.png} or {@code item/<name>.png}), or its block's model for an ItemBlock.
     */
    private static void itemFallbackModels(Map<String, Supplier<byte[]>> out) {
        int made = 0;
        // spawn eggs of mod entities (EntityRegistry egg colours): vanilla's tinted egg model
        for (ResourceLocation eggId : org.kylepitcock.legacyconnector.v1_12_2.entity.Hosts1122.spawnEggIds()) {
            String file = "assets/" + eggId.getNamespace() + "/models/item/" + eggId.getPath() + ".json";
            if (out.containsKey(file) || modelExists(eggId.getNamespace(), "item/" + eggId.getPath())) continue;
            String json = "{\"parent\":\"minecraft:item/template_spawn_egg\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            out.put(file, () -> bytes);
            if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) org.kylepitcock.legacyconnector.v1_7_10.client.SharedItemModels.note(eggId, json);
        }
        for (legacyconnector.v1_12_2.net.minecraft.item.Item item : org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.modItems()) {
            if (item.getRegistryName() == null) continue;
            for (int meta : org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.variants(item).keySet()) {
                var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.modern(item, meta);
                if (modern == null) continue;
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(modern);
                String ns = id.getNamespace();
                String file = "assets/" + ns + "/models/item/" + id.getPath() + ".json";
                if (out.containsKey(file) || modelExists(ns, "item/" + id.getPath())) continue;
                JsonObject model = new JsonObject();
                String base = id.getPath().replaceFirst("_\\d+$", "");
                String texture = null;
                for (String candidate : new String[] {id.getPath(), base}) {
                    for (String dir : new String[] {"items/", "item/"}) {
                        if (texture == null && resourceExists("assets/" + ns + "/textures/" + dir + candidate + ".png")) texture = ns + ":" + dir + candidate;
                    }
                }
                if (texture != null) {
                    model.addProperty("parent", "minecraft:item/generated");
                    JsonObject textures = new JsonObject();
                    textures.addProperty("layer0", texture);
                    model.add("textures", textures);
                } else if (item instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemBlock && modelExists(ns, "block/" + base)) {
                    model.addProperty("parent", ns + ":block/" + base);
                } else {
                    continue;
                }
                String json = GSON.toJson(model);
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                out.put(file, () -> bytes);
                if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) org.kylepitcock.legacyconnector.v1_7_10.client.SharedItemModels.note(id, json);
                made++;
            }
        }
        if (made > 0) LOG.info("1.12.2 item models: {} item(s) without a model file got a flat model of their texture", made);
    }

    /** The item models of a block's items (an ItemBlock without a model file of its own draws the block's model of its metadata). */
    private static void blockItemModels(Block block, ResourceLocation blockId, Map<Integer, String> models, Map<String, Supplier<byte[]>> out) {
        var item = org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.itemOf(block);
        if (item == null || item.getRegistryName() == null) return;
        var variants = org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.variants(item);
        for (int meta = 0; meta < 16; meta++) {
            // an ItemBlock with subtypes has an item (and so a model) per metadata; one without has one item, which any metadata's model may stand for
            var modern = variants.size() > 1 ? variants.get(meta) : org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.modern(item, meta);
            if (modern == null) continue;
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(modern);
            if (!itemId.getNamespace().equals(blockId.getNamespace()) || modelExists(itemId.getNamespace(), "item/" + itemId.getPath())) continue;
            String m = models.get(meta);
            if (m == null) continue;
            JsonObject model = new JsonObject();
            model.addProperty("parent", m);
            String json = GSON.toJson(model);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            out.put("assets/" + itemId.getNamespace() + "/models/item/" + itemId.getPath() + ".json", () -> bytes);
            if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) org.kylepitcock.legacyconnector.v1_7_10.client.SharedItemModels.note(itemId, json);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String nameOf(IProperty property, Comparable<?> value) {
        return property.getName((Comparable) value);
    }

    private static JsonObject readOriginal(ResourceLocation id) {
        return readJson("assets/" + id.getNamespace() + "/blockstates/" + id.getPath() + ".json");
    }

    /** Parsed mod JSON files by path (callers change what they get, so they receive copies). */
    private static final Map<String, java.util.Optional<JsonObject>> JSON_FILES = new java.util.concurrent.ConcurrentHashMap<>();

    private static JsonObject readJson(String path) {
        var cached = JSON_FILES.get(path);
        if (cached == null) {
            cached = java.util.Optional.ofNullable(parseJson(path));
            JSON_FILES.put(path, cached);
        }
        return cached.map(JsonObject::deepCopy).orElse(null);
    }

    private static JsonObject parseJson(String path) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (var in = loader.getResourceAsStream(path)) {
            if (in == null) return null;
            JsonElement root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return root.isJsonObject() ? root.getAsJsonObject() : null;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }
}
