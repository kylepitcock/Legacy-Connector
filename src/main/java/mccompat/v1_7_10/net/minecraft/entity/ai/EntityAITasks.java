package mccompat.v1_7_10.net.minecraft.entity.ai;

import java.util.ArrayList;
import java.util.List;

public class EntityAITasks {
    public List taskEntries = new ArrayList();
    private List executingTaskEntries = new ArrayList();

    public void addTask(int priority, EntityAIBase task) {
        taskEntries.add(new EntityAITaskEntry(priority, task));
    }

    public void removeTask(EntityAIBase task) {
        taskEntries.removeIf(e -> ((EntityAITaskEntry) e).action == task);
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
