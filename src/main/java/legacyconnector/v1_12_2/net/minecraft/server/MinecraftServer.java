package legacyconnector.v1_12_2.net.minecraft.server;

import com.google.common.util.concurrent.ListenableFuture;
import legacyconnector.v1_12_2.net.minecraft.command.CommandHandler;
import legacyconnector.v1_12_2.net.minecraft.command.ICommandManager;

/**
 * The running server as 1.12 mods see it: one object for the game's life, answering from the modern server that is running
 * (worlds are reached through {@code DimensionManager}).
 */
public class MinecraftServer implements legacyconnector.v1_12_2.net.minecraft.command.ICommandSender,
        legacyconnector.v1_12_2.net.minecraft.util.IThreadListener {
    private static final MinecraftServer INSTANCE = new MinecraftServer();
    private final CommandHandler commandManager = new CommandHandler();
    private final legacyconnector.v1_12_2.net.minecraft.server.management.PlayerList playerList =
            new legacyconnector.v1_12_2.net.minecraft.server.management.PlayerList(this);
    private final legacyconnector.v1_12_2.net.minecraft.server.management.PlayerProfileCache profileCache =
            new legacyconnector.v1_12_2.net.minecraft.server.management.PlayerProfileCache();
    /** The section profiler mods report to (it records nothing until enabled). */
    public final legacyconnector.v1_12_2.net.minecraft.profiler.Profiler profiler = new legacyconnector.v1_12_2.net.minecraft.profiler.Profiler();

    /** Phantom fields: accesses are redirected to the accessors below. */
    @Deprecated
    public long[] tickTimeArray;
    @Deprecated
    public java.io.File anvilFile;
    @Deprecated
    public java.util.Hashtable<Integer, long[]> worldTickTimes;

    public static MinecraftServer mcc$instance() {
        return INSTANCE;
    }

    private static net.minecraft.server.MinecraftServer modern() {
        return net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
    }

    /** The last 100 tick lengths in nanoseconds. */
    public static long[] mcc$get$tickTimeArray(MinecraftServer s) {
        var m = modern();
        return m == null ? new long[100] : m.getTickTimesNanos();
    }

    public static java.io.File mcc$get$anvilFile(MinecraftServer s) {
        var m = modern();
        return m == null ? new java.io.File("saves") : m.getServerDirectory().resolve("saves").toFile();
    }

    /** The tick lengths of each dimension (by legacy id) in nanoseconds. */
    public static java.util.Hashtable<Integer, long[]> mcc$get$worldTickTimes(MinecraftServer s) {
        java.util.Hashtable<Integer, long[]> out = new java.util.Hashtable<>();
        var m = modern();
        if (m == null) return out;
        for (var level : m.getAllLevels()) {
            long[] times = m.getTickTime(level.dimension());
            out.put(org.kylepitcock.legacyconnector.v1_12_2.bridge.EventBridge1122.dimensionId(level.dimension()), times == null ? new long[100] : times);
        }
        return out;
    }

    public ICommandManager getCommandManager() {
        return commandManager;
    }

    public legacyconnector.v1_12_2.net.minecraft.server.management.PlayerList getPlayerList() {
        return playerList;
    }

    public legacyconnector.v1_12_2.net.minecraft.server.management.PlayerProfileCache getPlayerProfileCache() {
        return profileCache;
    }

    // the main thread

    @Override
    public ListenableFuture<Object> addScheduledTask(Runnable runnableToSchedule) {
        var m = modern();
        if (m == null) {
            try {
                runnableToSchedule.run();
                return com.google.common.util.concurrent.Futures.immediateFuture(null);
            } catch (Throwable t) {
                return com.google.common.util.concurrent.Futures.immediateFailedFuture(t);
            }
        }
        return legacyconnector.v1_12_2.net.minecraft.util.IThreadListener.mcc$of(m).addScheduledTask(runnableToSchedule);
    }

    public <V> ListenableFuture<V> callFromMainThread(java.util.concurrent.Callable<V> callable) {
        com.google.common.util.concurrent.SettableFuture<V> f = com.google.common.util.concurrent.SettableFuture.create();
        Runnable task = () -> {
            try {
                f.set(callable.call());
            } catch (Throwable t) {
                f.setException(t);
            }
        };
        var m = modern();
        if (m == null || m.isSameThread()) task.run();
        else m.execute(task);
        return f;
    }

    @Override
    public boolean isCallingFromMinecraftThread() {
        var m = modern();
        return m == null || m.isSameThread();
    }

    // the server as a command sender (the console)

    @Override
    public String getName() {
        return "Server";
    }

    @Override
    public void sendMessage(legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent component) {
        if (component != null) org.slf4j.LoggerFactory.getLogger("LegacyConnector/1.12.2").info(component.getUnformattedText());
    }

    @Override
    public boolean canUseCommand(int permLevel, String commandName) {
        return true;
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.world.World getEntityWorld() {
        var server = modern();
        return server == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.wrap(server.overworld());
    }

    @Override
    public MinecraftServer getServer() {
        return this;
    }

    // state

    public boolean isDedicatedServer() {
        return !net.neoforged.fml.loading.FMLEnvironment.dist.isClient();
    }

    public boolean isSinglePlayer() {
        return net.neoforged.fml.loading.FMLEnvironment.dist.isClient();
    }

    public String getMinecraftVersion() {
        return "1.12.2";
    }

    public boolean isServerRunning() {
        var m = modern();
        return m != null && m.isRunning();
    }

    public int getTickCounter() {
        var m = modern();
        return m == null ? 0 : m.getTickCount();
    }

    public static long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    public boolean isHardcore() {
        var m = modern();
        return m != null && m.isHardcore();
    }

    public boolean isPVPEnabled() {
        var m = modern();
        return m == null || m.isPvpAllowed();
    }

    public void setAllowPvp(boolean allowPvp) {
        var m = modern();
        if (m != null) m.setPvpAllowed(allowPvp);
    }

    public boolean isServerInOnlineMode() {
        var m = modern();
        return m != null && m.usesAuthentication();
    }

    public boolean isSnooperEnabled() {
        return false;
    }

    public int getOpPermissionLevel() {
        var m = modern();
        return m == null ? 4 : m.getOperatorUserPermissionLevel();
    }

    /** The single player world's owner (null on a dedicated server). */
    public String getServerOwner() {
        var m = modern();
        if (m == null || !m.isSingleplayer() || m.getSingleplayerProfile() == null) return null;
        return m.getSingleplayerProfile().getName();
    }

    public String[] getOnlinePlayerNames() {
        return playerList.getOnlinePlayerNames();
    }

    public String getFolderName() {
        var m = modern();
        return m == null ? "world" : m.getWorldData().getLevelName();
    }

    public com.mojang.authlib.minecraft.MinecraftSessionService getMinecraftSessionService() {
        var m = modern();
        return m == null ? null : m.getSessionService();
    }

    /** Whether spawn protection stops the player from changing the block. */
    public boolean isBlockProtected(legacyconnector.v1_12_2.net.minecraft.world.World worldIn, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer playerIn) {
        var m = modern();
        if (m == null || worldIn == null || !(worldIn.mcc$level() instanceof net.minecraft.server.level.ServerLevel level) || playerIn == null
                || playerIn.mcc$player() == null) {
            return false;
        }
        return m.isUnderSpawnProtection(level, new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ()), playerIn.mcc$player());
    }

    /** An entity in any loaded world by its UUID. */
    public legacyconnector.v1_12_2.net.minecraft.entity.Entity getEntityFromUuid(java.util.UUID uuid) {
        var m = modern();
        if (m == null || uuid == null) return null;
        for (var level : m.getAllLevels()) {
            var e = level.getEntity(uuid);
            if (e != null) return org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(e);
        }
        return null;
    }

    /** A file in the server's directory. */
    public java.io.File getFile(String fileName) {
        return new java.io.File(getDataDirectory(), fileName);
    }

    /** The game's directory (the server directory; the client's game directory in single player). */
    public java.io.File getDataDirectory() {
        var server = modern();
        return server == null ? net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().toFile() : server.getServerDirectory().toFile();
    }

    private static net.minecraft.server.MinecraftServer mcc$modern() {
        return net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
    }

    public legacyconnector.v1_12_2.net.minecraft.world.WorldServer getWorld(int dimension) {
        var level = org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.levelOf(mcc$modern(), dimension);
        if (level == null) level = mcc$modern() == null ? null : mcc$modern().overworld();
        return (legacyconnector.v1_12_2.net.minecraft.world.WorldServer) org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.wrap(level);
    }

    public int getCurrentPlayerCount() {
        var server = mcc$modern();
        return server == null ? 0 : server.getPlayerCount();
    }

    public int getMaxPlayers() {
        var server = mcc$modern();
        return server == null ? 0 : server.getMaxPlayers();
    }

    public String getMOTD() {
        var server = mcc$modern();
        return server == null ? "" : server.getMotd();
    }

    public boolean getAllowNether() {
        var server = mcc$modern();
        return server == null || server.getLevel(net.minecraft.world.level.Level.NETHER) != null;
    }

    public boolean isFlightAllowed() {
        var server = mcc$modern();
        return server != null && server.isFlightAllowed();
    }
}
