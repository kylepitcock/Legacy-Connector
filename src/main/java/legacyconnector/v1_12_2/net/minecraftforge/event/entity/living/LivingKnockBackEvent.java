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

/** A living entity is knocked back (cancel to stay; strength and direction can be changed). */
@Cancelable
public class LivingKnockBackEvent extends LivingEvent {
    protected Entity attacker;
    protected float strength;
    protected double ratioX;
    protected double ratioZ;
    protected final Entity originalAttacker;
    protected final float originalStrength;
    protected final double originalRatioX;
    protected final double originalRatioZ;

    public LivingKnockBackEvent(EntityLivingBase target, Entity attacker, float strength, double ratioX, double ratioZ) {
        super(target);
        this.attacker = this.originalAttacker = attacker;
        this.strength = this.originalStrength = strength;
        this.ratioX = this.originalRatioX = ratioX;
        this.ratioZ = this.originalRatioZ = ratioZ;
    }

    public Entity getAttacker() {
        return attacker;
    }

    public void setAttacker(Entity attacker) {
        this.attacker = attacker;
    }

    public float getStrength() {
        return strength;
    }

    public void setStrength(float strength) {
        this.strength = strength;
    }

    public double getRatioX() {
        return ratioX;
    }

    public void setRatioX(double ratioX) {
        this.ratioX = ratioX;
    }

    public double getRatioZ() {
        return ratioZ;
    }

    public void setRatioZ(double ratioZ) {
        this.ratioZ = ratioZ;
    }

    public Entity getOriginalAttacker() {
        return originalAttacker;
    }

    public float getOriginalStrength() {
        return originalStrength;
    }

    public double getOriginalRatioX() {
        return originalRatioX;
    }

    public double getOriginalRatioZ() {
        return originalRatioZ;
    }
}
