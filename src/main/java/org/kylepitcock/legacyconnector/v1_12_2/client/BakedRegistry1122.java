package org.kylepitcock.legacyconnector.v1_12_2.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.IBakedModel;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.registry.IRegistry;
import net.minecraft.client.resources.model.BakedModel;

/** The modern baked model map as ModelBakeEvent's registry: keyed by legacy locations (see {@link Models1122#modernLocations}). */
public final class BakedRegistry1122 implements IRegistry<ModelResourceLocation, IBakedModel> {
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
            if (model != null) return Models1122.legacy(model);
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
    public Iterator<IBakedModel> iterator() {
        List<IBakedModel> values = new ArrayList<>();
        for (ModelResourceLocation key : getKeys()) {
            IBakedModel model = getObject(key);
            if (model != null) values.add(model);
        }
        return values.iterator();
    }
}
