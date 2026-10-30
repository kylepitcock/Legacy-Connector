package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.BlockDoor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.pathfinding.Path;
import legacyconnector.v1_12_2.net.minecraft.pathfinding.PathNavigateGround;
import legacyconnector.v1_12_2.net.minecraft.pathfinding.PathPoint;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/**
 * The 1.12 base of the door tasks: runs while a wooden door ({@link BlockDoor} of wood) lies on the next steps of the mob's path and the
 * mob bumps into it, until the mob has passed the door. Mods subclass it for their own door handling.
 */
public abstract class EntityAIDoorInteract extends EntityAIBase {
    protected EntityLiving entity;
    protected BlockPos doorPosition = BlockPos.ORIGIN;
    protected BlockDoor doorBlock;
    boolean hasStoppedDoorInteraction;
    float entityPositionX;
    float entityPositionZ;

    public EntityAIDoorInteract(EntityLiving entity) {
        this.entity = entity;
        if (!(entity.getNavigator() instanceof PathNavigateGround)) {
            throw new IllegalArgumentException("Unsupported mob type for DoorInteractGoal");
        }
    }

    @Override
    public boolean shouldExecute() {
        if (!Entity.mcc$get$collidedHorizontally(entity)) return false;
        PathNavigateGround nav = (PathNavigateGround) entity.getNavigator();
        Path path = nav.getPath();
        if (path == null || path.isFinished() || !nav.getEnterDoors()) return false;
        int end = Math.min(path.getCurrentPathIndex() + 2, path.getCurrentPathLength());
        for (int i = 0; i < end; ++i) {
            PathPoint p = path.getPathPointFromIndex(i);
            doorPosition = new BlockPos(p.x, p.y + 1, p.z);
            if (entity.getDistanceSq(doorPosition.getX(), entity.mcc$y(), doorPosition.getZ()) <= 2.25D) {
                doorBlock = getBlockDoor(doorPosition);
                if (doorBlock != null) return true;
            }
        }
        doorPosition = new BlockPos(entity).up();
        doorBlock = getBlockDoor(doorPosition);
        return doorBlock != null;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return !hasStoppedDoorInteraction;
    }

    @Override
    public void startExecuting() {
        hasStoppedDoorInteraction = false;
        entityPositionX = (float) (doorPosition.getX() + 0.5F - entity.mcc$x());
        entityPositionZ = (float) (doorPosition.getZ() + 0.5F - entity.mcc$z());
    }

    @Override
    public void updateTask() {
        float dx = (float) (doorPosition.getX() + 0.5F - entity.mcc$x());
        float dz = (float) (doorPosition.getZ() + 0.5F - entity.mcc$z());
        if (entityPositionX * dx + entityPositionZ * dz < 0.0F) hasStoppedDoorInteraction = true;
    }

    private BlockDoor getBlockDoor(BlockPos pos) {
        var world = Entity.mcc$get$world(entity);
        if (world == null) return null;
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        return block instanceof BlockDoor door && state.getMaterial() == Material.WOOD ? door : null;
    }
}
