package legacyconnector.v1_12_2.net.minecraft.entity.passive;

import static org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy;

import legacyconnector.v1_12_2.net.minecraft.entity.SharedMonsterAttributes;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataSerializers;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.EntityDataManager;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.SoundEvent;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.sounds.SoundEvents;

/** 1.12.2 mooshroom (sheared into a cow, milked into stew). */
public class EntityMooshroom extends EntityCow implements legacyconnector.v1_12_2.net.minecraftforge.common.IShearable {
    public EntityMooshroom(World world) {
        super(world);
        setSize(0.9F, 1.4F);
    }

    protected EntityMooshroom(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.animal.MushroomCow mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.animal.MushroomCow m ? m : null;
    }

    @Override
    public EntityMooshroom createChild(legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable ageable) {
        return new EntityMooshroom(mcc$get$world(this));
    }

    @Override
    public boolean isShearable(legacyconnector.v1_12_2.net.minecraft.item.ItemStack item, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return !isChild();
    }

    /** Shearing: the mooshroom turns into a cow and drops five red mushrooms. */
    @Override
    public java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> onSheared(legacyconnector.v1_12_2.net.minecraft.item.ItemStack item,
            legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, int fortune) {
        setDead();
        World w = mcc$get$world(this);
        if (w != null && !w.isRemote) {
            EntityCow cow = new EntityCow(w);
            cow.setLocationAndAngles(mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this), mcc$get$rotationYaw(this), mcc$get$rotationPitch(this));
            w.spawnEntity(cow);
        }
        java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> out = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) out.add(new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Blocks.RED_MUSHROOM));
        return out;
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/mushroom_cow");
    }
}
