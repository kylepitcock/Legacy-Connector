package legacyconnector.v1_12_2.net.minecraft.entity.effect;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** A weather entity (lightning). */
public abstract class EntityWeatherEffect extends legacyconnector.v1_12_2.net.minecraft.entity.Entity {
    public EntityWeatherEffect(World world) {
        super(world);
    }

    protected EntityWeatherEffect(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }
}
