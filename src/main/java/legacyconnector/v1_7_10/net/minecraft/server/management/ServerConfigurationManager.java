package legacyconnector.v1_7_10.net.minecraft.server.management;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;

public class ServerConfigurationManager {
    private final MinecraftServer mcc$server;
    /** Phantom: redirected to {@link #mcc$get$playerEntityList}. */
    @Deprecated
    public List playerEntityList;

    public ServerConfigurationManager(MinecraftServer server) {
        this.mcc$server = server;
    }

    public static List mcc$get$playerEntityList(ServerConfigurationManager m) {
        List<EntityPlayerMP> out = new ArrayList<>();
        for (var p : m.mcc$server.mcc$modern().getPlayerList().getPlayers()) out.add((EntityPlayerMP) EntityBridge.wrap(p));
        return out;
    }

    public EntityPlayerMP getPlayerByUsername(String name) {
        var p = mcc$server.mcc$modern().getPlayerList().getPlayerByName(name);
        return p == null ? null : (EntityPlayerMP) EntityBridge.wrap(p);
    }

    public void sendChatMsg(IChatComponent message) {
        mcc$server.mcc$modern().getPlayerList().broadcastSystemMessage(message.mcc$toModern(), false);
    }

    public void sendChatMsgImpl(IChatComponent message, boolean system) {
        sendChatMsg(message);
    }

    /**
     * Moves a player to another dimension and lets the teleporter place them there. Coordinates are kept (1.7.10 scaled
     * them by the dimensions' move factors for the nether; mod dimensions do not).
     */
    public void transferPlayerToDimension(EntityPlayerMP player, int dimension, legacyconnector.v1_7_10.net.minecraft.world.Teleporter teleporter) {
        var target = org.kylepitcock.legacyconnector.v1_7_10.bridge.DimensionBridge.level(mcc$server.mcc$modern(), dimension);
        if (target == null) return;
        var sp = player.mcc$serverPlayer();
        double x = sp.getX(), y = sp.getY(), z = sp.getZ();
        float yaw = sp.getYRot();
        sp.teleportTo(target, x, y, z, yaw, sp.getXRot());
        if (teleporter != null) {
            try {
                teleporter.placeInPortal(player, x, y, z, yaw);
            } catch (RuntimeException | LinkageError e) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Dimensions").error("The teleporter {} failed", teleporter.getClass().getName(), e);
            }
        }
    }

    public void transferEntityToWorld(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity, int fromDimension,
            legacyconnector.v1_7_10.net.minecraft.world.WorldServer from, legacyconnector.v1_7_10.net.minecraft.world.WorldServer to) {
        entity.travelToDimension(legacyconnector.v1_7_10.net.minecraft.world.World.mcc$get$provider(to).dimensionId);
    }

    public int getCurrentPlayerCount() {
        return mcc$server.getCurrentPlayerCount();
    }

    public String[] getAllUsernames() {
        return mcc$server.getAllUsernames();
    }

    public boolean canSendCommands(com.mojang.authlib.GameProfile profile) {
        return mcc$server.mcc$modern().getPlayerList().isOp(profile);
    }
}
