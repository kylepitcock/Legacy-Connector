package legacyconnector.v1_20_1.net.minecraftforge.event.entity.living;

import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.event.entity.EntityEvent;

/**
 * Forge 1.20.1's zombie event base ({@code ZombieEvent}) and its reinforcement request ({@code ZombieEvent.SummonAidEvent}, a result event a mod answers
 * with ALLOW or DENY). NeoForge 1.21.1 dropped the hook: the reinforcement roll sits in the middle of {@code Zombie.hurt} (chance attribute, difficulty,
 * game rule, then the placement loop), with no NeoForge event. These stand-ins let a registration succeed; they are not fired.
 */
public class ZombieEvent extends EntityEvent {
    public ZombieEvent(Zombie zombie) {
        super(zombie);
    }

    public Zombie getEntity() {
        return (Zombie) super.getEntity();
    }
}
