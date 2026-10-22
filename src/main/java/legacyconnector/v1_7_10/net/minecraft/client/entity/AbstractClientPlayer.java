package legacyconnector.v1_7_10.net.minecraft.client.entity;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.entity.player.Player;

public abstract class AbstractClientPlayer extends EntityPlayer {
    protected AbstractClientPlayer(Player modern) {
        super(modern);
    }
}
