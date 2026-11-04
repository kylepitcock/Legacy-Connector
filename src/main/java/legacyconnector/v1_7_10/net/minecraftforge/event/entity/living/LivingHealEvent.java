package legacyconnector.v1_7_10.net.minecraftforge.event.entity.living;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;

/** Forge's heal event: posted before an entity regains health; cancel it or change {@code amount}. */
@Cancelable
public class LivingHealEvent extends LivingEvent {
    public float amount;

    public LivingHealEvent(EntityLivingBase entity, float amount) {
        super(entity);
        this.amount = amount;
    }
}
