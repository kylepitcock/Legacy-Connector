package legacyconnector.v1_12_2.net.minecraftforge.event.entity.living;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** An enderman or ender pearl teleport (cancel to stay; the target can be changed). */
@Cancelable
public class EnderTeleportEvent extends LivingEvent {
    private double targetX;
    private double targetY;
    private double targetZ;
    private float attackDamage;

    public EnderTeleportEvent(EntityLivingBase entity, double targetX, double targetY, double targetZ, float attackDamage) {
        super(entity);
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.attackDamage = attackDamage;
    }

    public double getTargetX() {
        return targetX;
    }

    public void setTargetX(double targetX) {
        this.targetX = targetX;
    }

    public double getTargetY() {
        return targetY;
    }

    public void setTargetY(double targetY) {
        this.targetY = targetY;
    }

    public double getTargetZ() {
        return targetZ;
    }

    public void setTargetZ(double targetZ) {
        this.targetZ = targetZ;
    }

    public float getAttackDamage() {
        return attackDamage;
    }

    public void setAttackDamage(float attackDamage) {
        this.attackDamage = attackDamage;
    }
}
