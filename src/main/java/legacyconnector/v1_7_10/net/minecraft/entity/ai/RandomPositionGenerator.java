package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityCreature;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.util.Vec3;

/**
 * Random nearby block positions for a creature's AI, as 1.7.10 picked them: ten tries at whole block coordinates around the
 * creature (optionally only in one half-plane, towards or away from a point), kept inside the creature's home area when it is
 * already near it, and the one the creature likes best ({@code getBlockPathWeight}: grass for animals, darkness for monsters)
 * wins. Null when no try qualified.
 */
public class RandomPositionGenerator {
    public static Vec3 findRandomTarget(EntityCreature entity, int horizontal, int vertical) {
        return pick(entity, horizontal, vertical, 0.0, 0.0, false);
    }

    public static Vec3 findRandomTargetBlockTowards(EntityCreature entity, int horizontal, int vertical, Vec3 towards) {
        return pick(entity, horizontal, vertical, towards.xCoord - entity.mcc$x(), towards.zCoord - entity.mcc$z(), true);
    }

    public static Vec3 findRandomTargetBlockAwayFrom(EntityCreature entity, int horizontal, int vertical, Vec3 from) {
        return pick(entity, horizontal, vertical, entity.mcc$x() - from.xCoord, entity.mcc$z() - from.zCoord, true);
    }

    private static Vec3 pick(EntityCreature entity, int horizontal, int vertical, double dirX, double dirZ, boolean directed) {
        if (horizontal <= 0 || vertical <= 0) return null;
        Random random = entity.getRNG();
        int ex = MathHelper.floor_double(entity.mcc$x());
        int ey = MathHelper.floor_double(entity.mcc$y());
        int ez = MathHelper.floor_double(entity.mcc$z());
        boolean nearHome = false;
        if (entity.hasHome()) {
            double dist = entity.getHomePosition().getDistanceSquared(ex, ey, ez) + 4.0F;
            double range = entity.getMaximumHomeDistance() + horizontal;
            nearHome = dist < range * range;
        }
        boolean found = false;
        int bx = 0, by = 0, bz = 0;
        float best = -99999.0F;
        for (int i = 0; i < 10; i++) {
            int dx = random.nextInt(2 * horizontal) - horizontal;
            int dy = random.nextInt(2 * vertical) - vertical;
            int dz = random.nextInt(2 * horizontal) - horizontal;
            if (directed && dx * dirX + dz * dirZ < 0.0) continue;
            int x = dx + ex, y = dy + ey, z = dz + ez;
            if (nearHome && !entity.isWithinHomeDistance(x, y, z)) continue;
            float weight = entity.getBlockPathWeight(x, y, z);
            if (weight > best) {
                best = weight;
                bx = x;
                by = y;
                bz = z;
                found = true;
            }
        }
        return found ? Vec3.createVectorHelper(bx, by, bz) : null;
    }
}
