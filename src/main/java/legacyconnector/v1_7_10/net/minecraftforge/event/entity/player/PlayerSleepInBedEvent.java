package legacyconnector.v1_7_10.net.minecraftforge.event.entity.player;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;

/** A player tries to sleep in a bed; set {@link #result} to refuse (or to allow, OK) instead of the normal checks. */
public class PlayerSleepInBedEvent extends PlayerEvent {
    public EntityPlayer.EnumStatus result = null;
    public final int x;
    public final int y;
    public final int z;

    public PlayerSleepInBedEvent(EntityPlayer player, int x, int y, int z) {
        super(player);
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
