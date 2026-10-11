package mccompat.v1_7_10.net.minecraft.entity.ai;

import java.util.function.Function;
import mccompat.v1_7_10.net.minecraft.entity.EntityLiving;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * A vanilla legacy AI task backed by the equivalent modern goal of the entity's host (created on first use, since
 * tasks are built in the legacy constructor). Mod subclasses can still override any of the task methods.
 */
public abstract class GoalTask extends EntityAIBase {
    protected final EntityLiving owner;
    private final Function<PathfinderMob, Goal> factory;
    private Goal goal;
    private boolean resolved;

    protected GoalTask(EntityLiving owner, int mutexBits, Function<PathfinderMob, Goal> factory) {
        this.owner = owner;
        this.factory = factory;
        setMutexBits(mutexBits);
    }

    protected Goal goal() {
        if (!resolved && owner != null && owner.mcc$entity instanceof Mob) {
            resolved = true;
            if (owner.mcc$entity instanceof PathfinderMob m) {
                try {
                    goal = factory.apply(m);
                } catch (RuntimeException | LinkageError e) {
                    goal = null;
                }
            }
        }
        return goal;
    }

    @Override
    public boolean shouldExecute() {
        Goal g = goal();
        return g != null && g.canUse();
    }

    @Override
    public boolean continueExecuting() {
        Goal g = goal();
        return g != null && g.canContinueToUse();
    }

    @Override
    public boolean isInterruptible() {
        Goal g = goal();
        return g == null || g.isInterruptable();
    }

    @Override
    public void startExecuting() {
        Goal g = goal();
        if (g != null) g.start();
    }

    @Override
    public void resetTask() {
        Goal g = goal();
        if (g != null) g.stop();
    }

    @Override
    public void updateTask() {
        Goal g = goal();
        if (g != null) g.tick();
    }
}
