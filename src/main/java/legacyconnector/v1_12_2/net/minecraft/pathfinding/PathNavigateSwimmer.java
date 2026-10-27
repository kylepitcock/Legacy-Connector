package legacyconnector.v1_12_2.net.minecraft.pathfinding;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** A mob navigation over the modern navigation of the mob (the host's navigation does the moving). */
public class PathNavigateSwimmer extends PathNavigate {
    public PathNavigateSwimmer(EntityLiving entity, World world) {
        super(entity, world);
    }
}
