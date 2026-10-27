package legacyconnector.v1_12_2.net.minecraft.server.management;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;

/** 1.12.2 player list: a view over the modern server's players. */
public class PlayerList {
    private final net.minecraft.server.MinecraftServer server;

    public PlayerList(net.minecraft.server.MinecraftServer server) {
        this.server = server;
    }

    private static EntityPlayerMP mcc$view(net.minecraft.server.level.ServerPlayer p) {
        return p == null ? null : (EntityPlayerMP) org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(p);
    }

    public java.util.List<EntityPlayerMP> getPlayers() {
        java.util.List<EntityPlayerMP> out = new java.util.ArrayList<>();
        for (var p : server.getPlayerList().getPlayers()) out.add(mcc$view(p));
        return out;
    }

    public EntityPlayerMP getPlayerByUUID(java.util.UUID uuid) {
        return mcc$view(server.getPlayerList().getPlayer(uuid));
    }

    public EntityPlayerMP getPlayerByUsername(String name) {
        return mcc$view(server.getPlayerList().getPlayerByName(name));
    }

    public int getCurrentPlayerCount() {
        return server.getPlayerCount();
    }

    public int getMaxPlayers() {
        return server.getMaxPlayers();
    }

    public String[] getOnlinePlayerNames() {
        return server.getPlayerNames();
    }

    public com.mojang.authlib.GameProfile[] getOnlinePlayerProfiles() {
        return server.getPlayerList().getPlayers().stream().map(net.minecraft.world.entity.player.Player::getGameProfile).toArray(com.mojang.authlib.GameProfile[]::new);
    }

    public boolean canSendCommands(com.mojang.authlib.GameProfile profile) {
        return server.getPlayerList().isOp(profile);
    }

    public void sendMessage(legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent component) {
        sendMessage(component, true);
    }

    public void sendMessage(legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent component, boolean system) {
        if (component != null) server.getPlayerList().broadcastSystemMessage(component.mcc$modern(), false);
    }

    public void saveAllPlayerData() {
        server.getPlayerList().saveAll();
    }

    public int getViewDistance() {
        return server.getPlayerList().getViewDistance();
    }
}
