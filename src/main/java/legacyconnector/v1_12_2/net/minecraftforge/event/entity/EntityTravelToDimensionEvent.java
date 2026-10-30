package legacyconnector.v1_12_2.net.minecraftforge.event.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;

/** An entity is about to change dimension (1.12 numeric id); cancel to keep it where it is. */
@Cancelable
public class EntityTravelToDimensionEvent extends EntityEvent {
    private final int dimension;

    public EntityTravelToDimensionEvent(Entity entity, int dimension) {
        super(entity);
        this.dimension = dimension;
    }

    public int getDimension() {
        return dimension;
    }
}
