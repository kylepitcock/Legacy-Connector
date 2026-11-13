package legacyconnector.vb1_7_3.net.minecraft.entity.player;

import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta's client-player API identity; server callback views retain the same legacy client-era type. */
public class ClientPlayerEntity extends PlayerEntity {
    /** Installed by the client: presses or releases the native key bound to a LWJGL key code. */
    public static volatile java.util.function.BiConsumer<Integer,Boolean> mcc$keyUpdate;
    public void updateKey(int code,boolean pressed){var update=mcc$keyUpdate;if(update!=null)update.accept(code,pressed);}
    public final org.kylepitcock.legacyconnector.vb1_7_3.PlayerApiB173.State mcc$playerApi = new org.kylepitcock.legacyconnector.vb1_7_3.PlayerApiB173.State();
    /** The dimension id the player goes to when it stands in a portal ({@code EntityPlayerSP.dimension} of ShockAhPI, set by a portal block's collision). */
    public int portal;
    public ClientPlayerEntity(World world){super(world);}
    /** {@code EntityPlayerSP(Minecraft, World, Session, int dimension)}: what a mod's own player subclass calls. */
    public ClientPlayerEntity(legacyconnector.vb1_7_3.net.minecraft.client.Minecraft minecraft,World world,legacyconnector.vb1_7_3.net.minecraft.client.util.Session session,int dimension){
        super(world);dimensionId=dimension;
    }
}
