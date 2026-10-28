package legacyconnector.v1_12_2.net.minecraft.entity.projectile;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 experience bottle (breaks into experience). */
public class EntityExpBottle extends EntityThrowable {
    public EntityExpBottle(World world) {
        super(world);
    }

    public EntityExpBottle(World world, EntityLivingBase thrower) {
        super(world, thrower);
    }

    public EntityExpBottle(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    protected EntityExpBottle(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    private boolean mcc$server() {
        return mcc$entity != null && !mcc$entity.level().isClientSide();
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (mcc$server()) {
            var level = mcc$entity.level();
            level.levelEvent(2002, mcc$entity.blockPosition(), net.minecraft.world.item.alchemy.PotionContents.getColor(
                    net.minecraft.world.item.alchemy.Potions.WATER));
            int xp = 3 + rand.nextInt(5) + rand.nextInt(5);
            if (level instanceof net.minecraft.server.level.ServerLevel sl) net.minecraft.world.entity.ExperienceOrb.award(sl, mcc$entity.position(), xp);
            setDead();
        }
    }
}
