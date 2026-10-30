package legacyconnector.v1_12_2.net.minecraftforge.event.entity.player;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** Whether a sleeping player's bed is still a place to sleep (ALLOW: keep sleeping anywhere, DENY: wake up, DEFAULT: only in a bed). */
@Event.HasResult
public class SleepingLocationCheckEvent extends PlayerEvent {
    private final BlockPos sleepingLocation;

    public SleepingLocationCheckEvent(EntityPlayer player, BlockPos sleepingLocation) {
        super(player);
        this.sleepingLocation = sleepingLocation;
    }

    public BlockPos getSleepingLocation() {
        return sleepingLocation;
    }
}
