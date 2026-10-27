package legacyconnector.v1_12_2.net.minecraft.client;

import legacyconnector.v1_12_2.net.minecraft.client.resources.IReloadableResourceManager;
import legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager;
import legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManagerReloadListener;

/**
 * The client singleton as 1.12 mods see it. Only the parts that do not need a running 1.12 renderer exist: scheduling work on the client
 * thread, the resource manager (reload listeners are called once when it exists) and the thread checks.
 */
public class Minecraft {
    private static final Minecraft INSTANCE = new Minecraft();

    private final IReloadableResourceManager resourceManager = new legacyconnector.v1_12_2.net.minecraft.client.resources.SimpleReloadableResourceManager();

    public static Minecraft getMinecraft() {
        return INSTANCE;
    }

    public IResourceManager getResourceManager() {
        return resourceManager;
    }

    public boolean isCallingFromMinecraftThread() {
        return !net.neoforged.fml.loading.FMLEnvironment.dist.isClient() || net.minecraft.client.Minecraft.getInstance().isSameThread();
    }

    public com.google.common.util.concurrent.ListenableFuture<Object> addScheduledTask(Runnable task) {
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            net.minecraft.client.Minecraft.getInstance().execute(task);
        } else {
            task.run();
        }
        return com.google.common.util.concurrent.Futures.immediateFuture(null);
    }

    public boolean isSingleplayer() {
        return true;
    }

    public static long getSystemTime() {
        return System.currentTimeMillis();
    }

    // ------------------------------------------------------------------ rendering objects (area R: client.renderer)

    public final legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureManager renderEngine =
            new legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureManager(resourceManager);

    public legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureManager getTextureManager() {
        return renderEngine;
    }

    public legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureMap getTextureMapBlocks() {
        return legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureMap.mcc$blocks();
    }
}
