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
        for (var location : Models1122.modernLocations(key)) {
            models.put(location, modern);
            replaced++;
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
