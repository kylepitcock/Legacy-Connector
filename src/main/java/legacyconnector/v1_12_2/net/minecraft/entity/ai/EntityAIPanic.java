package legacyconnector.v1_12_2.net.minecraft.entity.ai;

/** Runs away after being hurt or while burning (the modern panic goal). */
public class EntityAIPanic extends GoalTask1122 {
    protected final legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature creature;
    protected double speed;
    protected double randPosX;
    protected double randPosY;
    protected double randPosZ;

    public EntityAIPanic(legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature creature, double speed) {
        super(creature, 1, pathfinder(m -> new net.minecraft.world.entity.ai.goal.PanicGoal(m, speed)));
        this.creature = creature;
        this.speed = speed;
    }

    /** The nearest water block within the ranges (a burning mob runs there), or null. */
    protected legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos getRandPos(legacyconnector.v1_12_2.net.minecraft.world.World world,
            legacyconnector.v1_12_2.net.minecraft.entity.Entity entity, int horizontalRange, int verticalRange) {
        var access = world == null ? null : world.mcc$access();
        if (access == null || entity == null) return null;
        var origin = new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos(entity);
        int x0 = origin.getX(), y0 = origin.getY(), z0 = origin.getZ();
        float best = horizontalRange * horizontalRange * verticalRange * 2;
        legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos found = null;
        var m = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int x = x0 - horizontalRange; x <= x0 + horizontalRange; ++x) {
            for (int y = y0 - verticalRange; y <= y0 + verticalRange; ++y) {
                for (int z = z0 - horizontalRange; z <= z0 + horizontalRange; ++z) {
                    m.set(x, y, z);
                    if (!access.getFluidState(m).is(net.minecraft.tags.FluidTags.WATER)) continue;
                    float d = (x - x0) * (x - x0) + (y - y0) * (y - y0) + (z - z0) * (z - z0);
                    if (d < best) {
                        best = d;
                        found = new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos(x, y, z);
                    }
                }
            }
        }
        return found;
    }

    protected boolean findRandomPosition() {
        var v = RandomPositionGenerator.findRandomTarget(creature, 5, 4);
        if (v == null) return false;
        randPosX = v.x;
        randPosY = v.y;
        randPosZ = v.z;
        return true;
    }

}
