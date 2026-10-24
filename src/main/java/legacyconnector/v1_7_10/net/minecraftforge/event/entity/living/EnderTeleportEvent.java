package legacyconnector.v1_7_10.net.minecraftforge.event.entity.living;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;

/** An enderman or ender pearl teleport: handlers may move the target or cancel it. */
@Cancelable
public class EnderTeleportEvent extends LivingEvent {
    public double targetX;
    public double targetY;
    public double targetZ;
    public float attackDamage;

    public EnderTeleportEvent(EntityLivingBase entity, double targetX, double targetY, double targetZ, float attackDamage) {
        super(entity);
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.attackDamage = attackDamage;
    }
}
