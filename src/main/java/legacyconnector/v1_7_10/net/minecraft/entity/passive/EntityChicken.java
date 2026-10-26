package legacyconnector.v1_7_10.net.minecraft.entity.passive;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityAgeable;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 chicken: lays an egg every five to ten minutes. */
public class EntityChicken extends EntityAnimal {
    public float field_70886_e;
    public float destPos;
    public float field_70884_g;
    public float field_70888_h;
    public float field_70889_i = 1.0F;
    public int timeUntilNextEgg;

    /** View of a modern vanilla mob (mods test and cast vanilla mobs against this class). */
    public EntityChicken(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    public EntityChicken(World world) {
        super(world);
        setSize(0.3F, 0.7F);
        this.timeUntilNextEgg = getRNG().nextInt(6000) + 6000;
    }

    @Override
    public EntityAgeable createChild(EntityAgeable mate) {
        return new EntityChicken(mcc$world());
    }

    /** 1.7.10: the task AI (mods' subclasses inherit it: their walking speed follows the move helper). */
    @Override
    public boolean isAIEnabled() {
        return true;
    }
}
