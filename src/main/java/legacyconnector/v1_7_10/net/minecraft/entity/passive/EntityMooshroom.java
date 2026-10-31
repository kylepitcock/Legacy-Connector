package legacyconnector.v1_7_10.net.minecraft.entity.passive;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityAgeable;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 mooshroom: a cow that can be sheared and milked for soup. */
public class EntityMooshroom extends EntityCow implements legacyconnector.v1_7_10.net.minecraftforge.common.IShearable {
    /** View of a modern vanilla mob (mods test and cast vanilla mobs against this class). */
    public EntityMooshroom(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    public EntityMooshroom(World world) {
        super(world);
        setSize(0.9F, 1.3F);
    }

    @Override
    public EntityAgeable createChild(EntityAgeable mate) {
        return new EntityMooshroom(mcc$world());
    }

    /** Forge: an adult mooshroom can be sheared (it becomes a cow and drops mushrooms). */
    @Override
    public boolean isShearable(legacyconnector.v1_7_10.net.minecraft.item.ItemStack item, legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y,
            int z) {
        if (mcc$entity instanceof net.minecraft.world.entity.animal.MushroomCow m && !mcc$hosted()) return m.readyForShearing();
        return !isChild();
    }

    @Override
    public java.util.ArrayList<legacyconnector.v1_7_10.net.minecraft.item.ItemStack> onSheared(legacyconnector.v1_7_10.net.minecraft.item.ItemStack item,
            legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z, int fortune) {
        java.util.ArrayList<legacyconnector.v1_7_10.net.minecraft.item.ItemStack> drops = new java.util.ArrayList<>();
        if (mcc$entity instanceof net.minecraft.world.entity.animal.MushroomCow m && !mcc$hosted()) {
            m.shear(net.minecraft.sounds.SoundSource.PLAYERS);
            return drops;
        }
        setDead();
        EntityCow cow = new EntityCow(mcc$world());
        legacyconnector.v1_7_10.net.minecraft.entity.Entity self = this;
        cow.setLocationAndAngles(legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$posX(self), legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$posY(self),
                legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$posZ(self), legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$rotationYaw(self),
                legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$rotationPitch(self));
        mcc$world().spawnEntityInWorld(cow);
        for (int i = 0; i < 5; i++) {
            drops.add(new legacyconnector.v1_7_10.net.minecraft.item.ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Blocks.red_mushroom));
        }
        return drops;
    }
}
