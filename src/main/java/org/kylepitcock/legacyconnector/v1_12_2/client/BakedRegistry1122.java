package org.kylepitcock.legacyconnector.v1_12_2.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.IBakedModel;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.model.BakedModel;

/** The modern baked model map as ModelBakeEvent's registry: keyed by legacy locations (see {@link Models1122#modernLocations}). */
public final class BakedRegistry1122 extends legacyconnector.v1_12_2.net.minecraft.util.registry.RegistrySimple<ModelResourceLocation, IBakedModel> {
    // a RegistrySimple: mods cast ModelBakeEvent.getModelRegistry() to it (OpenComputers' ModelInitialization)
    private final Map<net.minecraft.client.resources.model.ModelResourceLocation, BakedModel> models;
    int replaced;

    public BakedRegistry1122(Map<net.minecraft.client.resources.model.ModelResourceLocation, BakedModel> models) {
        this.models = models;
    }

    @Override
    public IBakedModel getObject(ModelResourceLocation name) {
        if (name == null) return null;
        for (var modern : Models1122.modernLocations(name)) {
            BakedModel model = models.get(modern);
            if (model != null) {
                // 1.12 gave every location whose json failed to load the missing model itself; the modern bake draws a copy of it per location, and a
                // mod that asks "is this the missing model" (Extra Utilities 2 decides whether a json overrides its built-in models) must see it
                BakedModel missing = models.get(net.minecraft.client.resources.model.ModelBakery.MISSING_MODEL_VARIANT);
                if (missing != null && model != missing
                        && (org.kylepitcock.legacyconnector.v1_12_2.Assets1122.isStandIn(modern) || Models1122.looksMissing(model, missing))) model = missing;
                return Models1122.legacy(model);
            }
        }
        return null;
    }

    @Override
    public void putObject(ModelResourceLocation key, IBakedModel value) {
        if (key == null || value == null) return;
        BakedModel modern = Models1122.modern(value);
        // a variant of block states the metadata does not hold (Techguns puts a model for every stairs shape, which getActualState works out):
        // kept per state for ActualStateModels1122, and not written over the metadata state's model (the last shape put won every stair)
        List<legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState> states = statesNamed(key);
        boolean beyondMeta = !states.isEmpty();
        for (var state : states) {
            if (org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.metaHolds(state)) beyondMeta = false;
        }
        if (beyondMeta) {
            for (var state : states) stateModels.put(org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.canonical(state), modern);
        }
        for (var location : Models1122.modernLocations(key)) {
            if (beyondMeta && !"inventory".equals(location.getVariant())) continue;
            models.put(location, modern);
            replaced++;
        }
    }

    /** Models put for block states beyond their metadata, by (canonical) legacy state. */
    final Map<legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState, BakedModel> stateModels = new java.util.HashMap<>();
    /** Per block: its state mapper's variant names and the states they stand for (the mapper as it answers during the event). */
    private final Map<legacyconnector.v1_12_2.net.minecraft.block.Block, Map<String, List<legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState>>> named =
            new java.util.IdentityHashMap<>();

    /** The legacy states of a mod block a variant name stands for (none for an inventory name or a name no block's mapper gives). */
    private List<legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState> statesNamed(ModelResourceLocation key) {
        String variant = key.getVariant();
        if (variant == null || variant.equals("inventory")) return List.of();
        try {
            var block = org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.BLOCKS.getValue(
                    new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(key.getNamespace(), key.getPath()));
            if (block == null || !(block.mcc$modern instanceof org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block)) return List.of();
            var byName = named.get(block);
            if (byName == null) {
                byName = new java.util.HashMap<>();
                var mapper = legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.mcc$stateMapper(block);
                if (mapper == null) mapper = new legacyconnector.v1_12_2.net.minecraft.client.renderer.block.statemap.DefaultStateMapper();
                for (var e : mapper.putStateModelLocations(block).entrySet()) {
                    byName.computeIfAbsent(e.getValue().toString(), k -> new ArrayList<>()).add(e.getKey());
                }
                // an empty answer (a mapper that names nothing yet) is not kept: it may name the states later in the event
                if (!byName.isEmpty()) named.put(block, byName);
            }
            return byName.getOrDefault(key.toString(), List.of());
        } catch (RuntimeException | LinkageError e) {
            return List.of();
        }
    }

    @Override
    public Set<ModelResourceLocation> getKeys() {
        Set<ModelResourceLocation> keys = new LinkedHashSet<>();
        for (String k : Models1122.allLocations().keySet()) keys.add(new ModelResourceLocation(k));
        return keys;
    }

    @Override
    public boolean containsKey(ModelResourceLocation key) {
        return getObject(key) != null;
    }

    @Override
    public Iterator<IBakedModel> iterator() {
        List<IBakedModel> values = new ArrayList<>();
        for (ModelResourceLocation key : getKeys()) {
            IBakedModel model = getObject(key);
            if (model != null) values.add(model);
        }
        return values.iterator();
    }
}
