package legacyconnector.v1_7_10.net.minecraft.server.dedicated;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.command.ICommandSender;
import legacyconnector.v1_7_10.net.minecraft.command.ServerCommand;
import legacyconnector.v1_7_10.net.minecraft.crash.CrashReport;
import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_7_10.net.minecraft.server.management.ServerConfigurationManager;

/**
 * The dedicated server, which in 1.7.10 is also the console's command sender: mods tell console commands from player ones with
 * {@code sender instanceof DedicatedServer} (MineTweaker's {@code /minetweaker reload} lets the console run it only so).
 */
public class DedicatedServer extends MinecraftServer {
    private final List<ServerCommand> mcc$pending = Collections.synchronizedList(new ArrayList<>());

    public DedicatedServer(net.minecraft.server.MinecraftServer server) {
        super(server);
    }

    @Override
    protected ServerConfigurationManager mcc$createPlayerList() {
        return new DedicatedPlayerList(this);
    }

    @Override
    public DedicatedPlayerList getConfigurationManager() {
        return (DedicatedPlayerList) super.getConfigurationManager();
    }

    public boolean isDedicatedServer() {
        return true;
    }

    /** Phantom: redirected to {@link #mcc$get$pendingCommandList} (the console's queue, one list per server). */
    @Deprecated
    public List pendingCommandList;

    public static List mcc$get$pendingCommandList(DedicatedServer s) {
        return s.mcc$pending;
    }

    /** Queues a console command line, run by {@link #executePendingCommands}. */
    public void addPendingCommand(String command, ICommandSender sender) {
        mcc$pending.add(new ServerCommand(command, sender));
    }

    /** Runs the queued console commands through the command manager (the host runs its own console queue itself). */
    public void executePendingCommands() {
        List<ServerCommand> todo;
        synchronized (mcc$pending) {
            todo = new ArrayList<>(mcc$pending);
            mcc$pending.clear();
        }
        for (ServerCommand c : todo) getCommandManager().executeCommand(c.sender, c.command);
    }

    public boolean allowSpawnMonsters() {
        return mcc$modern() == null || mcc$modern().isSpawningMonsters();
    }

    private final java.util.Map<String, String> mcc$changedProperties = new java.util.concurrent.ConcurrentHashMap<>();

    /** Sets a value of server.properties (written by the next {@link #saveProperties}; the running server keeps its settings). */
    public void setProperty(String key, Object value) {
        if (key != null && value != null) mcc$changedProperties.put(key, String.valueOf(value));
    }

    /** Writes the values set with {@link #setProperty} into server.properties (the file's other entries stay). */
    public void saveProperties() {
        Path file = Path.of("server.properties");
        java.util.Properties properties = new java.util.Properties();
        try {
            if (java.nio.file.Files.isRegularFile(file)) {
                try (var in = java.nio.file.Files.newInputStream(file)) {
                    properties.load(in);
                }
            }
            properties.putAll(mcc$changedProperties);
            try (var out = java.nio.file.Files.newOutputStream(file)) {
                properties.store(out, "Minecraft server properties");
            }
        } catch (java.io.IOException e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector").warn("Could not save server.properties: {}", e.toString());
        }
    }

    /** Adds the server's own details to a crash report. */
    public CrashReport addServerInfoToCrashReport(CrashReport report) {
        report.getCategory().addCrashSection("Is Modded", "Definitely; Server brand changed to 'legacyconnector'");
        report.getCategory().addCrashSection("Type", "Dedicated Server (map_server.txt)");
        return report;
    }
}
