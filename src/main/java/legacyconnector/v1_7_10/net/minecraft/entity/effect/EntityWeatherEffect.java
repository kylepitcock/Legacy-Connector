package legacyconnector.v1_7_10.net.minecraft.entity.effect;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Base of weather effects (lightning; mods' fake lightning and sky effects). */
public abstract class EntityWeatherEffect extends Entity {
    public EntityWeatherEffect(World world) {
        super(world);
    }

    /** View of a modern entity. */
    protected EntityWeatherEffect(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }
}
