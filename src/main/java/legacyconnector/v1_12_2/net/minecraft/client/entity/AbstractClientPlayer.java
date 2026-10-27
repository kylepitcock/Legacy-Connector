package legacyconnector.v1_12_2.net.minecraft.client.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** A player as the client sees it: the local player ({@link EntityPlayerSP}) or another one ({@link EntityOtherPlayerMP}). */
public abstract class AbstractClientPlayer extends EntityPlayer {
    public AbstractClientPlayer(World world) {
        super(world);
    }
}
