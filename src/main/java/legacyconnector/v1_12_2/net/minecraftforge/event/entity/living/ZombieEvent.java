package legacyconnector.v1_12_2.net.minecraftforge.event.entity.living;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityZombie;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.event.entity.EntityEvent;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** Events about zombies. */
public class ZombieEvent extends EntityEvent {
    public ZombieEvent(EntityZombie entity) {
        super(entity);
    }

    public EntityZombie getSummoner() {
        return (EntityZombie) getEntity();
    }

    /**
     * A hurt zombie calls for aid: ALLOW spawns the custom aid (or a zombie) without the chance roll, DENY spawns none, DEFAULT
     * leaves it to the summon chance.
     */
    @Event.HasResult
    public static class SummonAidEvent extends ZombieEvent {
        private EntityZombie customSummonedAid;
        private final World world;
        private final int x;
        private final int y;
        private final int z;
        private final EntityLivingBase attacker;
        private final double summonChance;

        public SummonAidEvent(EntityZombie zombie, World world, int x, int y, int z, EntityLivingBase attacker, double summonChance) {
            super(zombie);
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.attacker = attacker;
            this.summonChance = summonChance;
        }

        public EntityZombie getCustomSummonedAid() {
            return customSummonedAid;
        }

        public void setCustomSummonedAid(EntityZombie customSummonedAid) {
            this.customSummonedAid = customSummonedAid;
        }

        public World getWorld() {
            return world;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }

        public EntityLivingBase getAttacker() {
            return attacker;
        }

        public double getSummonChance() {
            return summonChance;
        }
    }
}
