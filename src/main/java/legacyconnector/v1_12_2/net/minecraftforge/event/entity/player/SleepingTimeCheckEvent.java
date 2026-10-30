package legacyconnector.v1_12_2.net.minecraftforge.event.entity.player;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** Whether a sleeping player may keep sleeping (ALLOW: at any time, DENY: wake up, DEFAULT: only at night). */
@Event.HasResult
public class SleepingTimeCheckEvent extends PlayerEvent {
    private final BlockPos sleepingLocation;

    public SleepingTimeCheckEvent(EntityPlayer player, BlockPos sleepingLocation) {
        super(player);
        this.sleepingLocation = sleepingLocation;
    }

    public BlockPos getSleepingLocation() {
        return sleepingLocation;
    }
}
