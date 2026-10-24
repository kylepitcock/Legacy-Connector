package legacyconnector.v1_7_10.net.minecraft.entity;

import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.util.IntHashMap;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A world's entity tracker. The modern server tracks entities itself, so this only answers the lookups mods make. */
public class EntityTracker {
    public final World mcc$world;
    public IntHashMap trackedEntityIDs;
    public java.util.Set trackedEntities = new java.util.HashSet<>();

    public EntityTracker(World world) {
        this.mcc$world = world;
        final Map<Integer, EntityTrackerEntry> entries = new HashMap<>();
        this.trackedEntityIDs = new IntHashMap() {
            @Override
            public Object lookup(int id) {
                Entity entity = mcc$world.getEntityByID(id);
                return entity == null ? null : entries.computeIfAbsent(id, k -> new EntityTrackerEntry(entity));
            }
        };
    }
}
