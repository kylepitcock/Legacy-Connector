package legacyconnector.v1_12_2.net.minecraft.entity.effect;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** A lightning bolt: a view of the modern bolt ({@code effectOnly} bolts make no fire and hurt nothing). */
public class EntityLightningBolt extends EntityWeatherEffect {
    public EntityLightningBolt(World world, double x, double y, double z, boolean effectOnly) {
        super(world);
        setLocationAndAngles(x, y, z, 0.0F, 0.0F);
        if (mcc$entity instanceof net.minecraft.world.entity.LightningBolt bolt) bolt.setVisualOnly(effectOnly);
    }

    public EntityLightningBolt(net.minecraft.world.entity.LightningBolt modern) {
        super(modern);
    }
}
