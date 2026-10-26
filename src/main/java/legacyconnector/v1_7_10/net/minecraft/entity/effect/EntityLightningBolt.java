package legacyconnector.v1_7_10.net.minecraft.entity.effect;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A lightning strike. Adding it to the world strikes the modern lightning bolt at its position. */
public class EntityLightningBolt extends EntityWeatherEffect {
    public EntityLightningBolt(World world, double x, double y, double z) {
        super(world);
        setLocationAndAngles(x, y, z, 0.0F, 0.0F);
    }

    /** View of a modern lightning bolt (the bolt handed to {@code onStruckByLightning}). */
    public EntityLightningBolt(net.minecraft.world.entity.LightningBolt modern) {
        super(modern);
    }

    @Override
    protected void entityInit() {
    }

    @Override
    protected void readEntityFromNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
    }

    @Override
    protected void writeEntityToNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
    }
}
