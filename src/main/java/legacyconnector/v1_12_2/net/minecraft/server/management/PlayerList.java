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

    /** Phantom field: reads go to {@link #mcc$get$playerDataManager}. */
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.world.storage.IPlayerFileData playerDataManager;

    public PlayerList(MinecraftServer server) {
        this.server = server;
    }

    /** The players' saved data ({@code playerdata/} of the world). */
    public static legacyconnector.v1_12_2.net.minecraft.world.storage.IPlayerFileData mcc$get$playerDataManager(PlayerList list) {
        return legacyconnector.v1_12_2.net.minecraft.world.storage.IPlayerFileData.mcc$server();
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

    public UserListBans getBannedPlayers() {
        var m = modern();
        return m == null ? new UserListBans((java.io.File) null) : new UserListBans(m.getBans());
    }

    public UserListIPBans getBannedIPs() {
        var m = modern();
        return m == null ? new UserListIPBans((java.io.File) null) : new UserListIPBans(m.getIpBans());
    }

    public UserListWhitelist getWhitelistedPlayers() {
        var m = modern();
        return m == null ? new UserListWhitelist((java.io.File) null) : new UserListWhitelist(m.getWhiteList());
    }

    /**
     * A player entity for a profile that is not (necessarily) online, in the overworld, as login made one (mods use it to read and
     * edit offline players' data). Its saved data is not loaded: {@code readPlayerDataFromFile} does that.
     */
    public EntityPlayerMP createPlayerForUser(GameProfile profile) {
        var s = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (s == null || profile == null) return null;
        var sp = new net.minecraft.server.level.ServerPlayer(s, s.overworld(), profile, net.minecraft.server.level.ClientInformation.createDefault());
        return view(sp);
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

    // ---- area F: dimension travel and per-player updates

    /** Moves the player to the dimension, then lets the teleporter place it (mods' teleporters build their own arrival spots). */
    public void transferPlayerToDimension(EntityPlayerMP player, int dimensionIn, legacyconnector.v1_12_2.net.minecraft.world.Teleporter teleporter) {
        transferPlayerToDimension(player, dimensionIn, (legacyconnector.v1_12_2.net.minecraftforge.common.util.ITeleporter) teleporter);
    }

    public void transferPlayerToDimension(EntityPlayerMP player, int dimensionIn, legacyconnector.v1_12_2.net.minecraftforge.common.util.ITeleporter teleporter) {
        var sp = player == null ? null : player.mcc$serverPlayer();
        if (sp == null) return;
        var target = org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.levelOf(sp.server, dimensionIn);
        if (target == null) return;
        if (target != sp.serverLevel()) sp.teleportTo(target, sp.getX(), sp.getY(), sp.getZ(), sp.getYRot(), sp.getXRot());
        if (teleporter != null) {
            teleporter.placeEntity(org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.wrap(target), player, sp.getYRot());
            // the teleporter moved the server-side player: tell its client where it is now
            sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ(), sp.getYRot(), sp.getXRot());
        }
    }

    /** Moves a (non-player) entity between the worlds, then lets the teleporter place it. */
    public void transferEntityToWorld(legacyconnector.v1_12_2.net.minecraft.entity.Entity entityIn, int lastDimension,
            legacyconnector.v1_12_2.net.minecraft.world.WorldServer oldWorldIn, legacyconnector.v1_12_2.net.minecraft.world.WorldServer toWorldIn,
            legacyconnector.v1_12_2.net.minecraft.world.Teleporter teleporter) {
        if (entityIn == null || toWorldIn == null || !(toWorldIn.mcc$level() instanceof net.minecraft.server.level.ServerLevel target)) return;
        var m = entityIn.mcc$modern();
        if (m == null) return;
        if (m.level() != target) {
            m.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(target, m.position(), m.getDeltaMovement(), m.getYRot(),
                    m.getXRot(), net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING));
        }
        if (teleporter != null) teleporter.placeEntity(toWorldIn, entityIn, m.getYRot());
    }

    /** The modern level adds an arriving player to its chunk tracking itself. */
    public void preparePlayer(EntityPlayerMP playerIn, legacyconnector.v1_12_2.net.minecraft.world.WorldServer worldIn) {
    }

    /** Sends the world's border, time, spawn and weather to the player. */
    public void updateTimeAndWeatherForPlayer(EntityPlayerMP playerIn, legacyconnector.v1_12_2.net.minecraft.world.WorldServer worldIn) {
        var sp = playerIn == null ? null : playerIn.mcc$serverPlayer();
        var m = modern();
        if (sp == null || m == null) return;
        var level = worldIn != null && worldIn.mcc$level() instanceof net.minecraft.server.level.ServerLevel l ? l : sp.serverLevel();
        m.sendLevelInfo(sp, level);
    }

    /** How far away entities are tracked, in blocks (1.12: the view distance in chunks, less one, times 16). */
    public int getEntityViewDistance() {
        var m = modern();
        return m == null ? 144 : m.getViewDistance() * 16 - 16;
    }

    /** The player's saved data, as written to its player file. */
    public legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound getPlayerNBT(EntityPlayerMP player) {
        var sp = player == null ? null : player.mcc$serverPlayer();
        if (sp == null) return null;
        return new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound(sp.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
    }

    /** The player's statistics (the modern counter, loaded from the world's stats folder). */
    public legacyconnector.v1_12_2.net.minecraft.stats.StatisticsManagerServer getPlayerStatsFile(EntityPlayer playerIn) {
        var m = modern();
        if (m == null || playerIn == null || playerIn.mcc$player() == null) return null;
        return legacyconnector.v1_12_2.net.minecraft.stats.StatisticsManagerServer.mcc$of(m.getPlayerStats(playerIn.mcc$player()));
    }

    /**
     * Respawns the player as a new entity (1.12: after death or when leaving the End, {@code conqueredEnd} keeps everything): the modern
     * respawn at the player's respawn point, then a move to {@code dimension} if that is another world.
     */
    public EntityPlayerMP recreatePlayerEntity(EntityPlayerMP playerIn, int dimension, boolean conqueredEnd) {
        var m = modern();
        var sp = playerIn == null ? null : playerIn.mcc$serverPlayer();
        if (m == null || sp == null) return playerIn;
        var fresh = m.respawn(sp, conqueredEnd, conqueredEnd ? net.minecraft.world.entity.Entity.RemovalReason.CHANGED_DIMENSION
                : net.minecraft.world.entity.Entity.RemovalReason.KILLED);
        var target = org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.levelOf(fresh.server, dimension);
        if (target != null && target != fresh.serverLevel()) {
            var spawn = target.getSharedSpawnPos();
            fresh.teleportTo(target, spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, fresh.getYRot(), fresh.getXRot());
        }
        EntityPlayerMP v = view(fresh);
        return v != null ? v : playerIn;
    }
}
