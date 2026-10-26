package legacyconnector.v1_7_10.net.minecraft.server;

import java.util.Map;
import java.util.WeakHashMap;
import legacyconnector.v1_7_10.net.minecraft.command.ICommandSender;
import legacyconnector.v1_7_10.net.minecraft.server.management.ServerConfigurationManager;
import legacyconnector.v1_7_10.net.minecraft.util.ChatComponentText;
import legacyconnector.v1_7_10.net.minecraft.util.ChunkCoordinates;
import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.WorldServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.DimensionBridge;

/** View over the running modern server. */
public class MinecraftServer implements ICommandSender, Runnable {
    public static final java.io.File USER_CACHE_FILE = new java.io.File("usercache.json");
    public final legacyconnector.v1_7_10.net.minecraft.profiler.Profiler theProfiler = legacyconnector.v1_7_10.net.minecraft.profiler.Profiler.INSTANCE;
    private static final Map<net.minecraft.server.MinecraftServer, MinecraftServer> VIEWS = new WeakHashMap<>();
    private final net.minecraft.server.MinecraftServer mcc$server;
    private final ServerConfigurationManager configurationManager;

    protected MinecraftServer(net.minecraft.server.MinecraftServer server) {
        this.mcc$server = server;
        this.configurationManager = new ServerConfigurationManager(this);
    }

    public static synchronized MinecraftServer mcc$wrap(net.minecraft.server.MinecraftServer server) {
        return server == null ? null : VIEWS.computeIfAbsent(server,
                s -> s.isDedicatedServer() ? new MinecraftServer(s) : new legacyconnector.v1_7_10.net.minecraft.server.integrated.IntegratedServer(s));
    }

    public net.minecraft.server.MinecraftServer mcc$modern() {
        return mcc$server;
    }

    /** Stand-in while mods load: 1.7.10 mods see a server object in init and register commands on it. */
    private static final MinecraftServer PRESTART = new MinecraftServer(null);

    public static MinecraftServer getServer() {
        MinecraftServer running = mcc$wrap(ServerLifecycleHooks.getCurrentServer());
        return running != null ? running : legacyconnector.v1_7_10.cpw.mods.fml.common.Loader.mcc$isLoading() ? PRESTART : null;
    }

    private legacyconnector.v1_7_10.net.minecraft.command.ServerCommandManager commandManager;

    public synchronized legacyconnector.v1_7_10.net.minecraft.command.ICommandManager getCommandManager() {
        if (commandManager == null) commandManager = new legacyconnector.v1_7_10.net.minecraft.command.ServerCommandManager(mcc$server);
        return commandManager;
    }

    public ServerConfigurationManager getConfigurationManager() {
        return configurationManager;
    }

    /** Phantom: redirected to {@link #mcc$get$worldServers} (the overworld first, as 1.7.10's array was indexed by dimension order). */
    @Deprecated
    public WorldServer[] worldServers;

    public static WorldServer[] mcc$get$worldServers(MinecraftServer s) {
        if (s.mcc$server == null) return new WorldServer[0];
        java.util.List<WorldServer> out = new java.util.ArrayList<>();
        ServerLevel overworld = s.mcc$server.overworld();
        out.add((WorldServer) WorldServer.mcc$wrap(overworld));
        for (ServerLevel level : s.mcc$server.getAllLevels()) {
            if (level != overworld) out.add((WorldServer) WorldServer.mcc$wrap(level));
        }
        return out.toArray(new WorldServer[0]);
    }

    public WorldServer worldServerForDimension(int dimension) {
        ServerLevel level = DimensionBridge.level(mcc$server, dimension);
        return level == null ? null : (WorldServer) WorldServer.mcc$wrap(level);
    }

    public java.io.File getFile(String name) {
        return new java.io.File(mcc$server.getServerDirectory().toFile(), name);
    }

    /** The name of the world (the level name; the folder name if there is none). */
    public String getWorldName() {
        String name = mcc$server.getWorldData().getLevelName();
        return name == null || name.isEmpty() ? getFolderName() : name;
    }

    /** The world's folder ("world"): the root path is "world/.", so it is the normalised path's own name, not its parent's. */
    public String getFolderName() {
        var root = mcc$server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize();
        return root.getFileName() == null ? "world" : root.getFileName().toString();
    }

    public boolean isServerInOnlineMode() {
        return mcc$server.usesAuthentication();
    }

    public static long getSystemTimeMillis() {
        return System.nanoTime() / 1000000L;
    }

    public boolean isCommandBlockEnabled() {
        return mcc$server.isCommandBlockEnabled();
    }

    public com.mojang.authlib.minecraft.MinecraftSessionService getMinecraftSessionService() {
        return mcc$server.getSessionService();
    }

    public int getTickCounter() {
        return mcc$server.getTickCount();
    }

    public boolean isDedicatedServer() {
        return mcc$server.isDedicatedServer();
    }

    public int getCurrentPlayerCount() {
        return mcc$server.getPlayerCount();
    }

    public int getMaxPlayers() {
        return mcc$server.getMaxPlayers();
    }

    public String[] getAllUsernames() {
        return mcc$server.getPlayerNames();
    }

    public String getServerOwner() {
        return mcc$server.getSingleplayerProfile() == null ? null : mcc$server.getSingleplayerProfile().getName();
    }

    public String getMOTD() {
        return mcc$server.getMotd();
    }

    public boolean isServerRunning() {
        return mcc$server.isRunning();
    }

    public boolean isSinglePlayer() {
        return mcc$server.isSingleplayer();
    }

    public String getServerModName() {
        return "fml,forge,legacyconnector";
    }

    public String getMinecraftVersion() {
        return "1.7.10";
    }

    public void initiateShutdown() {
        mcc$server.halt(false);
    }

    @Override
    public void run() {
    }

    // ------------------------------------------------------------------ ICommandSender (the console)

    @Override
    public String getCommandSenderName() {
        return "Server";
    }

    @Override
    public IChatComponent getFormattedCommandSenderName() {
        return new ChatComponentText(getCommandSenderName());
    }

    @Override
    public void addChatMessage(IChatComponent message) {
        mcc$server.sendSystemMessage(message.mcc$toModern());
    }

    @Override
    public boolean canCommandSenderUseCommand(int level, String command) {
        return true;
    }

    @Override
    public ChunkCoordinates getPlayerCoordinates() {
        return new ChunkCoordinates(0, 0, 0);
    }

    @Override
    public World getEntityWorld() {
        return worldServerForDimension(0);
    }
    public boolean isPVPEnabled() {
        return mcc$server.isPvpAllowed();
    }

    public boolean getAllowNether() {
        return mcc$server.getLevel(net.minecraft.world.level.Level.NETHER) != null;
    }

    public boolean isServerStopped() {
        return mcc$server.isStopped();
    }

    public legacyconnector.v1_7_10.net.minecraft.server.management.PlayerProfileCache getPlayerProfileCache() {
        return new legacyconnector.v1_7_10.net.minecraft.server.management.PlayerProfileCache(mcc$modern());
    }
}
