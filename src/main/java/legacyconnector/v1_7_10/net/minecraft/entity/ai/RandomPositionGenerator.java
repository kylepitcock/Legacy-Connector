package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityCreature;
import legacyconnector.v1_7_10.net.minecraft.util.Vec3;

/** Picks random walkable-looking positions near a creature for its AI. */
public class RandomPositionGenerator {
    public static Vec3 findRandomTarget(EntityCreature entity, int horizontal, int vertical) {
        return pick(entity, horizontal, vertical, null);
    }

    public static Vec3 findRandomTargetBlockTowards(EntityCreature entity, int horizontal, int vertical, Vec3 towards) {
        return pick(entity, horizontal, vertical, towards);
    }

    public static Vec3 findRandomTargetBlockAwayFrom(EntityCreature entity, int horizontal, int vertical, Vec3 from) {
        Vec3 away = Vec3.createVectorHelper(entity.mcc$x() - from.xCoord, entity.mcc$y() - from.yCoord, entity.mcc$z() - from.zCoord);
        return pick(entity, horizontal, vertical, Vec3.createVectorHelper(entity.mcc$x() + away.xCoord, entity.mcc$y(), entity.mcc$z() + away.zCoord));
    }

    private static Vec3 pick(EntityCreature entity, int horizontal, int vertical, Vec3 towards) {
        Random random = entity.getRNG();
        double bestScore = Double.NEGATIVE_INFINITY;
        Vec3 best = null;
        for (int i = 0; i < 10; i++) {
            double x = entity.mcc$x() + random.nextInt(2 * horizontal + 1) - horizontal;
            double y = entity.mcc$y() + random.nextInt(2 * vertical + 1) - vertical;
            double z = entity.mcc$z() + random.nextInt(2 * horizontal + 1) - horizontal;
            double score = towards == null ? random.nextDouble()
                    : -((x - towards.xCoord) * (x - towards.xCoord) + (z - towards.zCoord) * (z - towards.zCoord));
            if (score > bestScore) {
                bestScore = score;
                best = Vec3.createVectorHelper(x, y, z);
            }
        }
        return best;
    }
}
