package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.entity.SharedMonsterAttributes;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The giant: a zombie six times as large (100 health, 50 attack). A plain one is the modern giant. */
public class EntityGiantZombie extends EntityMob {
    /** View of a modern giant. */
    public EntityGiantZombie(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    public EntityGiantZombie(World world) {
        super(world);
        this.yOffset *= 6.0F;
        setSize(0.6F * 6.0F, 1.8F * 6.0F);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(100.0D);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.5D);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(50.0D);
    }

    @Override
    public float getBlockPathWeight(int x, int y, int z) {
        return mcc$world().getLightBrightness(x, y, z) - 0.5F;
    }
}
