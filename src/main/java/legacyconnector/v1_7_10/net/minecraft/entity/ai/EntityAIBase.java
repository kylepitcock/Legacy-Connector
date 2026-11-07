package legacyconnector.v1_7_10.net.minecraft.entity.ai;

/** Legacy AI task. Not driven by the modern goal system yet; exists so mods that touch AI lists link. */
public abstract class EntityAIBase {
    private int mutexBits;

    /** The 1.5.2 constructors take an EntityLiving; the shim's tasks are driven through an EntityCreature (null for a living entity that is none). */
    protected static legacyconnector.v1_7_10.net.minecraft.entity.EntityCreature mcc$creature(legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving e) {
        return e instanceof legacyconnector.v1_7_10.net.minecraft.entity.EntityCreature c ? c : null;
    }

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
