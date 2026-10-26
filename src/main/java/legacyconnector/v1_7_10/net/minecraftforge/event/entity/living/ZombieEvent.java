package legacyconnector.v1_7_10.net.minecraftforge.event.entity.living;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityZombie;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Zombie events. */
public class ZombieEvent extends legacyconnector.v1_7_10.net.minecraftforge.event.entity.EntityEvent {
    public ZombieEvent(EntityZombie entity) {
        super(entity);
    }

    public EntityZombie getSummoner() {
        return (EntityZombie) entity;
    }

    /**
     * A hurt zombie calls for help: DENY stops it, ALLOW spawns {@link #customSummonedAid} (or a zombie) without the
     * chance roll.
     */
    @legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event.HasResult
    public static class SummonAidEvent extends ZombieEvent {
        public EntityZombie customSummonedAid;
        public final World world;
        public final int x;
        public final int y;
        public final int z;
        public final EntityLivingBase attacker;
        public final double summonChance;

        public SummonAidEvent(EntityZombie summoner, World world, int x, int y, int z, EntityLivingBase attacker, double summonChance) {
            super(summoner);
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.attacker = attacker;
            this.summonChance = summonChance;
        }
    }
}
