package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityWolf;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A wolf begging (1.7.10 {@code EntityAIBeg}): while a player near it holds a bone (or, for a tame wolf, its food) it tilts its head and looks at them. */
public class EntityAIBeg extends EntityAIBase {
    private final EntityWolf theWolf;
    private EntityPlayer thePlayer;
    private final World worldObject;
    private final float minPlayerDistance;
    private int field_75384_e;

    public EntityAIBeg(EntityWolf wolf, float minDistance) {
        this.theWolf = wolf;
        this.worldObject = Entity.mcc$get$worldObj(wolf);
        this.minPlayerDistance = minDistance;
        setMutexBits(2);
    }

    @Override
    public boolean shouldExecute() {
        this.thePlayer = worldObject.getClosestPlayerToEntity(theWolf, minPlayerDistance);
        return thePlayer != null && hasPlayerGotBoneInHand(thePlayer);
    }

    @Override
    public boolean continueExecuting() {
        if (!thePlayer.isEntityAlive()) return false;
        if (theWolf.getDistanceSqToEntity(thePlayer) > (double) (minPlayerDistance * minPlayerDistance)) return false;
        return field_75384_e > 0 && hasPlayerGotBoneInHand(thePlayer);
    }

    @Override
    public void startExecuting() {
        theWolf.func_70918_i(true);
        this.field_75384_e = 40 + theWolf.getRNG().nextInt(40);
    }

    @Override
    public void resetTask() {
        theWolf.func_70918_i(false);
        this.thePlayer = null;
    }

    @Override
    public void updateTask() {
        theWolf.getLookHelper().setLookPosition(Entity.mcc$get$posX(thePlayer), Entity.mcc$get$posY(thePlayer) + thePlayer.getEyeHeight(),
                Entity.mcc$get$posZ(thePlayer), 10.0F, (float) theWolf.getVerticalFaceSpeed());
        this.field_75384_e--;
    }

    private boolean hasPlayerGotBoneInHand(EntityPlayer player) {
        ItemStack held = player.inventory.getCurrentItem();
        if (held == null) return false;
        return !theWolf.isTamed() && held.getItem() == Items.bone || theWolf.isBreedingItem(held);
    }
}
