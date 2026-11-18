package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.resources.model.BakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.MultipartModelData;

/**
 * Stands in for {@code MultipartModelData.Builder} (Forge 1.20.1; NeoForge 1.21.1 has no builder, its {@code MultipartModelData.PROPERTY} holds the plain
 * {@code Map<BakedModel, ModelData>} that {@code MultipartModelData.resolve} reads). The builder collects the per-part model data the way the original did and
 * {@link #build()} answers a {@link MultipartModelData} that IS that map, so a mod that puts it under {@code MultipartModelData.PROPERTY} feeds the vanilla reader.
 */
public final class LegacyMultipartBuilder {
    private final Map<BakedModel, ModelData> data = new IdentityHashMap<>();

    public LegacyMultipartBuilder with(BakedModel model, ModelData modelData) {
        data.put(model, modelData);
        return this;
    }

    public MultipartModelData build() {
        return new Built(data);
    }

    /** The built value: a {@code MultipartModelData} (the type the mod's code expects) that is also the map the 1.21.1 property carries. */
    static final class Built extends MultipartModelData implements Map<BakedModel, ModelData> {
        private final Map<BakedModel, ModelData> map;

        Built(Map<BakedModel, ModelData> source) {
            this.map = new IdentityHashMap<>(source);
        }

        @Override public int size() { return map.size(); }
        @Override public boolean isEmpty() { return map.isEmpty(); }
        @Override public boolean containsKey(Object key) { return map.containsKey(key); }
        @Override public boolean containsValue(Object value) { return map.containsValue(value); }
        @Override public ModelData get(Object key) { return map.get(key); }
        @Override public ModelData put(BakedModel key, ModelData value) { return map.put(key, value); }
        @Override public ModelData remove(Object key) { return map.remove(key); }
        @Override public void putAll(Map<? extends BakedModel, ? extends ModelData> m) { map.putAll(m); }
        @Override public void clear() { map.clear(); }
        @Override public Set<BakedModel> keySet() { return map.keySet(); }
        @Override public Collection<ModelData> values() { return map.values(); }
        @Override public Set<Entry<BakedModel, ModelData>> entrySet() { return map.entrySet(); }
    }
}
