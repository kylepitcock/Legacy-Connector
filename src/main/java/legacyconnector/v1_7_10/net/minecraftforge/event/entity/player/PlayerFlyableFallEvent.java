package legacyconnector.v1_7_10.net.minecraftforge.event.entity.player;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;

/** A player that may fly lands from a height (no fall damage): {@link #distance} is the height fallen. */
public class PlayerFlyableFallEvent extends PlayerEvent {
    public float distance;

    public PlayerFlyableFallEvent(EntityPlayer player, float distance) {
        super(player);
        this.distance = distance;
    }
}
