package legacyconnector.v1_12_2.net.minecraft.entity.ai;

/**
 * A 1.12.2 AI task. Mutex bits: 1 movement, 2 looking, 4 jumping/swimming; tasks sharing a bit do not run together. Run by the
 * legacy scheduler ({@link EntityAITasks}) of a hosted mob, or as a modern goal on a vanilla mob.
 */
public abstract class EntityAIBase {
    private int mutexBits;

    public abstract boolean shouldExecute();

    public boolean shouldContinueExecuting() {
        return shouldExecute();
    }

    public boolean isInterruptible() {
        return true;
    }

    public void startExecuting() {
    }

    public void resetTask() {
    }

    public void updateTask() {
    }

    public void setMutexBits(int bits) {
        mutexBits = bits;
    }

    public int getMutexBits() {
        return mutexBits;
    }
}
