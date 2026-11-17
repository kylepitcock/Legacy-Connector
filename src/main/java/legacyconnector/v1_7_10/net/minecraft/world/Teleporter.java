package legacyconnector.v1_7_10.net.minecraft.world;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;

/**
 * Places an entity that arrives in a dimension (vanilla: finds or builds a nether portal). Mods subclass it for
 * their own portals; {@code ServerConfigurationManager.transferPlayerToDimension} calls {@link #placeInPortal} after
 * moving the entity into the target world, as 1.7.10 did.
 */
public class Teleporter {
    protected final WorldServer worldServerInstance;
    protected final Random random;
    /** Portals found or built, by chunk-coordinate key (mods cache their own portals here, as vanilla did). */
    public final legacyconnector.v1_7_10.net.minecraft.util.LongHashMap destinationCoordinateCache = new legacyconnector.v1_7_10.net.minecraft.util.LongHashMap();
    /** The keys of {@link #destinationCoordinateCache}, for removing stale portals. */
    public final java.util.List destinationCoordinateKeys = new java.util.ArrayList();

    public Teleporter(WorldServer world) {
        this.worldServerInstance = world;
        this.random = new Random(world == null ? 0L : world.getSeed());
    }

    public void placeInPortal(Entity entity, double x, double y, double z, float yaw) {
        // Vanilla would look for or create a portal; arriving entities stay where they were put. A 1.2.5 mod teleporter (Twilight Forest, Tropicraft)
        // overrides the three hooks below, which the 1.2.5 game called with the world and the entity; the layer's dimension travel reaches them here.
        mcc125$onDimensionChanged(worldServerInstance, entity);
    }

    /** 1.2.5's {@code placeInPortal(world, entity)}: find a portal, else make one and find it. A mod's override is emitted by the loader under this name. */
    public void mcc125$onDimensionChanged(World world, Entity entity) {
        if (!mcc125$findNetherPortal(world, entity)) {
            mcc125$generateNetherPortal(world, entity);
            mcc125$findNetherPortal(world, entity);
        }
    }

    /** 1.2.5's {@code placeInExistingPortal(world, entity)}: true when the entity was put at a portal. */
    public boolean mcc125$findNetherPortal(World world, Entity entity) {
        return false;
    }

    /** 1.2.5's {@code makePortal(world, entity)}. */
    public boolean mcc125$generateNetherPortal(World world, Entity entity) {
        return false;
    }

    public boolean placeInExistingPortal(Entity entity, double x, double y, double z, float yaw) {
        return false;
    }

    public boolean makePortal(Entity entity) {
        return false;
    }

    /** Forgets cached portals no one used for 600 ticks (every 100 ticks). */
    public void removeStalePortalLocations(long worldTime) {
        if (worldTime % 100L != 0L) return;
        long stale = worldTime - 600L;
        var it = destinationCoordinateKeys.iterator();
        while (it.hasNext()) {
            Long key = (Long) it.next();
            Object position = destinationCoordinateCache.getValueByKey(key);
            if (position == null || position instanceof PortalPosition p && p.lastUpdateTime < stale) {
                it.remove();
                destinationCoordinateCache.remove(key);
            }
        }
    }

    /** A cached portal position and the last time an entity used it. */
    public class PortalPosition extends legacyconnector.v1_7_10.net.minecraft.util.ChunkCoordinates {
        public long lastUpdateTime;

        public PortalPosition(int x, int y, int z, long time) {
            super(x, y, z);
            this.lastUpdateTime = time;
        }
    }
}
