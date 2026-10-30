package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The 1.12.2 AI scheduler. Every third tick each task is re-evaluated (start when allowed and wanted, stop when it may no longer
 * continue); in between, running tasks are only checked for continuation. A task may run when every running task of equal or higher
 * priority uses disjoint mutex bits, and no running lower-priority task it would displace is uninterruptible.
 *
 * <p>The task lists of a vanilla mob's view are bound to the modern goal selectors instead: tasks a mod adds become modern goals
 * ({@link org.kylepitcock.legacyconnector.v1_12_2.entity.LegacyGoal1122}) next to the mob's own.</p>
 */
public class EntityAITasks {
    public final Set<EntityAITaskEntry> taskEntries = new LinkedHashSet<>();
    private final Set<EntityAITaskEntry> executingTaskEntries = new LinkedHashSet<>();
    private int tickCount;
    private int tickRate = 3;
    private int disabledControlFlags;
    private net.minecraft.world.entity.Mob mcc$goalOwner;
    private boolean mcc$targets;

    public EntityAITasks() {
    }

    /** {@code new EntityAITasks(world.profiler)}: the profiler is not used. */
    public EntityAITasks(Object profiler) {
    }

    /** {@code new EntityAITasks(world.profiler)}: the legacy profiler records nothing for the AI (the modern one profiles the goals). */
    public EntityAITasks(legacyconnector.v1_12_2.net.minecraft.profiler.Profiler profiler) {
    }

    /** Binds the list to a vanilla mob's goal selector (target selector for target tasks). */
    public final void mcc$bindGoals(legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving owner, boolean targets) {
        if (owner.mcc$entity instanceof net.minecraft.world.entity.Mob m) {
            this.mcc$goalOwner = m;
            this.mcc$targets = targets;
        }
    }

    private net.minecraft.world.entity.ai.goal.GoalSelector mcc$selector() {
        var m = mcc$goalOwner;
        if (m == null) return null;
        return mcc$targets ? m.targetSelector : m.goalSelector;
    }

    public void addTask(int priority, EntityAIBase task) {
        if (task == null) return;
        EntityAITaskEntry entry = new EntityAITaskEntry(priority, task);
        taskEntries.add(entry);
        var selector = mcc$selector();
        if (selector != null) {
            entry.mcc$goal = new org.kylepitcock.legacyconnector.v1_12_2.entity.LegacyGoal1122(task, mcc$targets);
            selector.addGoal(priority, entry.mcc$goal);
        }
    }

    public void removeTask(EntityAIBase task) {
        Iterator<EntityAITaskEntry> it = taskEntries.iterator();
        while (it.hasNext()) {
            EntityAITaskEntry e = it.next();
            if (e.action != task) continue;
            if (e.using) {
                e.using = false;
                e.action.resetTask();
                executingTaskEntries.remove(e);
            }
            var selector = mcc$selector();
            if (selector != null && e.mcc$goal != null) selector.removeGoal(e.mcc$goal);
            it.remove();
            return;
        }
    }

    public void onUpdateTasks() {
        if (mcc$selector() != null) return; // the modern goal selector runs them
        if (tickCount++ % tickRate == 0) {
            for (EntityAITaskEntry e : new java.util.ArrayList<>(taskEntries)) {
                if (e.using) {
                    if (!canUse(e) || !canContinue(e)) {
                        e.using = false;
                        safe(e.action::resetTask);
                        executingTaskEntries.remove(e);
                    }
                } else if (canUse(e) && safeTest(e.action::shouldExecute)) {
                    e.using = true;
                    safe(e.action::startExecuting);
                    executingTaskEntries.add(e);
                }
            }
        } else {
            Iterator<EntityAITaskEntry> it = executingTaskEntries.iterator();
            while (it.hasNext()) {
                EntityAITaskEntry e = it.next();
                if (!canContinue(e)) {
                    e.using = false;
                    safe(e.action::resetTask);
                    it.remove();
                }
            }
        }
        if (!executingTaskEntries.isEmpty()) {
            for (EntityAITaskEntry e : new java.util.ArrayList<>(executingTaskEntries)) safe(e.action::updateTask);
        }
    }

    private static final java.util.Set<Class<?>> FAILED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** An AI task that throws (an API the layer lacks) is reported once per class and treated as not running. */
    private static void safe(Runnable r) {
        try {
            r.run();
        } catch (RuntimeException | LinkageError e) {
            if (FAILED.add(r.getClass())) org.slf4j.LoggerFactory.getLogger("LegacyConnector/Entities").warn("An AI task failed: {}", e.toString());
        }
    }

    private static boolean safeTest(java.util.function.BooleanSupplier s) {
        try {
            return s.getAsBoolean();
        } catch (RuntimeException | LinkageError e) {
            if (FAILED.add(s.getClass())) org.slf4j.LoggerFactory.getLogger("LegacyConnector/Entities").warn("An AI task failed: {}", e.toString());
            return false;
        }
    }

    private boolean canContinue(EntityAITaskEntry e) {
        return safeTest(e.action::shouldContinueExecuting);
    }

    private boolean canUse(EntityAITaskEntry e) {
        if ((e.action.getMutexBits() & disabledControlFlags) != 0) return false;
        for (EntityAITaskEntry other : executingTaskEntries) {
            if (other == e) continue;
            if (e.priority >= other.priority) {
                if (!areTasksCompatible(e, other)) return false;
            } else if (!other.action.isInterruptible()) {
                return false;
            }
        }
        return true;
    }

    private boolean areTasksCompatible(EntityAITaskEntry a, EntityAITaskEntry b) {
        return (a.action.getMutexBits() & b.action.getMutexBits()) == 0;
    }

    public boolean isControlFlagDisabled(int flag) {
        return (disabledControlFlags & flag) > 0;
    }

    public void disableControlFlag(int flag) {
        disabledControlFlags |= flag;
    }

    public void enableControlFlag(int flag) {
        disabledControlFlags &= ~flag;
    }

    public void setControlFlag(int flag, boolean enabled) {
        if (enabled) enableControlFlag(flag);
        else disableControlFlag(flag);
    }

    public class EntityAITaskEntry {
        public final EntityAIBase action;
        public final int priority;
        public boolean using;
        org.kylepitcock.legacyconnector.v1_12_2.entity.LegacyGoal1122 mcc$goal;

        public EntityAITaskEntry(int priority, EntityAIBase action) {
            this.priority = priority;
            this.action = action;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof EntityAITaskEntry e && action.equals(e.action);
        }

        @Override
        public int hashCode() {
            return action.hashCode();
        }
    }
}
