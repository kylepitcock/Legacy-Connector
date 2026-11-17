package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

/**
 * The baked model map of {@code ModelEvent.ModifyBakingResult} / {@code BakingCompleted} as 1.20.1 had it ({@code Map<ResourceLocation, BakedModel>}, keyed by
 * model locations): a LIVE view of NeoForge's {@code Map<ModelResourceLocation, BakedModel>}. Nothing is copied; writes go through to the real map.
 *
 * <p>Keys (see {@link ModelLocationCompat} for the mapping): iteration yields {@link LegacyModelResourceLocation}s for models with a variant and plain
 * {@link ResourceLocation}s for standalone ones; {@code get}/{@code containsKey}/{@code remove} take a legacy location (exact variant), a plain location
 * (the standalone model, else the {@code inventory} variant: lookups only) or a record. {@code put} with a plain location writes the standalone entry.
 */
public final class LegacyModelMap extends AbstractMap<ResourceLocation, BakedModel> {
    private final Map<ModelResourceLocation, BakedModel> real;

    public LegacyModelMap(Map<ModelResourceLocation, BakedModel> real) {
        this.real = real;
    }

    /** The record a lookup of this key finds in the real map, or null when there is none. */
    private ModelResourceLocation find(Object key) {
        ModelResourceLocation m = ModelLocationCompat.modern(key);
        if (m == null) return null;
        if (real.containsKey(m)) return m;
        if (!(key instanceof LegacyModelResourceLocation) && !(key instanceof ModelResourceLocation) && key instanceof ResourceLocation r) {
            ModelResourceLocation inv = ModelResourceLocation.inventory(m.id());
            if (real.containsKey(inv)) return inv;
        }
        return null;
    }

    @Override
    public BakedModel get(Object key) {
        ModelResourceLocation m = find(key);
        return m == null ? null : real.get(m);
    }

    @Override
    public boolean containsKey(Object key) {
        return find(key) != null;
    }

    @Override
    public BakedModel put(ResourceLocation key, BakedModel value) {
        ModelResourceLocation m = ModelLocationCompat.modern(key);
        if (m == null) throw new NullPointerException("model location");
        return real.put(m, value);
    }

    @Override
    public BakedModel remove(Object key) {
        ModelResourceLocation m = find(key);
        return m == null ? null : real.remove(m);
    }

    @Override
    public void clear() {
        real.clear();
    }

    @Override
    public int size() {
        return real.size();
    }

    @Override
    public boolean isEmpty() {
        return real.isEmpty();
    }

    @Override
    public Set<Entry<ResourceLocation, BakedModel>> entrySet() {
        return new AbstractSet<>() {
            @Override
            public Iterator<Entry<ResourceLocation, BakedModel>> iterator() {
                Iterator<Entry<ModelResourceLocation, BakedModel>> it = real.entrySet().iterator();
                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public Entry<ResourceLocation, BakedModel> next() {
                        Entry<ModelResourceLocation, BakedModel> e = it.next();
                        return new Entry<>() {
                            private final ResourceLocation key = ModelLocationCompat.legacy(e.getKey());

                            @Override
                            public ResourceLocation getKey() {
                                return key;
                            }

                            @Override
                            public BakedModel getValue() {
                                return e.getValue();
                            }

                            @Override
                            public BakedModel setValue(BakedModel value) {
                                return e.setValue(value);
                            }

                            @Override
                            public boolean equals(Object o) {
                                return o instanceof Map.Entry<?, ?> x && key.equals(x.getKey()) && java.util.Objects.equals(getValue(), x.getValue());
                            }

                            @Override
                            public int hashCode() {
                                return key.hashCode() ^ java.util.Objects.hashCode(getValue());
                            }

                            @Override
                            public String toString() {
                                return key + "=" + getValue();
                            }
                        };
                    }

                    @Override
                    public void remove() {
                        it.remove();
                    }
                };
            }

            @Override
            public int size() {
                return real.size();
            }

            @Override
            public void clear() {
                real.clear();
            }
        };
    }
}
