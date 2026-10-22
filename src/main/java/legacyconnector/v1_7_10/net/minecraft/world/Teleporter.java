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

    public Teleporter(WorldServer world) {
        this.worldServerInstance = world;
        this.random = new Random(world == null ? 0L : world.getSeed());
    }

    public void placeInPortal(Entity entity, double x, double y, double z, float yaw) {
        // Vanilla would look for or create a portal; arriving entities stay where they were put.
    }

    public boolean placeInExistingPortal(Entity entity, double x, double y, double z, float yaw) {
        return false;
    }

    public boolean makePortal(Entity entity) {
        return false;
    }

    public void removeStalePortalLocations(long worldTime) {
    }
}
