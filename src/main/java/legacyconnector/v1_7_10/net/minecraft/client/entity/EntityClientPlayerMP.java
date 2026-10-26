package legacyconnector.v1_7_10.net.minecraft.client.entity;

import net.minecraft.world.entity.player.Player;

/** The local player as 1.7.10 code sees it ({@code Minecraft.getMinecraft().thePlayer}). */
public class EntityClientPlayerMP extends EntityPlayerSP {
    /** The client's statistics when no integrated server holds the real ones (multiplayer). */
    private final legacyconnector.v1_7_10.net.minecraft.stats.StatFileWriter mcc$clientStats = new legacyconnector.v1_7_10.net.minecraft.stats.StatFileWriter();

    public EntityClientPlayerMP(Player modern) {
        super(modern);
    }

    public void sendChatMessage(String message) {
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        if (connection == null) return;
        if (message.startsWith("/")) connection.sendCommand(message.substring(1));
        else connection.sendChat(message);
    }

    public float getSwingProgress(float partial) {
        return mcc$player().getAttackAnim(partial);
    }

    /**
     * The player's statistics and achievements as the client knows them: in single player the integrated server's record
     * of this player (achievements unlocked by mods there show at once), otherwise the client's own table.
     */
    public legacyconnector.v1_7_10.net.minecraft.stats.StatFileWriter getStatFileWriter() {
        try {
            var server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
            if (server != null) {
                var serverPlayer = server.getPlayerList().getPlayer(mcc$player().getUUID());
                if (serverPlayer != null && org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge.wrap(serverPlayer)
                        instanceof legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP mp) {
                    return mp.getStatFile();
                }
            }
        } catch (RuntimeException | LinkageError e) {
            // no integrated server
        }
        return mcc$clientStats;
    }

    /** Asks the server to respawn the (dead) player. */
    public void respawnPlayer() {
        if (mcc$player() instanceof net.minecraft.client.player.LocalPlayer local) local.respawn();
    }
}
