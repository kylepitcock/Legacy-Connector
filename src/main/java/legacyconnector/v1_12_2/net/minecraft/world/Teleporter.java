package legacyconnector.v1_12_2.net.minecraft.world;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;

/**
 * Places an entity arriving in a world. Vanilla's nether portal search is the modern game's own (it runs for vanilla travel);
 * this base keeps the entity where it arrives, and mods' subclasses place it themselves.
 */
public class Teleporter implements legacyconnector.v1_12_2.net.minecraftforge.common.util.ITeleporter {
    protected final WorldServer world;
    protected final Random random;
    /** Portals found recently, by packed chunk-column position (mods' teleporters cache their own portals here). */
    protected final it.unimi.dsi.fastutil.longs.Long2ObjectMap<PortalPosition> destinationCoordinateCache = new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>(4096);

    public Teleporter(WorldServer worldIn) {
        this.world = worldIn;
        this.random = new Random(worldIn.getSeed());
    }

    public void placeInPortal(Entity entityIn, float rotationYaw) {
        if (!placeInExistingPortal(entityIn, rotationYaw)) {
            makePortal(entityIn);
            placeInExistingPortal(entityIn, rotationYaw);
        }
    }

    public boolean placeInExistingPortal(Entity entityIn, float rotationYaw) {
        return true;
    }

    public boolean makePortal(Entity entityIn) {
        return false;
    }

    /** Every 100 ticks forgets cached portals not used in the last 300 ticks. */
    public void removeStalePortalLocations(long worldTime) {
        if (worldTime % 100L != 0L) return;
        long oldest = worldTime - 300L;
        destinationCoordinateCache.values().removeIf(p -> p == null || p.lastUpdateTime < oldest);
    }

    /** A cached portal position and when it was last used. */
    public class PortalPosition extends legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos {
        public long lastUpdateTime;

        public PortalPosition(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, long lastUpdate) {
            super(pos.getX(), pos.getY(), pos.getZ());
            this.lastUpdateTime = lastUpdate;
        }
    }

    /** Forge's ITeleporter hook. */
    @Override
    public void placeEntity(World world, Entity entity, float yaw) {
        placeInPortal(entity, yaw);
    }

    @Override
    public boolean isVanilla() {
        return getClass() == Teleporter.class;
    }
}
