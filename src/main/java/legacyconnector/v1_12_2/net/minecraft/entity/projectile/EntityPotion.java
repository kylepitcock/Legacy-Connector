package legacyconnector.v1_12_2.net.minecraft.entity.projectile;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 thrown potion: splashes its effects on the living entities nearby (lingering ones leave a cloud). */
public class EntityPotion extends EntityThrowable {
    public EntityPotion(World world) {
        super(world);
    }

    public EntityPotion(World world, EntityLivingBase thrower) {
        super(world, thrower);
    }

    public EntityPotion(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    private legacyconnector.v1_12_2.net.minecraft.item.ItemStack potion = legacyconnector.v1_12_2.net.minecraft.item.ItemStack.EMPTY;

    public EntityPotion(World world, EntityLivingBase thrower, legacyconnector.v1_12_2.net.minecraft.item.ItemStack potion) {
        super(world, thrower);
        setItem(potion);
    }

    public EntityPotion(World world, double x, double y, double z, legacyconnector.v1_12_2.net.minecraft.item.ItemStack potion) {
        super(world, x, y, z);
        setItem(potion);
    }

    public legacyconnector.v1_12_2.net.minecraft.item.ItemStack getPotion() {
        if (!mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.projectile.ThrownPotion t) {
            return org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.fromModern(t.getItem());
        }
        return potion;
    }

    public void setItem(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        potion = stack == null ? legacyconnector.v1_12_2.net.minecraft.item.ItemStack.EMPTY : stack;
        if (!mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.projectile.ThrownPotion t) {
            t.setItem(org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(potion));
        }
    }

    @Override
    protected float getGravityVelocity() {
        return 0.05F;
    }

    protected EntityPotion(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    private boolean mcc$server() {
        return mcc$entity != null && !mcc$entity.level().isClientSide();
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (!mcc$server()) return;
        var stack = getPotion();
        var effects = legacyconnector.v1_12_2.net.minecraft.potion.PotionUtils.getEffectsFromStack(stack);
        var level = mcc$entity.level();
        boolean lingering = stack.getItem() == legacyconnector.v1_12_2.net.minecraft.item.Item.getByNameOrId("minecraft:lingering_potion");
        if (lingering) {
            var cloud = new net.minecraft.world.entity.AreaEffectCloud(level, mcc$x(), mcc$y(), mcc$z());
            cloud.setRadius(3.0F);
            cloud.setRadiusOnUse(-0.5F);
            cloud.setWaitTime(10);
            cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
            for (var e : effects) {
                var m = e.mcc$toModern();
                if (m != null) cloud.addEffect(m);
            }
            level.addFreshEntity(cloud);
        } else if (!effects.isEmpty()) {
            var box = mcc$entity.getBoundingBox().inflate(4.0D, 2.0D, 4.0D);
            for (var living : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box)) {
                if (!living.isAffectedByPotions()) continue;
                double d = mcc$entity.distanceToSqr(living);
                if (d >= 16.0D) continue;
                double scale = 1.0D - Math.sqrt(d) / 4.0D;
                if (result.entityHit != null && result.entityHit.mcc$entity == living) scale = 1.0D;
                for (var e : effects) {
                    var m = e.mcc$toModern();
                    if (m == null) continue;
                    if (m.getEffect().value().isInstantenous()) {
                        m.getEffect().value().applyInstantenousEffect(mcc$entity, getThrower() == null ? null : getThrower().mcc$entity, living, m.getAmplifier(), scale);
                    } else {
                        int duration = (int) (scale * m.getDuration() + 0.5D);
                        if (duration > 20) living.addEffect(new net.minecraft.world.effect.MobEffectInstance(m.getEffect(), duration, m.getAmplifier(),
                                m.isAmbient(), m.isVisible()), mcc$entity);
                    }
                }
            }
        }
        level.levelEvent(lingering ? 2007 : 2002, mcc$entity.blockPosition(), legacyconnector.v1_12_2.net.minecraft.potion.PotionUtils.getColor(stack));
        setDead();
    }
}
