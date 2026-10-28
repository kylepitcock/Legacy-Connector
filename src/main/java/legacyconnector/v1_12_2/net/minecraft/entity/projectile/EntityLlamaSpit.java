package legacyconnector.v1_12_2.net.minecraft.entity.projectile;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 llama spit (the modern spit). */
public class EntityLlamaSpit extends legacyconnector.v1_12_2.net.minecraft.entity.Entity {
    public EntityLlamaSpit(World world) {
        super(world);
        setSize(0.25F, 0.25F);
    }

    protected EntityLlamaSpit(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.projectile.LlamaSpit mcc$self() {
        return !mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.projectile.LlamaSpit m ? m : null;
    }

    @Override
    protected void entityInit() {
    }

    @Override
    protected void readEntityFromNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
    }

    @Override
    protected void writeEntityToNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
    }
}
