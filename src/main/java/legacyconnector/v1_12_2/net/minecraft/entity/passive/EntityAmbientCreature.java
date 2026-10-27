package legacyconnector.v1_12_2.net.minecraft.entity.passive;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 EntityAmbientCreature: ambient life (bats); cannot be leashed. */
public abstract class EntityAmbientCreature extends EntityLiving implements IAnimals {
    public EntityAmbientCreature(World world) {
        super(world);
    }

    protected EntityAmbientCreature(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    @Override
    public boolean canBeLeashedTo(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        return false;
    }
}
