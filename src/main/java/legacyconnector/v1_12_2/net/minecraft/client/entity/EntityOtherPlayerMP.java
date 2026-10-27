package legacyconnector.v1_12_2.net.minecraft.client.entity;

/** Another player as the client sees it. */
public class EntityOtherPlayerMP extends AbstractClientPlayer {
    public EntityOtherPlayerMP(net.minecraft.world.entity.player.Player modern) {
        super(modern);
    }

    /** A player a mod makes on the client (a fake player drawn in a screen or the world). */
    public EntityOtherPlayerMP(legacyconnector.v1_12_2.net.minecraft.world.World world, com.mojang.authlib.GameProfile profile) {
        super(world, profile);
    }
}
