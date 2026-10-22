package legacyconnector.v1_7_10.net.minecraftforge.event.entity.living;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;

@Cancelable
public class LivingAttackEvent extends LivingEvent {
    public final DamageSource source;
    /** Sic: the 1.7.10 field name. Changes are written back to the damage dealt. */
    public final float ammount;

    public LivingAttackEvent(EntityLivingBase entity, DamageSource source, float ammount) {
        super(entity);
        this.source = source;
        this.ammount = ammount;
    }
}
