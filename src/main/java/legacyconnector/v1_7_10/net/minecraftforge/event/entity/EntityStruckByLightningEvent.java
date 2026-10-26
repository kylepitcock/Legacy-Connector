package legacyconnector.v1_7_10.net.minecraftforge.event.entity;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.effect.EntityLightningBolt;

/** Lightning hits an entity; cancel it to spare the entity. */
@legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable
public class EntityStruckByLightningEvent extends EntityEvent {
    public final EntityLightningBolt lightning;

    public EntityStruckByLightningEvent(Entity entity, EntityLightningBolt lightning) {
        super(entity);
        this.lightning = lightning;
    }
}
