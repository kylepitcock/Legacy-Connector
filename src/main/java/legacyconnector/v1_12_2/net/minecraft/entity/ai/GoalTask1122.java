package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import java.util.function.Function;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * A vanilla 1.12.2 AI task backed by the equivalent modern goal on the task owner's modern mob (made on first use: tasks are built in
 * the legacy constructor). Mod subclasses can still override any of the task methods. When the owner's modern mob is not of the kind
 * the goal needs, the task never runs.
 */
public abstract class GoalTask1122 extends EntityAIBase {
    protected final EntityLiving mcc$owner;
    private final Function<Mob, Goal> factory;
    private Goal goal;
    private boolean resolved;

    protected GoalTask1122(EntityLiving owner, int mutexBits, Function<Mob, Goal> factory) {
        this.mcc$owner = owner;
        this.factory = factory;
        setMutexBits(mutexBits);
    }

    protected final Goal mcc$goal() {
        if (!resolved && mcc$owner != null && mcc$owner.mcc$entity instanceof Mob m) {
            resolved = true;
            try {
                goal = factory.apply(m);
            } catch (RuntimeException | LinkageError e) {
                goal = null;
            }
        }
        return goal;
    }

    @Override
    public boolean shouldExecute() {
        Goal g = mcc$goal();
        return g != null && g.canUse();
    }

    @Override
    public boolean shouldContinueExecuting() {
        Goal g = mcc$goal();
        return g != null && g.canContinueToUse();
    }

    @Override
    public boolean isInterruptible() {
        Goal g = mcc$goal();
        return g == null || g.isInterruptable();
    }

    @Override
    public void startExecuting() {
        Goal g = mcc$goal();
        if (g != null) g.start();
    }

    @Override
    public void resetTask() {
        Goal g = mcc$goal();
        if (g != null) g.stop();
    }

    @Override
    public void updateTask() {
        Goal g = mcc$goal();
        if (g != null) g.tick();
    }

    /** The modern goal factory only applies to pathfinding mobs. */
    protected static Function<Mob, Goal> pathfinder(Function<net.minecraft.world.entity.PathfinderMob, Goal> f) {
        return m -> m instanceof net.minecraft.world.entity.PathfinderMob p ? f.apply(p) : null;
    }

    /** A modern entity class filter for a legacy class: every living entity whose legacy view is of that class. */
    protected static java.util.function.Predicate<net.minecraft.world.entity.LivingEntity> legacyClass(Class<?> cls) {
        if (cls == null) return e -> true;
        return e -> cls.isInstance(org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(e));
    }
}
