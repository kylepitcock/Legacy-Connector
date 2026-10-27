package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import org.kylepitcock.legacyconnector.v1_12_2.entity.EntityQueries1122;

/** Walks to a nearby block the subclass picks with {@link #shouldMoveTo} and stays there a while. */
public abstract class EntityAIMoveToBlock extends EntityAIBase {
    private final EntityCreature creature;
    private final double movementSpeed;
    protected int runDelay;
    private int timeoutCounter;
    private int maxStayTicks;
    protected BlockPos destinationBlock = BlockPos.ORIGIN;
    private boolean isAboveDestination;
    private final int searchLength;

    public EntityAIMoveToBlock(EntityCreature creature, double speed, int length) {
        this.creature = creature;
        this.movementSpeed = speed;
        this.searchLength = length;
        setMutexBits(5);
    }

    @Override
    public boolean shouldExecute() {
        if (runDelay > 0) {
            --runDelay;
            return false;
        }
        runDelay = 200 + creature.getRNG().nextInt(200);
        return searchForDestination();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return timeoutCounter >= -maxStayTicks && timeoutCounter <= 1200 && shouldMoveTo(creature.mcc$world(), destinationBlock);
    }

    @Override
    public void startExecuting() {
        creature.getNavigator().tryMoveToXYZ(destinationBlock.getX() + 0.5D, destinationBlock.getY() + 1, destinationBlock.getZ() + 0.5D, movementSpeed);
        timeoutCounter = 0;
        maxStayTicks = creature.getRNG().nextInt(creature.getRNG().nextInt(1200) + 1200) + 1200;
    }

    @Override
    public void updateTask() {
        if (creature.getDistanceSqToCenter(destinationBlock.up()) > 1.0D) {
            isAboveDestination = false;
            ++timeoutCounter;
            if (timeoutCounter % 40 == 0) {
                creature.getNavigator().tryMoveToXYZ(destinationBlock.getX() + 0.5D, destinationBlock.getY() + 1, destinationBlock.getZ() + 0.5D, movementSpeed);
            }
        } else {
            isAboveDestination = true;
            --timeoutCounter;
        }
    }

    protected boolean getIsAboveDestination() {
        return isAboveDestination;
    }

    private boolean searchForDestination() {
        int i = searchLength;
        BlockPos origin = creature.getPosition();
        for (int k = 0; k <= 1; k = k > 0 ? -k : 1 - k) {
            for (int l = 0; l < i; ++l) {
                for (int i1 = 0; i1 <= l; i1 = i1 > 0 ? -i1 : 1 - i1) {
                    for (int j1 = i1 < l && i1 > -l ? l : 0; j1 <= l; j1 = j1 > 0 ? -j1 : 1 - j1) {
                        BlockPos pos = origin.add(i1, k - 1, j1);
                        if (creature.isWithinHomeDistanceFromPosition(pos) && shouldMoveTo(creature.mcc$world(), pos)) {
                            destinationBlock = pos;
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    protected abstract boolean shouldMoveTo(legacyconnector.v1_12_2.net.minecraft.world.World world, BlockPos pos);
}
