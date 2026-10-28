package legacyconnector.v1_12_2.net.minecraft.entity.item;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 rideable minecart. */
public class EntityMinecartEmpty extends EntityMinecart {
    public EntityMinecartEmpty(World world) {
        super(world);
    }

    public EntityMinecartEmpty(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    protected EntityMinecartEmpty(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    @Override
    public Type getType() {
        return Type.RIDEABLE;
    }

    @Override
    public boolean processInitialInteract(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand) {
        if (!mcc$hosted()) return super.processInitialInteract(player, hand);
        if (player.isSneaking() || isBeingRidden()) return false;
        if (mcc$entity != null && !mcc$entity.level().isClientSide()) player.startRiding(this);
        return true;
    }
}
