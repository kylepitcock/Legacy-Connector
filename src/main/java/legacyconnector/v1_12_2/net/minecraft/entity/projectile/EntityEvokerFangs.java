package legacyconnector.v1_12_2.net.minecraft.entity.projectile;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 evoker fangs (the modern fangs: warm-up, bite, owner). */
public class EntityEvokerFangs extends legacyconnector.v1_12_2.net.minecraft.entity.Entity {
    public EntityEvokerFangs(World world) {
        super(world);
        setSize(0.5F, 0.8F);
    }

    protected EntityEvokerFangs(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.projectile.EvokerFangs mcc$self() {
        return !mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.projectile.EvokerFangs m ? m : null;
    }

    @Override
    protected void entityInit() {
    }

    public legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase getCaster() {
        var m = mcc$self();
        return m == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(m.getOwner())
                instanceof legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase l ? l : null;
    }

    public float getAnimationProgress(float partialTicks) {
        var m = mcc$self();
        return m != null ? m.getAnimationProgress(partialTicks) : 0.0F;
    }

    @Override
    protected void readEntityFromNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
    }

    @Override
    protected void writeEntityToNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
    }
}
