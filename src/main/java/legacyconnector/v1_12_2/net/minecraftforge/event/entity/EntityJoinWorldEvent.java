package legacyconnector.v1_12_2.net.minecraftforge.event.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** An entity is added to a world; canceling it keeps the entity out. */
@legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable
public class EntityJoinWorldEvent extends EntityEvent {
    private final World world;

    public EntityJoinWorldEvent(Entity entity, World world) {
        super(entity);
        this.world = world;
    }

    public World getWorld() {
        return world;
    }
}
