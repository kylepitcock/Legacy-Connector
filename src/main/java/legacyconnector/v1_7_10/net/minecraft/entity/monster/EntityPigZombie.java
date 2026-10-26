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

    /** Anger level of a mod's pigman (ticks it stays angry). */
    private int angerLevel;

    /** Turns hostile towards the entity (for 20-40 seconds); a modern zombified piglin gets angry by its own rules. */
    public void becomeAngryAt(legacyconnector.v1_7_10.net.minecraft.entity.Entity target) {
        if (mcc$entity instanceof net.minecraft.world.entity.monster.ZombifiedPiglin piglin && !mcc$hosted()) {
            if (target != null && target.mcc$entity instanceof net.minecraft.world.entity.LivingEntity living) {
                piglin.setTarget(living);
                piglin.setPersistentAngerTarget(living.getUUID());
                piglin.startPersistentAngerTimer();
            }
            return;
        }
        entityToAttack = target;
        angerLevel = 400 + rand.nextInt(400);
    }

    /** 1.7.10: zombie pigmen keep the old AI (it walks at the fixed old-AI speed). */
    @Override
    public boolean isAIEnabled() {
        return false;
    }
}
