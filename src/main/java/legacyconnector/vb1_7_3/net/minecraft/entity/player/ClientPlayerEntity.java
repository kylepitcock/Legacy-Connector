package legacyconnector.vb1_7_3.net.minecraft.entity.player;

import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta's client-player API identity; server callback views retain the same legacy client-era type. */
public abstract class ClientPlayerEntity extends PlayerEntity {
    public final org.kylepitcock.legacyconnector.vb1_7_3.PlayerApiB173.State mcc$playerApi = new org.kylepitcock.legacyconnector.vb1_7_3.PlayerApiB173.State();
    public ClientPlayerEntity(World world){super(world);}
}
