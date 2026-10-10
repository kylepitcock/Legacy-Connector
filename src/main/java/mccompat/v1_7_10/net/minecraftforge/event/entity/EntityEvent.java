package mccompat.v1_7_10.net.minecraftforge.event.entity;

import mccompat.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import mccompat.v1_7_10.net.minecraft.entity.Entity;

public class EntityEvent extends Event {
    public final Entity entity;

    public EntityEvent(Entity entity) {
        this.entity = entity;
    }
}
