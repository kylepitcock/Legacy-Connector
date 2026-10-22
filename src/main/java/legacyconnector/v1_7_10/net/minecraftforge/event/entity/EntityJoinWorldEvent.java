package legacyconnector.v1_7_10.net.minecraftforge.event.entity;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.world.World;

@Cancelable
public class EntityJoinWorldEvent extends EntityEvent {
    public final World world;

    public EntityJoinWorldEvent(Entity entity, World world) {
        super(entity);
        this.world = world;
    }
}
