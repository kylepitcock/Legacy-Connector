package mccompat.v1_7_10.cpw.mods.fml.common;

import mccompat.v1_7_10.cpw.mods.fml.common.eventhandler.EventBus;
import mccompat.v1_7_10.cpw.mods.fml.relauncher.Side;
import mccompat.v1_7_10.net.minecraft.server.MinecraftServer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class FMLCommonHandler {
    private static final FMLCommonHandler INSTANCE = new FMLCommonHandler();
    /** The "FML bus" (ticks, player login/logout, crafting) - separate from MinecraftForge.EVENT_BUS in 1.7.10. */
    private final EventBus bus = new EventBus("1.7.10-fml");

    public static FMLCommonHandler instance() {
        return INSTANCE;
    }

    public EventBus bus() {
        return bus;
    }

    public Side getSide() {
        return FMLEnvironment.dist.isClient() ? Side.CLIENT : Side.SERVER;
    }

    /** Logical side of the calling thread: the integrated server thread counts as SERVER. */
    public Side getEffectiveSide() {
        Thread t = Thread.currentThread();
        String name = t.getName();
        if (name.equals("Server thread") || name.startsWith("Netty Server IO") || name.startsWith("Netty Epoll Server IO")
                || name.startsWith("Worker-Main") && !FMLEnvironment.dist.isClient()) {
            return Side.SERVER;
        }
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.isSameThread()) return Side.SERVER;
        return getSide();
    }

    public MinecraftServer getMinecraftServerInstance() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : MinecraftServer.mcc$wrap(server);
    }

    /** A legacy mod giving up must not end the game: it fails (and is disabled) instead. */
    public void exitJava(int exitCode, boolean hardExit) {
        throw org.kylepitcock.mccompat.compat.jdk.JdkCompat.exitRequested(exitCode);
    }

    public void raiseException(Throwable exception, String message, boolean stopGame) {
        FMLLog.log(org.apache.logging.log4j.Level.ERROR, exception, "Legacy mod raised: %s", message);
        if (stopGame) throw new RuntimeException(message, exception);
    }

    public String getModName() {
        return "fml,forge,mccompat";
    }

    public ModContainer findContainerFor(Object mod) {
        for (ModContainer c : Loader.instance().getModList()) {
            if (c.getMod() == mod) return c;
        }
        // Mods also pass their id, or an instance created before the @Instance field was injected.
        for (ModContainer c : Loader.instance().getModList()) {
            if (mod instanceof String id && id.equalsIgnoreCase(c.getModId())) return c;
            if (mod != null && c.getMod() != null && c.getMod().getClass() == mod.getClass()) return c;
        }
        return org.kylepitcock.mccompat.v1_7_10.LegacyModContainer1710.active();
    }
}
