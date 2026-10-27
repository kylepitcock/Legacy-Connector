package org.kylepitcock.legacyconnector.v1_12_2.entity;

import java.util.EnumSet;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * A 1.12.2 AI task a mod added to a vanilla mob, run by the mob's modern goal selector. Mutex bits become goal flags (1 movement,
 * 2 looking, 4 jumping; target tasks claim the target flag). A task that throws stops running (reported once per class).
 */
public final class LegacyGoal1122 extends Goal {
    private final EntityAIBase task;
    private boolean broken;
    private static final java.util.Set<Class<?>> FAILED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public LegacyGoal1122(EntityAIBase task, boolean target) {
        this.task = task;
        EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);
        int bits = task.getMutexBits();
        if ((bits & 1) != 0) flags.add(Flag.MOVE);
        if ((bits & 2) != 0) flags.add(Flag.LOOK);
        if ((bits & 4) != 0) flags.add(Flag.JUMP);
        if (target) flags.add(Flag.TARGET);
        setFlags(flags);
    }

    public EntityAIBase task() {
        return task;
    }

    private void fail(Throwable e) {
        broken = true;
        if (FAILED.add(task.getClass())) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Entities").warn("AI task {} failed and was stopped: {}", task.getClass().getName(), e.toString());
        }
    }

    @Override
    public boolean canUse() {
        if (broken) return false;
        try {
            return task.shouldExecute();
        } catch (RuntimeException | LinkageError e) {
            fail(e);
            return false;
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (broken) return false;
        try {
            return task.shouldContinueExecuting();
        } catch (RuntimeException | LinkageError e) {
            fail(e);
            return false;
        }
    }

    @Override
    public boolean isInterruptable() {
        return task.isInterruptible();
    }

    @Override
    public void start() {
        try {
            task.startExecuting();
        } catch (RuntimeException | LinkageError e) {
            fail(e);
        }
    }

    @Override
    public void stop() {
        try {
            task.resetTask();
        } catch (RuntimeException | LinkageError e) {
            fail(e);
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (broken) return;
        try {
            task.updateTask();
        } catch (RuntimeException | LinkageError e) {
            fail(e);
        }
    }
}
