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

    /** The names of the operators (the ops list). */
    public String[] getOppedPlayerNames() {
        return mcc$server.mcc$modern().getPlayerList().getOpNames();
    }

    /** Whether the name is an operator. */
    public boolean func_152596_g(com.mojang.authlib.GameProfile profile) {
        return mcc$server.mcc$modern().getPlayerList().isOp(profile);
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

    public void syncPlayerInventory(EntityPlayerMP player) {
        player.mcc$serverPlayer().inventoryMenu.sendAllDataToRemote();
    }

    public void updateTimeAndWeatherForPlayer(EntityPlayerMP player, legacyconnector.v1_7_10.net.minecraft.world.WorldServer world) {
    }

    public void func_72375_a(EntityPlayerMP player, legacyconnector.v1_7_10.net.minecraft.world.WorldServer world) {
    }

    public MinecraftServer getServerInstance() {
        return mcc$server;
    }

    public void sendPacketToAllPlayers(legacyconnector.v1_7_10.net.minecraft.network.Packet packet) {
        if (packet instanceof legacyconnector.v1_7_10.cpw.mods.fml.common.network.internal.FMLProxyPacket fml) {
            org.kylepitcock.legacyconnector.v1_7_10.bridge.FmlChannelBridge.send(fml,
                    legacyconnector.v1_7_10.cpw.mods.fml.common.network.FMLOutboundHandler.OutboundTarget.ALL, null);
        } else if (packet != null) {
            for (var p : mcc$server.mcc$modern().getPlayerList().getPlayers()) mcc$send(p, packet);
        }
    }

    public void sendPacketToAllPlayersInDimension(legacyconnector.v1_7_10.net.minecraft.network.Packet packet, int dimension) {
        if (packet instanceof legacyconnector.v1_7_10.cpw.mods.fml.common.network.internal.FMLProxyPacket fml) {
            org.kylepitcock.legacyconnector.v1_7_10.bridge.FmlChannelBridge.send(fml,
                    legacyconnector.v1_7_10.cpw.mods.fml.common.network.FMLOutboundHandler.OutboundTarget.DIMENSION, dimension);
        } else if (packet != null) {
            var level = org.kylepitcock.legacyconnector.v1_7_10.bridge.DimensionBridge.loaded(mcc$server.mcc$modern(), dimension);
            for (var p : mcc$server.mcc$modern().getPlayerList().getPlayers()) if (p.serverLevel() == level) mcc$send(p, packet);
        }
    }

    /** Sends a legacy packet to one player the way their connection would (tile entity data, FML channels, vanilla packets). */
    private static void mcc$send(net.minecraft.server.level.ServerPlayer player, legacyconnector.v1_7_10.net.minecraft.network.Packet packet) {
        if (player.connection == null) return;
        new legacyconnector.v1_7_10.net.minecraft.network.NetHandlerPlayServer((EntityPlayerMP) EntityBridge.wrap(player)).sendPacket(packet);
    }

    /** Sends the packet to every player in the dimension within {@code radius} blocks of the position. */
    public void sendToAllNear(double x, double y, double z, double radius, int dimension, legacyconnector.v1_7_10.net.minecraft.network.Packet packet) {
        sendToAllNearExcept(null, x, y, z, radius, dimension, packet);
    }

    /** As {@link #sendToAllNear}, skipping {@code except} (the player who caused the effect and shows it already). */
    public void sendToAllNearExcept(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer except, double x, double y, double z,
            double radius, int dimension, legacyconnector.v1_7_10.net.minecraft.network.Packet packet) {
        var level = org.kylepitcock.legacyconnector.v1_7_10.bridge.DimensionBridge.loaded(mcc$server.mcc$modern(), dimension);
        if (level == null || packet == null) return;
        var skip = except == null ? null : except.mcc$player();
        for (var p : mcc$server.mcc$modern().getPlayerList().getPlayers()) {
            if (p == skip || p.serverLevel() != level) continue;
            double dx = x - p.getX(), dy = y - p.getY(), dz = z - p.getZ();
            if (dx * dx + dy * dy + dz * dz < radius * radius) mcc$send(p, packet);
        }
    }

    /**
     * Moves an entity to another world and lets the teleporter place it (Forge's variant: mods pass their own
     * teleporter). Coordinates are kept, as in {@link #transferPlayerToDimension}; an entity other than a player is
     * re-created in the target world, so the teleporter places the new one.
     */
    public void transferEntityToWorld(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity, int fromDimension,
            legacyconnector.v1_7_10.net.minecraft.world.WorldServer from, legacyconnector.v1_7_10.net.minecraft.world.WorldServer to,
            legacyconnector.v1_7_10.net.minecraft.world.Teleporter teleporter) {
        if (entity == null || to == null || !(to.mcc$fullLevel() instanceof net.minecraft.server.level.ServerLevel target)) return;
        var m = entity.mcc$entity;
        if (m == null) return;
        double x = m.getX(), y = m.getY(), z = m.getZ();
        float yaw = m.getYRot();
        legacyconnector.v1_7_10.net.minecraft.entity.Entity moved = entity;
        if (m instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.teleportTo(target, x, y, z, yaw, sp.getXRot());
        } else if (m.level() != target) {
            var copy = m.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(target, m.position(), m.getDeltaMovement(), yaw,
                    m.getXRot(), net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING));
            if (copy == null) return;
            moved = EntityBridge.wrap(copy);
        }
        if (teleporter == null || moved == null) return;
        try {
            teleporter.placeInPortal(moved, x, y, z, yaw);
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Dimensions").error("The teleporter {} failed", teleporter.getClass().getName(), e);
        }
    }

    // ------------------------------------------------------------------ user lists

    private UserListOps mcc$ops;
    private UserListBans mcc$bans;
    private UserListWhitelist mcc$whitelist;

    /** The operators (a view of the server's ops list). */
    public UserListOps getOppedPlayers() {
        if (mcc$ops == null) mcc$ops = new UserListOps(mcc$server.mcc$modern().getPlayerList().getOps());
        return mcc$ops;
    }

    /** The banned players (a view of the server's ban list). */
    public UserListBans getBannedPlayers() {
        if (mcc$bans == null) mcc$bans = new UserListBans(mcc$server.mcc$modern().getPlayerList().getBans());
        return mcc$bans;
    }

    /** The whitelisted players (a view of the server's whitelist). */
    public UserListWhitelist getWhitelistedPlayers() {
        if (mcc$whitelist == null) mcc$whitelist = new UserListWhitelist(mcc$server.mcc$modern().getPlayerList().getWhiteList());
        return mcc$whitelist;
    }

    public String[] getWhitelistedPlayerNames() {
        return mcc$server.mcc$modern().getPlayerList().getWhiteListNames();
    }

    public void addWhitelistedPlayer(com.mojang.authlib.GameProfile profile) {
        mcc$server.mcc$modern().getPlayerList().getWhiteList().add(new net.minecraft.server.players.UserWhiteListEntry(profile));
    }

    public void removePlayerFromWhitelist(com.mojang.authlib.GameProfile profile) {
        mcc$server.mcc$modern().getPlayerList().getWhiteList().remove(profile);
    }

    /** Makes the player an operator at the server's op permission level (an online player gets their new commands). */
    public void addOp(com.mojang.authlib.GameProfile profile) {
        mcc$server.mcc$modern().getPlayerList().op(profile);
    }

    public void removeOp(com.mojang.authlib.GameProfile profile) {
        mcc$server.mcc$modern().getPlayerList().deop(profile);
    }

    public boolean canJoin(com.mojang.authlib.GameProfile profile) {
        return mcc$server.mcc$modern().getPlayerList().isWhiteListed(profile);
    }

    // ------------------------------------------------------------------ view distance

    /** The view distance in chunks. */
    public int getViewDistance() {
        return mcc$server.mcc$modern().getPlayerList().getViewDistance();
    }

    /** How far (in blocks) entities are tracked: the view distance less one chunk. */
    public int getEntityViewDistance() {
        return getViewDistance() * 16 - 16;
    }

    public void setViewDistance(int distance) {
        mcc$server.mcc$modern().getPlayerList().setViewDistance(distance);
    }

    /**
     * A player entity for a profile, not added to any world (1.7.10 made the one a logging-in player gets; here it serves
     * mods that want an entity for a profile). An online player is returned as is instead of being disconnected for
     * logging in twice, as no second login is taking place.
     */
    public EntityPlayerMP createPlayerForUser(com.mojang.authlib.GameProfile profile) {
        var server = mcc$server.mcc$modern();
        var online = profile.getId() == null ? null : server.getPlayerList().getPlayer(profile.getId());
        if (online != null) return (EntityPlayerMP) EntityBridge.wrap(online);
        var player = new net.minecraft.server.level.ServerPlayer(server, server.overworld(), profile,
                net.minecraft.server.level.ClientInformation.createDefault());
        return (EntityPlayerMP) EntityBridge.wrap(player);
    }

    // ------------------------------------------------------------------ player selectors

    /**
     * The players a selector matches (1.7.10 rules): in {@code world} unless null, named {@code name} ("!" negates),
     * on {@code team} ("!" negates), between the ranges from {@code coords}, in {@code gameMode} (-1: any), within the
     * levels and scores; sorted nearest first, reversed for a negative {@code count}, at most |count| (0: all).
     */
    public List findPlayers(legacyconnector.v1_7_10.net.minecraft.util.ChunkCoordinates coords, int minRange, int maxRange, int count, int gameMode,
            int minLevel, int maxLevel, java.util.Map scores, String name, String team, legacyconnector.v1_7_10.net.minecraft.world.World world) {
        var players = mcc$server.mcc$modern().getPlayerList().getPlayers();
        if (players.isEmpty()) return java.util.Collections.emptyList();
        List<EntityPlayerMP> result = new ArrayList<>();
        boolean reverse = count < 0;
        boolean nameNot = name != null && name.startsWith("!");
        boolean teamNot = team != null && team.startsWith("!");
        int minSq = minRange * minRange;
        int maxSq = maxRange * maxRange;
        count = Math.abs(count);
        if (nameNot) name = name.substring(1);
        if (teamNot) team = team.substring(1);
        var level = world == null ? null : world.mcc$fullLevel();
        for (var p : players) {
            if (level != null && p.level() != level) continue;
            if (name != null && nameNot == name.equalsIgnoreCase(p.getGameProfile().getName())) continue;
            if (team != null) {
                var t = p.getTeam();
                String teamName = t == null ? "" : t.getName();
                if (teamNot == team.equals(teamName)) continue;
            }
            if (coords != null && (minRange > 0 || maxRange > 0)) {
                float d = coords.getDistanceSquared(net.minecraft.util.Mth.floor(p.getX()), net.minecraft.util.Mth.floor(p.getY() + 0.5D),
                        net.minecraft.util.Mth.floor(p.getZ()));
                if (minRange > 0 && d < minSq || maxRange > 0 && d > maxSq) continue;
            }
            if (!mcc$matchesScores(p, scores)) continue;
            if (gameMode != -1 && gameMode != p.gameMode.getGameModeForPlayer().getId()) continue;
            if (minLevel > 0 && p.experienceLevel < minLevel || p.experienceLevel > maxLevel) continue;
            result.add((EntityPlayerMP) EntityBridge.wrap(p));
        }
        if (coords != null) {
            result.sort(java.util.Comparator.comparingDouble(p -> coords.getDistanceSquaredToChunkCoordinates(p.getPlayerCoordinates())));
        }
        if (reverse) java.util.Collections.reverse(result);
        if (count > 0) result = new ArrayList<>(result.subList(0, Math.min(count, result.size())));
        return result;
    }

    /** {@code score_<objective>} (at most) and {@code score_<objective>_min} (at least) of a selector. */
    private static boolean mcc$matchesScores(net.minecraft.server.level.ServerPlayer player, java.util.Map scores) {
        if (scores == null || scores.isEmpty()) return true;
        var scoreboard = player.getScoreboard();
        for (Object o : scores.entrySet()) {
            var e = (java.util.Map.Entry<?, ?>) o;
            String key = String.valueOf(e.getKey());
            boolean min = false;
            if (key.endsWith("_min") && key.length() > 4) {
                min = true;
                key = key.substring(0, key.length() - 4);
            }
            var objective = scoreboard.getObjective(key);
            if (objective == null) return false;
            var info = scoreboard.getPlayerScoreInfo(player, objective);
            int score = info == null ? 0 : info.value();
            int value = ((Number) e.getValue()).intValue();
            if (score < value && min) return false;
            if (score > value && !min) return false;
        }
        return true;
    }
}
