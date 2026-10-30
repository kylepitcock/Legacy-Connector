package legacyconnector.v1_12_2.net.minecraftforge.server.permission.context;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** A player's context: their world. */
public class PlayerContext extends Context {
    private final EntityPlayer player;

    public PlayerContext(EntityPlayer ep) {
        this.player = ep;
    }

    @Override
    public World getWorld() {
        return player.getEntityWorld();
    }

    @Override
    public EntityPlayer getPlayer() {
        return player;
    }
}
