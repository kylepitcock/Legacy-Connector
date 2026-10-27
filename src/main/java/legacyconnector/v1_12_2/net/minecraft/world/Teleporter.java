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

    public void removeStalePortalLocations(long worldTime) {
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
