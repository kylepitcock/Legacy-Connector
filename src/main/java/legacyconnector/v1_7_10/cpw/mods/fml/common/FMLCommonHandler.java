package legacyconnector.v1_7_10.cpw.mods.fml.common;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.EventBus;
import legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side;
import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;
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

    public void fireKeyInput() {
        bus.post(new legacyconnector.v1_7_10.cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent());
    }

    public void fireMouseInput() {
        bus.post(new legacyconnector.v1_7_10.cpw.mods.fml.common.gameevent.InputEvent.MouseInputEvent());
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
        throw org.kylepitcock.legacyconnector.compat.jdk.JdkCompat.exitRequested(exitCode);
    }

    public void raiseException(Throwable exception, String message, boolean stopGame) {
        FMLLog.log(org.apache.logging.log4j.Level.ERROR, exception, "Legacy mod raised: %s", message);
        if (stopGame) throw new RuntimeException(message, exception);
    }

    public java.io.File getSavesDirectory() {
        return net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve("saves").toFile();
    }

    /** Crash report sections of mods are not part of the modern crash report: only logged when asked for. */
    public void registerCrashCallable(Object callable) {
    }

    /** The lines FML printed on the title screen: Minecraft, Forge and FML versions and the mod counts. */
    public java.util.List<String> getBrandings(boolean includeMC) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (includeMC) lines.add("Minecraft 1.7.10");
        lines.add("MCP 9.05");
        lines.add("FML 7.10.99.99");
        lines.add("Minecraft Forge " + legacyconnector.v1_7_10.net.minecraftforge.common.ForgeVersion.getVersion());
        int n = Loader.instance().getModList().size();
        lines.add(n + " mod" + (n == 1 ? "" : "s") + " loaded, " + n + " mod" + (n == 1 ? "" : "s") + " active");
        return lines;
    }

    // Mods fire these themselves when they craft, smelt, pick up or teleport outside the vanilla paths.
    public void firePlayerCraftingEvent(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_7_10.net.minecraft.item.ItemStack crafted, legacyconnector.v1_7_10.net.minecraft.inventory.IInventory matrix) {
        bus().post(new legacyconnector.v1_7_10.cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent(player, crafted, matrix));
    }

    public void firePlayerSmeltedEvent(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_7_10.net.minecraft.item.ItemStack smelted) {
        bus().post(new legacyconnector.v1_7_10.cpw.mods.fml.common.gameevent.PlayerEvent.ItemSmeltedEvent(player, smelted));
    }

    public void firePlayerItemPickupEvent(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem item) {
        bus().post(new legacyconnector.v1_7_10.cpw.mods.fml.common.gameevent.PlayerEvent.ItemPickupEvent(player, item));
    }

    public void firePlayerChangedDimensionEvent(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, int from, int to) {
        bus().post(new legacyconnector.v1_7_10.cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent(player, from, to));
    }

    public String getCurrentLanguage() {
        return "en_US";
    }

    public String getModName() {
        return "fml,forge,legacyconnector";
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
        return org.kylepitcock.legacyconnector.v1_7_10.LegacyModContainer1710.active();
    }

    public org.apache.logging.log4j.Logger getFMLLogger() {
        return org.apache.logging.log4j.LogManager.getLogger("FML");
    }

    public void registerCrashCallable(ICrashCallable callable) {
    }

    public void showGuiScreen(Object screen) {
        if (screen instanceof legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen gui) {
            net.minecraft.client.Minecraft.getInstance().execute(() -> legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(gui));
        }
    }
}
