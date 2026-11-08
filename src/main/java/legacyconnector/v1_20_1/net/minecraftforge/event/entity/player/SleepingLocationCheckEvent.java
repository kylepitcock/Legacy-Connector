package legacyconnector.v1_20_1.net.minecraftforge.event.entity.player;

import legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/**
 * Forge 1.20.1's "is the entity's sleeping position still valid" event (mods let players sleep on hammocks and sleeping bags). Posted from NeoForge's
 * {@code CanContinueSleepingEvent}; ALLOW keeps the entity asleep, DENY wakes it.
 */
public class SleepingLocationCheckEvent extends LivingEvent {
    private final BlockPos sleepingLocation;
    private Event$Result result = Event$Result.DEFAULT;

    public SleepingLocationCheckEvent(LivingEntity sleeper, BlockPos sleepingLocation) {
        super(sleeper);
        this.sleepingLocation = sleepingLocation;
    }

    public BlockPos getSleepingLocation() {
        return sleepingLocation;
    }

    public Event$Result getResult() {
        return result;
    }

    public void setResult(Event$Result result) {
        this.result = result;
    }

    public boolean hasResult() {
        return true;
    }
}
