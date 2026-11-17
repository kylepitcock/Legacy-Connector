package legacyconnector.v1_12_2.net.minecraftforge.event.entity.player;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;

/** A player who can fly (creative, elytra, flight mods) lands from a height; no damage is dealt (fed from the modern event). */
public class PlayerFlyableFallEvent extends PlayerEvent {
    private float distance;
    private float multiplier;

    public PlayerFlyableFallEvent(EntityPlayer player, float distance, float multiplier) {
        super(player);
        this.distance = distance;
        this.multiplier = multiplier;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public float getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(float multiplier) {
        this.multiplier = multiplier;
    }
}
