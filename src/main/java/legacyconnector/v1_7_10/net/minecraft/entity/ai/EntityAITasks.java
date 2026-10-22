package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Legacy AI scheduler. Every third tick each task is re-evaluated (start when allowed and wanted, stop when it no
 * longer may continue); in between, running tasks are only checked for continuation. A task may run when every
 * running task of equal or higher priority uses disjoint mutex bits, and no running lower-priority task it would
 * displace is uninterruptible.
 */
public class EntityAITasks {
    public List<EntityAITaskEntry> taskEntries = new ArrayList<>();
    private final List<EntityAITaskEntry> executingTaskEntries = new ArrayList<>();
    private int tickCount;
    private int tickRate = 3;

    public EntityAITasks() {
    }

    public EntityAITasks(Object profiler) {
    }

    public void addTask(int priority, EntityAIBase task) {
        taskEntries.add(new EntityAITaskEntry(priority, task));
    }

    public void removeTask(EntityAIBase task) {
        Iterator<EntityAITaskEntry> it = taskEntries.iterator();
        while (it.hasNext()) {
            EntityAITaskEntry e = it.next();
            if (e.action == task) {
                if (executingTaskEntries.remove(e)) e.action.resetTask();
                it.remove();
            }
        }
    }

    public void onUpdateTasks() {
        List<EntityAITaskEntry> starting = new ArrayList<>();
        if (tickCount++ % tickRate == 0) {
            for (EntityAITaskEntry e : new ArrayList<>(taskEntries)) {
                boolean running = executingTaskEntries.contains(e);
                if (running) {
                    if (canUse(e) && canContinue(e)) continue;
                    e.action.resetTask();
                    executingTaskEntries.remove(e);
                }
                if (canUse(e) && e.action.shouldExecute()) {
                    starting.add(e);
                    executingTaskEntries.add(e);
                }
            }
        } else {
            Iterator<EntityAITaskEntry> it = executingTaskEntries.iterator();
            while (it.hasNext()) {
                EntityAITaskEntry e = it.next();
                if (!e.action.continueExecuting()) {
                    e.action.resetTask();
                    it.remove();
                }
            }
        }
        for (EntityAITaskEntry e : starting) e.action.startExecuting();
        for (EntityAITaskEntry e : new ArrayList<>(executingTaskEntries)) e.action.updateTask();
    }

    private boolean canContinue(EntityAITaskEntry e) {
        return e.action.continueExecuting();
    }

    private boolean canUse(EntityAITaskEntry e) {
        for (EntityAITaskEntry other : taskEntries) {
            if (other == e || !executingTaskEntries.contains(other)) continue;
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

    public class EntityAITaskEntry {
        public EntityAIBase action;
        public int priority;

        public EntityAITaskEntry(int priority, EntityAIBase action) {
            this.priority = priority;
            this.action = action;
        }
    }
}
