package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;

/** Random reachable spots near a creature (the modern random position helpers). */
public class RandomPositionGenerator {
    private static PathfinderMob mob(EntityCreature c) {
        return c != null && c.mcc$entity instanceof PathfinderMob m ? m : null;
    }

    private static Vec3d of(net.minecraft.world.phys.Vec3 v) {
        return v == null ? null : Vec3d.mcc$of(v);
    }

    public static Vec3d findRandomTarget(EntityCreature entity, int xz, int y) {
        PathfinderMob m = mob(entity);
        return m == null ? null : of(DefaultRandomPos.getPos(m, xz, y));
    }

    public static Vec3d findRandomTargetBlockTowards(EntityCreature entity, int xz, int y, Vec3d target) {
        PathfinderMob m = mob(entity);
        return m == null || target == null ? null : of(DefaultRandomPos.getPosTowards(m, xz, y, target.mcc$modern(), Math.PI / 2));
    }

    public static Vec3d findRandomTargetBlockAwayFrom(EntityCreature entity, int xz, int y, Vec3d target) {
        PathfinderMob m = mob(entity);
        return m == null || target == null ? null : of(DefaultRandomPos.getPosAway(m, xz, y, target.mcc$modern()));
    }

    public static Vec3d getLandPos(EntityCreature entity, int xz, int y) {
        PathfinderMob m = mob(entity);
        return m == null ? null : of(LandRandomPos.getPos(m, xz, y));
    }
}
