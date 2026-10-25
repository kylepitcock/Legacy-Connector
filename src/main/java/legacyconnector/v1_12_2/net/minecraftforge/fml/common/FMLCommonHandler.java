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

    public void registerCrashCallable(Object callable) {
    }

    public String getModName() {
        return "fml,forge,legacyconnector";
    }

    public legacyconnector.v1_12_2.net.minecraftforge.common.util.CompoundDataFixer getDataFixer() {
        return legacyconnector.v1_12_2.net.minecraftforge.common.util.CompoundDataFixer.INSTANCE;
    }
}
