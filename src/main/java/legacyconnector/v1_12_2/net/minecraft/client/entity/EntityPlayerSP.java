package legacyconnector.v1_12_2.net.minecraft.client.entity;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** The local player ({@code Minecraft.player}): a view over the modern local player. */
public class EntityPlayerSP extends AbstractClientPlayer {
    public EntityPlayerSP(World world) {
        super(world);
    }

    /** Closes the open screen and container as the server asked or the player did. */
    public void closeScreen() {
        if (mcc$entity instanceof net.minecraft.world.entity.player.Player p) p.closeContainer();
    }
}
