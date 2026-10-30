package legacyconnector.v1_12_2.net.minecraft.entity.projectile;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 spectral arrow: makes what it hits glow (the modern spectral arrow; mods' subclasses apply the glowing themselves). */
public class EntitySpectralArrow extends EntityArrow {
    private int duration = 200;

    public EntitySpectralArrow(World world) {
        super(world);
    }

    public EntitySpectralArrow(World world, EntityLivingBase shooter) {
        super(world, shooter);
    }

    public EntitySpectralArrow(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    protected EntitySpectralArrow(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    @Override
    protected ItemStack getArrowStack() {
        return new ItemStack(legacyconnector.v1_12_2.net.minecraft.item.Item.getByNameOrId("minecraft:spectral_arrow"));
    }

    @Override
    protected void arrowHit(EntityLivingBase living) {
        super.arrowHit(living);
        if (living != null && living.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l) {
            l.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, duration, 0), mcc$entity);
        }
    }

    @Override
    protected void readEntityFromNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (tag.hasKey("Duration")) duration = tag.getInteger("Duration");
    }

    @Override
    protected void writeEntityToNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("Duration", duration);
    }
}
