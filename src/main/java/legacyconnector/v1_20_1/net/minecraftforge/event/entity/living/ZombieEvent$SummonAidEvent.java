package legacyconnector.v1_20_1.net.minecraftforge.event.entity.living;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/** Forge 1.20.1's reinforcement event; a stand-in that is not fired (see {@link ZombieEvent}). */
public class ZombieEvent$SummonAidEvent extends ZombieEvent {
    private final Zombie customSummonedAid;
    private final Level level;
    private final int x;
    private final int y;
    private final int z;
    private final LivingEntity attacker;
    private final double summonChance;

    public ZombieEvent$SummonAidEvent(Zombie zombie, Level level, int x, int y, int z, LivingEntity attacker, double summonChance) {
        super(zombie);
        this.customSummonedAid = null;
        this.level = level;
        this.x = x;
        this.y = y;
        this.z = z;
        this.attacker = attacker;
        this.summonChance = summonChance;
    }

    public Zombie getSummoned() {
        return customSummonedAid;
    }

    public Level getLevel() {
        return level;
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

    public LivingEntity getAttacker() {
        return attacker;
    }

    public double getSummonChance() {
        return summonChance;
    }
}
