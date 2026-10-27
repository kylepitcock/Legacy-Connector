package legacyconnector.v1_12_2.net.minecraft.server;

import legacyconnector.v1_12_2.net.minecraft.command.CommandHandler;
import legacyconnector.v1_12_2.net.minecraft.command.ICommandManager;

/** The running server as 1.12 mods see it: what exists here is what needs no 1.12 world or player classes. */
public class MinecraftServer implements legacyconnector.v1_12_2.net.minecraft.command.ICommandSender {
    private static final MinecraftServer INSTANCE = new MinecraftServer();
    private final CommandHandler commandManager = new CommandHandler();

    public static MinecraftServer mcc$instance() {
        return INSTANCE;
    }

    public ICommandManager getCommandManager() {
        return commandManager;
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
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.wrap(server.overworld());
    }

    @Override
    public MinecraftServer getServer() {
        return this;
    }

    public boolean isDedicatedServer() {
        return !net.neoforged.fml.loading.FMLEnvironment.dist.isClient();
    }

    public boolean isSinglePlayer() {
        return net.neoforged.fml.loading.FMLEnvironment.dist.isClient();
    }

    public String getMinecraftVersion() {
        return "1.12.2";
    }

    /** The game's directory (the server directory; the client's game directory in single player). */
    public java.io.File getDataDirectory() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server == null ? net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().toFile() : server.getServerDirectory().toFile();
    }

    private static net.minecraft.server.MinecraftServer mcc$modern() {
        return net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
    }

    private legacyconnector.v1_12_2.net.minecraft.server.management.PlayerList mcc$playerList;
    private net.minecraft.server.MinecraftServer mcc$playerListServer;

    public legacyconnector.v1_12_2.net.minecraft.server.management.PlayerList getPlayerList() {
        var server = mcc$modern();
        if (server == null) return null;
        if (mcc$playerListServer != server) {
            mcc$playerList = new legacyconnector.v1_12_2.net.minecraft.server.management.PlayerList(server);
            mcc$playerListServer = server;
        }
        return mcc$playerList;
    }

    public legacyconnector.v1_12_2.net.minecraft.world.WorldServer getWorld(int dimension) {
        var level = org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.levelOf(mcc$modern(), dimension);
        if (level == null) level = mcc$modern() == null ? null : mcc$modern().overworld();
        return (legacyconnector.v1_12_2.net.minecraft.world.WorldServer) org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.wrap(level);
    }

    public com.google.common.util.concurrent.ListenableFuture<Object> addScheduledTask(Runnable task) {
        var future = com.google.common.util.concurrent.ListenableFutureTask.<Object>create(task, null);
        var server = mcc$modern();
        if (server == null || server.isSameThread()) future.run();
        else server.execute(future);
        return future;
    }

    public <V> com.google.common.util.concurrent.ListenableFuture<V> callFromMainThread(java.util.concurrent.Callable<V> task) {
        var future = com.google.common.util.concurrent.ListenableFutureTask.create(task);
        var server = mcc$modern();
        if (server == null || server.isSameThread()) future.run();
        else server.execute(future);
        return future;
    }

    public boolean isCallingFromMinecraftThread() {
        var server = mcc$modern();
        return server == null || server.isSameThread();
    }

    public boolean isServerRunning() {
        var server = mcc$modern();
        return server != null && server.isRunning();
    }

    public int getCurrentPlayerCount() {
        var server = mcc$modern();
        return server == null ? 0 : server.getPlayerCount();
    }

    public int getMaxPlayers() {
        var server = mcc$modern();
        return server == null ? 0 : server.getMaxPlayers();
    }

    public String[] getOnlinePlayerNames() {
        var server = mcc$modern();
        return server == null ? new String[0] : server.getPlayerNames();
    }

    public int getTickCounter() {
        var server = mcc$modern();
        return server == null ? 0 : server.getTickCount();
    }

    public String getMOTD() {
        var server = mcc$modern();
        return server == null ? "" : server.getMotd();
    }

    public String getFolderName() {
        var server = mcc$modern();
        return server == null ? "world" : server.getWorldData().getLevelName();
    }

    public boolean isHardcore() {
        var server = mcc$modern();
        return server != null && server.isHardcore();
    }

    public boolean getAllowNether() {
        var server = mcc$modern();
        return server == null || server.getLevel(net.minecraft.world.level.Level.NETHER) != null;
    }

    public boolean isPVPEnabled() {
        var server = mcc$modern();
        return server == null || server.isPvpAllowed();
    }

    public boolean isFlightAllowed() {
        var server = mcc$modern();
        return server != null && server.isFlightAllowed();
    }

    public int getOpPermissionLevel() {
        var server = mcc$modern();
        return server == null ? 4 : server.getOperatorUserPermissionLevel();
    }

    public com.mojang.authlib.minecraft.MinecraftSessionService getMinecraftSessionService() {
        var server = mcc$modern();
        return server == null ? null : server.getSessionService();
    }

    public legacyconnector.v1_12_2.net.minecraft.entity.Entity getEntityFromUuid(java.util.UUID uuid) {
        var server = mcc$modern();
        if (server == null || uuid == null) return null;
        for (var level : server.getAllLevels()) {
            var e = level.getEntity(uuid);
            if (e != null) return org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(e);
        }
        return null;
    }
}
