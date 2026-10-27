package legacyconnector.v1_12_2.net.minecraftforge.fml.common;

import legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.EventBus;
import legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class FMLCommonHandler {
    private static final FMLCommonHandler INSTANCE = new FMLCommonHandler();

    public static FMLCommonHandler instance() {
        return INSTANCE;
    }

    /** 1.12.2 merged the FML bus into Forge's. */
    public EventBus bus() {
        return MinecraftForge.EVENT_BUS;
    }

    public Side getSide() {
        return FMLEnvironment.dist.isClient() ? Side.CLIENT : Side.SERVER;
    }

    public Side getEffectiveSide() {
        Thread t = Thread.currentThread();
        String name = t.getName();
        if (name.equals("Server thread") || name.startsWith("Netty Server IO") || name.startsWith("Netty Epoll Server IO")) return Side.SERVER;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.isSameThread()) return Side.SERVER;
        return getSide();
    }

    /** Becomes a section of the modern crash report's system details. */
    public void registerCrashCallable(ICrashCallable callable) {
        if (callable == null) return;
        String label;
        try {
            label = callable.getLabel();
        } catch (RuntimeException | LinkageError e) {
            label = callable.getClass().getName();
        }
        net.neoforged.fml.CrashReportCallables.registerCrashCallable(label == null ? callable.getClass().getName() : label, () -> {
            try {
                return callable.call();
            } catch (Throwable t) {
                return "~~ERROR~~ " + t;
            }
        });
    }

    public String getModName() {
        return "fml,forge,legacyconnector";
    }

    public legacyconnector.v1_12_2.net.minecraftforge.common.util.CompoundDataFixer getDataFixer() {
        return legacyconnector.v1_12_2.net.minecraftforge.common.util.CompoundDataFixer.INSTANCE;
    }

    /** The container of a mod given by its id or by its mod instance. */
    public ModContainer findContainerFor(Object mod) {
        var all = Loader.instance().getIndexedModList();
        if (mod instanceof String id) return all.get(id);
        for (ModContainer c : all.values()) {
            if (c.getMod() == mod) return c;
        }
        return null;
    }

    public legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer getMinecraftServerInstance() {
        return legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer.mcc$instance();
    }

    /** The thread a message handler hands its work to: the server's for a player's connection, else the client's. */
    public legacyconnector.v1_12_2.net.minecraft.util.IThreadListener getWorldThread(legacyconnector.v1_12_2.net.minecraft.network.INetHandler net) {
        if (net instanceof legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayServer || !FMLEnvironment.dist.isClient()) {
            return getMinecraftServerInstance();
        }
        return ClientThread.get();
    }

    /** Only loaded on the client. */
    private static final class ClientThread {
        static legacyconnector.v1_12_2.net.minecraft.util.IThreadListener get() {
            return legacyconnector.v1_12_2.net.minecraft.util.IThreadListener.mcc$of(net.minecraft.client.Minecraft.getInstance());
        }
    }

    public void firePlayerChangedDimensionEvent(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, int fromDim, int toDim) {
        bus().post(new legacyconnector.v1_12_2.net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent(player, fromDim, toDim));
    }

    public void firePlayerCraftingEvent(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_12_2.net.minecraft.item.ItemStack crafted, legacyconnector.v1_12_2.net.minecraft.inventory.IInventory craftMatrix) {
        bus().post(new legacyconnector.v1_12_2.net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent(player, crafted, craftMatrix));
    }

    public void firePlayerSmeltedEvent(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_12_2.net.minecraft.item.ItemStack smelted) {
        bus().post(new legacyconnector.v1_12_2.net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemSmeltedEvent(player, smelted));
    }

    public void firePlayerLoggedIn(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        bus().post(new legacyconnector.v1_12_2.net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent(player));
    }

    public void firePlayerLoggedOut(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        bus().post(new legacyconnector.v1_12_2.net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent(player));
    }

    public void firePlayerRespawnEvent(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, boolean endConquered) {
        bus().post(new legacyconnector.v1_12_2.net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent(player, endConquered));
    }

    /** A mod asking the game to quit: the modern server or client is told to stop instead of the JVM being halted under it. */
    public void exitJava(int exitCode, boolean hardExit) {
        org.slf4j.LoggerFactory.getLogger("LegacyConnector/1.12.2").warn("A 1.12.2 mod asked the game to exit with code {}", exitCode, new Throwable("exit request"));
        if (hardExit) Runtime.getRuntime().halt(exitCode);
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) server.halt(false);
        else System.exit(exitCode);
    }

    public java.util.List<String> getBrandings(boolean includeMC) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (includeMC) out.add("Minecraft 1.12.2");
        out.add("Powered by Forge 14.23.5.2860 (Legacy Connector)");
        out.add(Loader.instance().getActiveModList().size() + " legacy mods loaded");
        return out;
    }

    public org.apache.logging.log4j.Logger getFMLLogger() {
        return FMLLog.log;
    }

    public java.io.File getSavesDirectory() {
        return net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve("saves").toFile();
    }

    public void raiseException(Throwable exception, String message, boolean stopGame) {
        org.slf4j.LoggerFactory.getLogger("LegacyConnector/1.12.2").error("Something raised an exception. The message was '{}'. 'stopGame' is {}", message, stopGame,
                exception);
        if (stopGame) throw new RuntimeException(message, exception);
    }
}
