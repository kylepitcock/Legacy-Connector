package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.BlockDoor;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;

/**
 * Base of the door tasks (1.7.10 {@code EntityAIDoorInteract}): finds a wooden door on the entity's path that it has run into and stops when the entity
 * has walked past it. Mods subclass it for their own door behaviour (open for allies, break for raiders).
 */
public abstract class EntityAIDoorInteract extends EntityAIBase {
    protected EntityLiving theEntity;
    protected int entityPosX;
    protected int entityPosY;
    protected int entityPosZ;
    protected BlockDoor doorBlock;
    boolean hasStoppedDoorInteraction;
    float entityPositionX;
    float entityPositionZ;

    public EntityAIDoorInteract(EntityLiving entity) {
        this.theEntity = entity;
    }

    @Override
    public boolean shouldExecute() {
        var e = theEntity.mcc$entity;
        if (e == null || !legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$isCollidedHorizontally(theEntity)) return false;
        var navigator = theEntity.getNavigator();
        var path = navigator == null ? null : navigator.getPath();
        if (path == null || path.isFinished() || !navigator.getCanBreakDoors()) return false;
        for (int i = 0; i < Math.min(path.getCurrentPathIndex() + 2, path.getCurrentPathLength()); i++) {
            var point = path.getPathPointFromIndex(i);
            entityPosX = point.xCoord;
            entityPosY = point.yCoord;
            entityPosZ = point.zCoord;
            if (theEntity.getDistanceSq(entityPosX, e.getY(), entityPosZ) <= 2.25D) {
                doorBlock = func_151503_a(entityPosX, entityPosY, entityPosZ);
                if (doorBlock != null) return true;
            }
        }
        entityPosX = (int) Math.floor(e.getX());
        entityPosY = (int) Math.floor(e.getY() + 1.0D);
        entityPosZ = (int) Math.floor(e.getZ());
        doorBlock = func_151503_a(entityPosX, entityPosY, entityPosZ);
        return doorBlock != null;
    }

    @Override
    public boolean continueExecuting() {
        return !hasStoppedDoorInteraction;
    }

    @Override
    public void startExecuting() {
        hasStoppedDoorInteraction = false;
        var e = theEntity.mcc$entity;
        entityPositionX = (float) (entityPosX + 0.5F - e.getX());
        entityPositionZ = (float) (entityPosZ + 0.5F - e.getZ());
    }

    @Override
    public void updateTask() {
        var e = theEntity.mcc$entity;
        float dx = (float) (entityPosX + 0.5F - e.getX());
        float dz = (float) (entityPosZ + 0.5F - e.getZ());
        if (entityPositionX * dx + entityPositionZ * dz < 0.0F) hasStoppedDoorInteraction = true;
    }

    /** The wooden door at a position, or null (iron doors do not open for a mob). */
    private BlockDoor func_151503_a(int x, int y, int z) {
        Block block = legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$worldObj(theEntity).getBlock(x, y, z);
        return block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.wooden_door && block instanceof BlockDoor door ? door : null;
    }
}
