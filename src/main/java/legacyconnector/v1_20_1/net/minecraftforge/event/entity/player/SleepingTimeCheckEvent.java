package legacyconnector.v1_20_1.net.minecraftforge.event.entity.player;

import java.util.Optional;
import legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Forge 1.20.1's "may the player sleep at this time of day" event; posted from NeoForge's {@code CanContinueSleepingEvent} (ALLOW or DENY decides). */
public class SleepingTimeCheckEvent extends PlayerEvent {
    private final Optional<BlockPos> sleepingLocation;
    private Event$Result result = Event$Result.DEFAULT;

    public SleepingTimeCheckEvent(Player player, Optional<BlockPos> sleepingLocation) {
        super(player);
        this.sleepingLocation = sleepingLocation;
    }

    public Optional<BlockPos> getSleepingLocation() {
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
