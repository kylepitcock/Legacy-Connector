package legacyconnector.v1_7_10.net.minecraft.entity.ai;

/** Legacy AI task. Not driven by the modern goal system yet; exists so mods that touch AI lists link. */
public abstract class EntityAIBase {
    private int mutexBits;

    public abstract boolean shouldExecute();

    public boolean continueExecuting() {
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
