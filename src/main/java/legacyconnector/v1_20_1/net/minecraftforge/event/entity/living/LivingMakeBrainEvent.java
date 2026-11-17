package legacyconnector.v1_20_1.net.minecraftforge.event.entity.living;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/**
 * Forge 1.20.1's brain creation event ({@code LivingMakeBrainEvent}: a mod adds sensors, memories or activities to the brain a living entity just made).
 * NeoForge 1.21.1 has no counterpart and nothing to post it from without a mixin into every {@code makeBrain} override, so this stand-in only lets the
 * registration succeed; it is not fired (docs/wiki/Legacy-1.20.1-Client-Events.md, "Other events").
 */
public class LivingMakeBrainEvent extends LivingEvent {
    private final Brain<?> brain;

    public LivingMakeBrainEvent(LivingEntity entity, Brain<?> brain) {
        super(entity);
        this.brain = brain;
    }

    public Brain<?> getBrain() {
        return brain;
    }
}
