package legacyconnector.v1_7_10.net.minecraft.client.entity;

import legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.world.entity.player.Player;

/** The local player as 1.7.10 code sees it ({@code Minecraft.getMinecraft().thePlayer}). */
public class EntityClientPlayerMP extends EntityPlayerSP {
    /** Legacy mods use this handle to send packets through the active client connection. */
    public final NetHandlerPlayClient sendQueue;

    /** The client's statistics when no integrated server holds the real ones (multiplayer). */
    private final legacyconnector.v1_7_10.net.minecraft.stats.StatFileWriter mcc$clientStats = new legacyconnector.v1_7_10.net.minecraft.stats.StatFileWriter();

    public EntityClientPlayerMP(Player modern) {
        super(modern);
        this.sendQueue = NetHandlerPlayClient.INSTANCE;
    }

    /** 1.7.10's constructor (Galacticraft's player subclass calls it): a view player, as {@link EntityPlayerSP}'s. */
    public EntityClientPlayerMP(legacyconnector.v1_7_10.net.minecraft.client.Minecraft mc, legacyconnector.v1_7_10.net.minecraft.world.World world,
            legacyconnector.v1_7_10.net.minecraft.util.Session session, NetHandlerPlayClient handler,
            legacyconnector.v1_7_10.net.minecraft.stats.StatFileWriter stats) {
        super(mc, world, session, 0);
        this.sendQueue = handler != null ? handler : NetHandlerPlayClient.INSTANCE;
    }

    public void sendChatMessage(String message) {
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        if (connection == null) return;
        if (message.startsWith("/")) connection.sendCommand(message.substring(1));
        else connection.sendChat(message);
    }

    private legacyconnector.v1_7_10.net.minecraft.stats.StatFileWriter mcc$stats;

    /** The player's statistics; achievements are read from the same player on the integrated server. */
    public legacyconnector.v1_7_10.net.minecraft.stats.StatFileWriter getStatFileWriter() {
        if (mcc$stats == null) mcc$stats = new legacyconnector.v1_7_10.net.minecraft.stats.StatFileWriter(mcc$player().getUUID());
        return mcc$stats;
    }

    public float getSwingProgress(float partial) {
        return mcc$player().getAttackAnim(partial);
    }

    /** Asks the server to respawn the (dead) player. */
    public void respawnPlayer() {
        if (mcc$player() instanceof net.minecraft.client.player.LocalPlayer local) local.respawn();
    }
}
