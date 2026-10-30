package legacyconnector.v1_12_2.net.minecraftforge.event.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** Whether an entity may change the world (ALLOW or DENY override the mobGriefing game rule; posted from the modern event). */
@Event.HasResult
public class EntityMobGriefingEvent extends EntityEvent {
    public EntityMobGriefingEvent(Entity entity) {
        super(entity);
    }
}
