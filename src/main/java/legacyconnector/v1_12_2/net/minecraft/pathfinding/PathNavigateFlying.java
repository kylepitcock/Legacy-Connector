package legacyconnector.v1_12_2.net.minecraft.pathfinding;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** A mob navigation over the modern navigation of the mob (the host's navigation does the moving). */
public class PathNavigateFlying extends PathNavigate {
    public PathNavigateFlying(EntityLiving entity, World world) {
        super(entity, world);
    }

    public void setCanOpenDoors(boolean canOpenDoors) {
    }

    public void setCanEnterDoors(boolean canEnterDoors) {
    }
}
