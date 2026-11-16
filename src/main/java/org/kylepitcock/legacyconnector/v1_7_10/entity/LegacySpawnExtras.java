package org.kylepitcock.legacyconnector.v1_7_10.entity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import legacyconnector.v1_7_10.cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;

/**
 * Extra spawn data for legacy entities whose class does not implement {@link IEntityAdditionalSpawnData} itself but whose old API carried data with the
 * spawn packet anyway (ModLoaderMP's tracker entries with an owner: the vehicle spawn packet carried the owner's entity id). A provider answers
 * with the adapter for an entity or null; the first non-null answer wins. Used by {@link LegacySpawnData}.
 */
public final class LegacySpawnExtras {
    private static final List<Function<Entity, IEntityAdditionalSpawnData>> PROVIDERS = new CopyOnWriteArrayList<>();

    private LegacySpawnExtras() {}

    public static void register(Function<Entity, IEntityAdditionalSpawnData> provider) {
        PROVIDERS.add(provider);
    }

    /** The entity's own spawn data, else an adapter a provider offers, else null. */
    static IEntityAdditionalSpawnData of(Entity legacy) {
        if (legacy == null) return null;
        if (legacy instanceof IEntityAdditionalSpawnData data) return data;
        for (Function<Entity, IEntityAdditionalSpawnData> p : PROVIDERS) {
            IEntityAdditionalSpawnData d = p.apply(legacy);
            if (d != null) return d;
        }
        return null;
    }
}
