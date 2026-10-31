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

    private static void translateAll(Map<String, Supplier<byte[]>> out) {
        clientProxies();
        itemModels(out);
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
        LOG.info("1.12.2 render types: {} cutout, {} translucent, {} solid", stats.cutout, stats.translucent, stats.solid);
        LOG.info("1.12.2 blockstates: {} translated, {} fell back to a plain model{}", stats.translated, stats.fellBack,
                stats.fallbacks.isEmpty() ? "" : " (" + String.join("; ", stats.fallbacks.subList(0, Math.min(60, stats.fallbacks.size())))
                        + (stats.fallbacks.size() > 60 ? "; ... " + (stats.fallbacks.size() - 60) + " more" : "") + ")");
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
        for (legacyconnector.v1_12_2.net.minecraft.item.Item item : org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.modItems()) {
            if (item.getRegistryName() == null || item instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemBlock) continue;
            for (int meta = 0; meta < 16; meta++) {
                var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.modern(item, meta);
                if (modern == null) continue;
                var mrl = legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.mcc$model(item, meta);
                if (mrl == null) continue;
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(modern);
                String target = Ids.namespace(mrl.getNamespace()) + ":item/" + Ids.normalizePath(mrl.getPath());
                if (ObjAssets1122.isObj(mrl.getPath())) {
                    // an OBJ item model (models/item/<name>.obj): NeoForge's OBJ loader
                    target = ObjAssets1122.model(Ids.namespace(mrl.getNamespace()),
                            "item/" + Ids.normalizePath(mrl.getPath()), null, null, null, out);
                }
                if (target.equals(id.getNamespace() + ":item/" + id.getPath())) continue; // the mod's own file already has the id's name
                JsonObject model = new JsonObject();
                model.addProperty("parent", target);
                byte[] bytes = GSON.toJson(model).getBytes(StandardCharsets.UTF_8);
                out.put("assets/" + id.getNamespace() + "/models/item/" + id.getPath() + ".json", () -> bytes);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------
    // one block

    private static void translate(Block block, ResourceLocation id, Map<String, Supplier<byte[]>> out, Stats stats) {
        boolean debug = System.getProperty("legacyconnector.debugAssets") != null && id.getPath().equals(System.getProperty("legacyconnector.debugAssets"));
        JsonObject original = readOriginal(id);
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
            if (best.fallback != null) reasons.add("meta " + meta + ": " + best.fallback);
            if (debug) LOG.info("meta {} of {}: {} parts={} fallback={}", meta, id, best.model, best.parts, best.fallback);
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
        for (int meta = 0; meta < 16; meta++) variants.add("meta=" + meta, model);
        JsonObject result = new JsonObject();
        result.add("variants", variants);
        byte[] bytes = GSON.toJson(result).getBytes(StandardCharsets.UTF_8);
        out.put("assets/" + id.getNamespace() + "/blockstates/" + id.getPath() + ".json", () -> bytes);
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
        if (!usable(acc) && mapped != null) {
            // a block whose states are mapped in code (ModelLoader.setCustomStateMapper): the mapper names the blockstate file and variant
            ResourceLocation file = ResourceLocation.fromNamespaceAndPath(Ids.namespace(mapped.getNamespace()), Ids.normalizePath(mapped.getPath()));
            JsonObject mappedFile = readOriginal(file);
            if (mappedFile != null) {
                // Forge looked the variant up by the mapper's variant string ("type=plastic" for a block whose state has no such property)
                TreeMap<String, String> viaProps = new TreeMap<>(props);
                for (String part : mapped.getVariant().split(",")) {
                    int eq = part.indexOf('=');
                    if (eq > 0) viaProps.put(part.substring(0, eq), part.substring(eq + 1));
                }
                Acc viaMapper = merge(mappedFile, viaProps, List.of(mapped.getVariant(), exact));
                if (usable(viaMapper) || acc == null) acc = viaMapper;
            } else {
                reason = "state mapper file " + file + " not found";
            }
        }
        return finish(acc, id, key, reason, out);
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
            m.addProperty("model", "minecraft:block/cube_all");
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
        if (chain.blocked || colon < 0 || model.startsWith("minecraft:")) return copy;
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

    private static boolean modelExists(String namespace, String path) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader.getResource("assets/" + namespace + "/models/" + path + ".json") != null;
    }

    private static boolean resourceExists(String path) {
        return Thread.currentThread().getContextClassLoader().getResource(path) != null;
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
    private static final Map<String, String> VANILLA_MODEL_RENAMES = Map.of("block/half_slab", "block/slab", "block/upper_slab", "block/slab_top",
            "block/grass_path", "block/dirt_path", "block/slime", "block/slime_block");

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
        if (modelNs.equals("minecraft") && VANILLA_MODEL_RENAMES.containsKey(modelPath)) {
            // a vanilla model of 1.12 that 1.13 renamed (the loader renames the same ones in the mod's own model files)
            modelPath = VANILLA_MODEL_RENAMES.get(modelPath);
            model = "minecraft:" + modelPath;
        }
        if (ObjAssets1122.isObj(model)) {
            // a Forge OBJ model: NeoForge's OBJ loader draws it (with the variant's texture overrides and flip-v)
            if (!modelNs.equals("minecraft") && !resourceExists("assets/" + modelNs + "/models/" + modelPath)) return null;
            JsonObject textures = source.has("textures") && source.get("textures").isJsonObject() ? source.getAsJsonObject("textures") : null;
            copy.addProperty("model", ObjAssets1122.model(modelNs, modelPath, textures, ObjAssets1122.custom(source),
                    textures == null ? null : blockId.getPath() + "_" + key, out));
            return copy;
        }
        if (!modelResolves(modelNs, modelPath, out, 0)) return null;
        if (source.has("textures") && source.get("textures").isJsonObject() && source.getAsJsonObject("textures").size() > 0) {
            JsonObject generated = new JsonObject();
            generated.addProperty("parent", model);
            JsonObject textures = new JsonObject();
            for (var t : source.getAsJsonObject("textures").entrySet()) {
                if (t.getValue().isJsonPrimitive()) textures.addProperty(t.getKey().startsWith("#") ? t.getKey().substring(1) : t.getKey(), lowerRef(t.getValue().getAsString()));
            }
            generated.add("textures", textures);
            String path = "block/lc1122/" + blockId.getPath() + "_" + Ids.normalizePath(key);
            byte[] bytes = GSON.toJson(generated).getBytes(StandardCharsets.UTF_8);
            out.put("assets/" + blockId.getNamespace() + "/models/" + path + ".json", () -> bytes);
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
        return Ids.namespace(ns) + ":" + Ids.normalizePath(path);
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

    /** The item models of a block's items (an ItemBlock without a model file of its own draws the block's model of its metadata). */
    private static void blockItemModels(Block block, ResourceLocation blockId, Map<Integer, String> models, Map<String, Supplier<byte[]>> out) {
        var item = org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.itemOf(block);
        if (item == null || item.getRegistryName() == null) return;
        for (int meta = 0; meta < 16; meta++) {
            var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.modern(item, meta);
            if (modern == null) continue;
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(modern);
            if (!itemId.getNamespace().equals(blockId.getNamespace()) || modelExists(itemId.getNamespace(), "item/" + itemId.getPath())) continue;
            String m = models.get(meta);
            if (m == null) continue;
            JsonObject model = new JsonObject();
            model.addProperty("parent", m);
            byte[] bytes = GSON.toJson(model).getBytes(StandardCharsets.UTF_8);
            out.put("assets/" + itemId.getNamespace() + "/models/item/" + itemId.getPath() + ".json", () -> bytes);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String nameOf(IProperty property, Comparable<?> value) {
        return property.getName((Comparable) value);
    }

    private static JsonObject readOriginal(ResourceLocation id) {
        return readJson("assets/" + id.getNamespace() + "/blockstates/" + id.getPath() + ".json");
    }

    private static JsonObject readJson(String path) {
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
