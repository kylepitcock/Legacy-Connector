package legacyconnector.vb1_7_3.net.minecraft.entity.player;

import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta's client-player API identity; server callback views retain the same legacy client-era type. */
public abstract class ClientPlayerEntity extends PlayerEntity {
    /** Installed by the client: presses or releases the native key bound to a LWJGL key code. */
    public static volatile java.util.function.BiConsumer<Integer,Boolean> mcc$keyUpdate;
    public void updateKey(int code,boolean pressed){var update=mcc$keyUpdate;if(update!=null)update.accept(code,pressed);}
    public final org.kylepitcock.legacyconnector.vb1_7_3.PlayerApiB173.State mcc$playerApi = new org.kylepitcock.legacyconnector.vb1_7_3.PlayerApiB173.State();
    public ClientPlayerEntity(World world){super(world);}
}
