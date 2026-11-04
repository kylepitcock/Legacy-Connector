package legacyconnector.v1_7_10.net.minecraftforge.event.entity.player;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;

/** A player gets out of bed. */
public class PlayerWakeUpEvent extends PlayerEvent {
    public final boolean wakeImmediatly;
    public final boolean updateWorld;
    public final boolean setSpawn;

    public PlayerWakeUpEvent(EntityPlayer player, boolean wakeImmediatly, boolean updateWorld, boolean setSpawn) {
        super(player);
        this.wakeImmediatly = wakeImmediatly;
        this.updateWorld = updateWorld;
        this.setSpawn = setSpawn;
    }
}
