package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * What 1.20.1 code could reach from anywhere and 1.21.1 hands over as a parameter: the registry access ({@code HolderLookup.Provider}) of
 * {@code load}/{@code save}/{@code getUpdateTag}, the {@code SynchedEntityData.Builder} of {@code defineSynchedData}. The bridge methods the loader
 * generates for these overrides call {@link #remember(Object)} with the real argument first, so the old-signature code (and the helpers it calls) find it
 * again; a call with nothing remembered falls back to the running server's, or on a client the connected level's, registry access.
 */
public final class MigrateContext {
    private static final ThreadLocal<HolderLookup.Provider> PROVIDER = new ThreadLocal<>();
    private static final ThreadLocal<SynchedEntityData.Builder> BUILDER = new ThreadLocal<>();
    private static volatile HolderLookup.Provider last;

    private MigrateContext() {}

    /** Called first thing by a generated bridge method with the new parameter that carries context. */
    public static void remember(Object o) {
        if (o instanceof HolderLookup.Provider p) {
            PROVIDER.set(p);
            last = p;
        } else if (o instanceof SynchedEntityData.Builder b) {
            BUILDER.set(b);
        }
    }

    /** The registry access to use for NBT and component conversion. Never null once a server or a client level exists; a vanilla-only empty one before. */
    public static HolderLookup.Provider provider() {
        HolderLookup.Provider p = PROVIDER.get();
        if (p != null) return p;
        return registries();
    }

    public static RegistryAccess registries() {
        HolderLookup.Provider p = PROVIDER.get();
        if (p instanceof RegistryAccess ra) return ra;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) return server.registryAccess();
        if (FMLEnvironment.dist.isClient()) {
            RegistryAccess ra = Client.registries();
            if (ra != null) return ra;
        }
        if (last instanceof RegistryAccess ra) return ra;
        return RegistryAccess.EMPTY;
    }

    /** The builder of the {@code defineSynchedData} call in progress on this thread; a throwaway when called outside one. */
    public static SynchedEntityData.Builder builder() {
        SynchedEntityData.Builder b = BUILDER.get();
        if (b == null) throw new IllegalStateException("SynchedEntityData.define called outside defineSynchedData");
        return b;
    }

    /** Client-only classes stay out of this class's linkage. */
    private static final class Client {
        static RegistryAccess registries() {
            var mc = net.minecraft.client.Minecraft.getInstance();
            return mc.level != null ? mc.level.registryAccess() : (mc.getConnection() != null ? mc.getConnection().registryAccess() : null);
        }
    }
}
