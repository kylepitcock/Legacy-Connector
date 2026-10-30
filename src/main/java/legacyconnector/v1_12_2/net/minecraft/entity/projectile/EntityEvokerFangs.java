package legacyconnector.v1_12_2.net.minecraft.entity.projectile;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 evoker fangs (the modern fangs: warm-up, bite, owner). */
public class EntityEvokerFangs extends legacyconnector.v1_12_2.net.minecraft.entity.Entity {
    public EntityEvokerFangs(World world) {
        super(world);
        setSize(0.5F, 0.8F);
    }

    /**
     * Fangs at a position facing {@code rotation} (radians) that bite after {@code warmupDelay} ticks, owned by {@code caster}: for the
     * exact class a modern fangs entity made through its own constructor.
     */
    public EntityEvokerFangs(World world, double x, double y, double z, float rotation, int warmupDelay,
            legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase caster) {
        this(world);
        if (mcc$self() != null) {
            var owner = caster == null ? null : caster.mcc$living();
            org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.bind(this,
                    new net.minecraft.world.entity.projectile.EvokerFangs(mcc$self().level(), x, y, z, rotation, warmupDelay, owner));
            return;
        }
        mcc$warmupDelay = warmupDelay;
        mcc$caster = caster;
        mcc$set$rotationYaw(this, rotation * (180F / (float) Math.PI));
        setPosition(x, y, z);
    }

    private int mcc$warmupDelay;
    private legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase mcc$caster;

    public void setCaster(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase caster) {
        var m = mcc$self();
        if (m != null) m.setOwner(caster == null ? null : caster.mcc$living());
        mcc$caster = caster;
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
        if (m == null) return mcc$caster;
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(m.getOwner())
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
