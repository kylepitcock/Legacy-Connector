package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.BlockDoor;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.EnumDifficulty;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12's door breaker: a legacy-driven task on top of {@link EntityAIDoorInteract} (so a mod's subclass sees {@code entity},
 * {@code doorPosition} and {@code doorBlock} as the old game kept them). It breaks the door in 240 ticks with the crack animation and
 * the zombie sounds, on hard difficulty only, and only while mob griefing is on.
 */
public class EntityAIBreakDoor extends EntityAIDoorInteract {
    private int breakingTime;
    private int previousBreakProgress = -1;

    public EntityAIBreakDoor(EntityLiving entity) {
        super(entity);
    }

    private World world() {
        return Entity.mcc$get$world(entity);
    }

    @Override
    public boolean shouldExecute() {
        if (!super.shouldExecute()) return false;
        World world = world();
        return world != null && world.getGameRules().getBoolean("mobGriefing") && !BlockDoor.isOpen(world, doorPosition);
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
        breakingTime = 0;
    }

    @Override
    public boolean shouldContinueExecuting() {
        World world = world();
        if (world == null) return false;
        double d = entity.getDistanceSq(doorPosition.getX() + 0.5D, doorPosition.getY() + 0.5D, doorPosition.getZ() + 0.5D);
        return breakingTime <= 240 && !BlockDoor.isOpen(world, doorPosition) && d < 4.0D;
    }

    @Override
    public void resetTask() {
        super.resetTask();
        World world = world();
        if (world != null) world.sendBlockBreakProgress(entity.getEntityId(), doorPosition, -1);
    }

    @Override
    public void updateTask() {
        super.updateTask();
        World world = world();
        if (world == null) return;
        BlockPos pos = doorPosition;
        if (entity.getRNG().nextInt(20) == 0) world.playEvent(1019, pos, 0);
        ++breakingTime;
        int progress = (int) ((float) breakingTime / 240.0F * 10.0F);
        if (progress != previousBreakProgress) {
            world.sendBlockBreakProgress(entity.getEntityId(), pos, progress);
            previousBreakProgress = progress;
        }
        if (breakingTime == 240 && world.getDifficulty() == EnumDifficulty.HARD) {
            world.setBlockToAir(pos);
            world.playEvent(1021, pos, 0);
            world.playEvent(2001, pos, Block.getIdFromBlock(doorBlock));
        }
    }
}
