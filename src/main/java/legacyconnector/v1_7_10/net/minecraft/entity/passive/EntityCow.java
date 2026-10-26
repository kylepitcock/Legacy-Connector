package legacyconnector.v1_7_10.net.minecraft.entity.passive;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityAgeable;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 cow (and the base of mods' cattle). */
public class EntityCow extends EntityAnimal {
    /** View of a modern vanilla mob (mods test and cast vanilla mobs against this class). */
    public EntityCow(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    public EntityCow(World world) {
        super(world);
        setSize(0.9F, 1.3F);
    }

    @Override
    public EntityAgeable createChild(EntityAgeable mate) {
        return new EntityCow(mcc$world());
    }
}
