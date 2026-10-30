package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityWolf;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** A wolf tilts its head at a nearby player holding a bone (tamed) or its breeding food. */
public class EntityAIBeg extends EntityAIBase {
    private final EntityWolf wolf;
    private EntityPlayer player;
    private final World world;
    private final float minPlayerDistance;
    private int timeoutCounter;

    public EntityAIBeg(EntityWolf wolf, float minDistance) {
        this.wolf = wolf;
        this.world = Entity.mcc$get$world(wolf);
        this.minPlayerDistance = minDistance;
        setMutexBits(2);
    }

    @Override
    public boolean shouldExecute() {
        World w = world != null ? world : Entity.mcc$get$world(wolf);
        player = w == null ? null : w.getClosestPlayerToEntity(wolf, minPlayerDistance);
        return player != null && hasTemptationItemInHand(player);
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (player == null || !player.isEntityAlive()) return false;
        if (wolf.getDistanceSq(player) > minPlayerDistance * minPlayerDistance) return false;
        return timeoutCounter > 0 && hasTemptationItemInHand(player);
    }

    @Override
    public void startExecuting() {
        wolf.setBegging(true);
        timeoutCounter = 40 + wolf.getRNG().nextInt(40);
    }

    @Override
    public void resetTask() {
        wolf.setBegging(false);
        player = null;
    }

    @Override
    public void updateTask() {
        wolf.getLookHelper().setLookPosition(player.mcc$x(), player.mcc$y() + player.getEyeHeight(), player.mcc$z(), 10.0F, wolf.getVerticalFaceSpeed());
        --timeoutCounter;
    }

    private boolean hasTemptationItemInHand(EntityPlayer p) {
        for (EnumHand hand : EnumHand.values()) {
            ItemStack stack = p.getHeldItem(hand);
            if (wolf.isTamed() && !stack.isEmpty() && stack.getItem().mcc$modern == net.minecraft.world.item.Items.BONE) return true;
            if (wolf.isBreedingItem(stack)) return true;
        }
        return false;
    }
}
