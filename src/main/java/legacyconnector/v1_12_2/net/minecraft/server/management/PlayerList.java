package legacyconnector.v1_12_2.net.minecraft.server.management;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;

/** The server's online players: a view over the modern player list. */
public class PlayerList {
    private final MinecraftServer server;

    public PlayerList(MinecraftServer server) {
        this.server = server;
    }

    private static net.minecraft.server.players.PlayerList modern() {
        var s = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return s == null ? null : s.getPlayerList();
    }

    private static EntityPlayerMP view(net.minecraft.server.level.ServerPlayer p) {
        return p != null && EntityBridge1122.wrap(p) instanceof EntityPlayerMP mp ? mp : null;
    }

    private static net.minecraft.network.protocol.Packet<?> packet(Packet<?> packetIn) {
        return packetIn == null ? null : packetIn.mcc$modern();
    }

    public MinecraftServer getServerInstance() {
        return server;
    }

    public List<EntityPlayerMP> getPlayers() {
        List<EntityPlayerMP> out = new ArrayList<>();
        var m = modern();
        if (m == null) return out;
        for (var p : m.getPlayers()) {
            EntityPlayerMP v = view(p);
            if (v != null) out.add(v);
        }
        return out;
    }

    public EntityPlayerMP getPlayerByUUID(UUID playerUUID) {
        var m = modern();
        return m == null ? null : view(m.getPlayer(playerUUID));
    }

    public EntityPlayerMP getPlayerByUsername(String username) {
        var m = modern();
        return m == null ? null : view(m.getPlayerByName(username));
    }

    public int getCurrentPlayerCount() {
        var m = modern();
        return m == null ? 0 : m.getPlayerCount();
    }

    public int getMaxPlayers() {
        var m = modern();
        return m == null ? 0 : m.getMaxPlayers();
    }

    public String[] getOnlinePlayerNames() {
        var m = modern();
        return m == null ? new String[0] : m.getPlayerNamesArray();
    }

    public GameProfile[] getOnlinePlayerProfiles() {
        var m = modern();
        if (m == null) return new GameProfile[0];
        return m.getPlayers().stream().map(net.minecraft.world.entity.player.Player::getGameProfile).toArray(GameProfile[]::new);
    }

    public int getViewDistance() {
        var m = modern();
        return m == null ? 10 : m.getViewDistance();
    }

    public boolean canSendCommands(GameProfile profile) {
        var m = modern();
        return m != null && m.isOp(profile);
    }

    public UserListOps getOppedPlayers() {
        var m = modern();
        return new UserListOps(m == null ? null : m.getOps());
    }

    public String[] getOppedPlayerNames() {
        var m = modern();
        return m == null ? new String[0] : m.getOpNames();
    }

    public void updatePermissionLevel(EntityPlayerMP player) {
        var m = modern();
        if (m != null && player.mcc$serverPlayer() != null) m.sendPlayerPermissionLevel(player.mcc$serverPlayer());
    }

    public void syncPlayerInventory(EntityPlayerMP playerIn) {
        var p = playerIn.mcc$serverPlayer();
        if (p != null) p.inventoryMenu.sendAllDataToRemote();
    }

    public void saveAllPlayerData() {
        var m = modern();
        if (m != null) m.saveAll();
    }

    public void sendMessage(ITextComponent component) {
        sendMessage(component, true);
    }

    public void sendMessage(ITextComponent component, boolean isSystem) {
        var m = modern();
        if (m != null && component != null) m.broadcastSystemMessage(component.mcc$modern(), false);
    }

    /** To the player's team (or nobody when the player has none). */
    public void sendMessageToAllTeamMembers(EntityPlayer player, ITextComponent message) {
        var m = modern();
        var p = player == null ? null : player.mcc$player();
        if (m == null || p == null || p.getTeam() == null || message == null) return;
        for (var other : m.getPlayers()) {
            if (other != p && other.getTeam() == p.getTeam()) other.sendSystemMessage(message.mcc$modern());
        }
    }

    /** To everyone but the player's team (everyone when the player has none). */
    public void sendMessageToTeamOrAllPlayers(EntityPlayer player, ITextComponent message) {
        var m = modern();
        var p = player == null ? null : player.mcc$player();
        if (m == null || message == null) return;
        for (var other : m.getPlayers()) {
            if (p == null || p.getTeam() == null || other.getTeam() != p.getTeam()) other.sendSystemMessage(message.mcc$modern());
        }
    }

    public void sendPacketToAllPlayers(Packet<?> packetIn) {
        var m = modern();
        var packet = packet(packetIn);
        if (m != null && packet != null) m.broadcastAll(packet);
    }

    public void sendPacketToAllPlayersInDimension(Packet<?> packetIn, int dimension) {
        var m = modern();
        var packet = packet(packetIn);
        if (m == null || packet == null) return;
        for (var p : m.getPlayers()) {
            if (org.kylepitcock.legacyconnector.v1_12_2.bridge.EventBridge1122.dimensionId(p.level().dimension()) == dimension) p.connection.send(packet);
        }
    }

    public void sendToAllNearExcept(EntityPlayer except, double x, double y, double z, double radius, int dimension, Packet<?> packetIn) {
        var m = modern();
        var packet = packet(packetIn);
        if (m == null || packet == null) return;
        var skip = except == null ? null : except.mcc$player();
        for (var p : m.getPlayers()) {
            if (p == skip || org.kylepitcock.legacyconnector.v1_12_2.bridge.EventBridge1122.dimensionId(p.level().dimension()) != dimension) continue;
            if (p.distanceToSqr(x, y, z) < radius * radius) p.connection.send(packet);
        }
    }


    // ---- area F

    public legacyconnector.v1_12_2.net.minecraft.advancements.PlayerAdvancements getPlayerAdvancements(EntityPlayerMP player) {
        return new legacyconnector.v1_12_2.net.minecraft.advancements.PlayerAdvancements(player);
    }
}
