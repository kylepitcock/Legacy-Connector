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
    /** Beta's {@code movementInput}: the movement controls of this player (the keys of the game options unless a mod set its own). */
    public legacyconnector.vb1_7_3.net.minecraft.client.input.Input input=new legacyconnector.vb1_7_3.net.minecraft.client.input.KeyboardInput(
            legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance().options);
    /** Beta's {@code mc}: the game this player belongs to. */
    public legacyconnector.vb1_7_3.net.minecraft.client.Minecraft minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance();
    public ClientPlayerEntity(World world){super(world);}
    /** {@code EntityPlayerSP(Minecraft, World, Session, int dimension)}: what a mod's own player subclass calls. */
    public ClientPlayerEntity(legacyconnector.vb1_7_3.net.minecraft.client.Minecraft minecraft,World world,legacyconnector.vb1_7_3.net.minecraft.client.util.Session session,int dimension){
        super(world);dimensionId=dimension;
    }
    /** Beta's {@code rayTrace(reach, partialTick)}: the block the player looks at within {@code reach} blocks (null for none). */
    public legacyconnector.vb1_7_3.net.minecraft.util.hit.HitResult raycast(double reach,float partial){
        var from=legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d.create(prevX+(x-prevX)*partial,prevY+(y-prevY)*partial,prevZ+(z-prevZ)*partial);
        float yawRad=(prevYaw+(yaw-prevYaw)*partial)*0.017453292F,pitchRad=(prevPitch+(pitch-prevPitch)*partial)*0.017453292F;
        float cy=(float)Math.cos(-yawRad-Math.PI),sy=(float)Math.sin(-yawRad-Math.PI),cp=-(float)Math.cos(-pitchRad),sp=(float)Math.sin(-pitchRad);
        var to=from.add(sy*cp*reach,sp*reach,cy*cp*reach);
        return world==null?null:world.raycast(from,to);
    }
    /** The server-side client player has no chat box: the line goes out as the player's own chat. */
    public void sendChatMessage(String message){
        if(mcc$modern instanceof net.minecraft.server.level.ServerPlayer player&&message!=null)player.getServer().getPlayerList().broadcastSystemMessage(
                net.minecraft.network.chat.Component.literal("<"+player.getGameProfile().getName()+"> "+message),false);
    }
    /** Beta's {@code dropCurrentItem}: throws the selected stack. */
    public void dropSelectedItem(){dropItem(inventory.removeStack(inventory.selectedSlot,1),false);}
}
