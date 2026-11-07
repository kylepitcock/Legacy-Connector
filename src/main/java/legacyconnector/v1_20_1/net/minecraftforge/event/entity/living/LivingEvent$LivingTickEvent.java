package legacyconnector.v1_20_1.net.minecraftforge.event.entity.living;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/** Forge 1.20.1's per-entity tick event ({@code LivingEvent.LivingTickEvent}); posted from NeoForge's {@code EntityTickEvent.Pre} for living entities. */
public class LivingEvent$LivingTickEvent extends LivingEvent implements ICancellableEvent {
    public LivingEvent$LivingTickEvent(LivingEntity entity) {
        super(entity);
    }
}
