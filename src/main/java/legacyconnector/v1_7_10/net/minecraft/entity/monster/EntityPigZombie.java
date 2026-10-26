package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 zombie pigman: a zombie that is immune to fire. */
public class EntityPigZombie extends EntityZombie {
    /** View of a modern vanilla mob (mods test and cast vanilla mobs against this class). */
    public EntityPigZombie(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    public EntityPigZombie(World world) {
        super(world);
        this.isImmuneToFire = true;
    }

    /** 1.7.10: zombie pigmen keep the old AI (it walks at the fixed old-AI speed). */
    @Override
    public boolean isAIEnabled() {
        return false;
    }
}
