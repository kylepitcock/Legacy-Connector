package legacyconnector.v1_7_10.net.minecraft.entity.passive;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityAgeable;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 mooshroom: a cow that can be sheared and milked for soup. */
public class EntityMooshroom extends EntityCow {
    public EntityMooshroom(World world) {
        super(world);
        setSize(0.9F, 1.3F);
    }

    @Override
    public EntityAgeable createChild(EntityAgeable mate) {
        return new EntityMooshroom(mcc$world());
    }
}
