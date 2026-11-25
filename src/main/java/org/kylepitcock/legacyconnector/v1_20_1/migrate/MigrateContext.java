package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * What 1.20.1 code could reach from anywhere and 1.21.1 hands over as a parameter: the registry access ({@code HolderLookup.Provider}) of
 * {@code load}/{@code save}/{@code getUpdateTag}. Generated bridges call {@link #remember(Object)} with the real provider first.
 * Entity data construction has its own scoped context, shared by translated overrides and injection handlers.
 */
public final class MigrateContext {
    /** A remembered provider and the world generation it belongs to (a ThreadLocal of another thread cannot be cleared from here). */
    private record Stamped(HolderLookup.Provider provider, int generation) {}

    private static final ThreadLocal<Stamped> PROVIDER = new ThreadLocal<>();
    /** Bumped when a world is left: what was remembered before belongs to the registries of that world (issue 113's class of defect). */
    private static volatile int generation;
    private static final ThreadLocal<EntityDataScope> ENTITY_DATA = new ThreadLocal<>();
    private static final class EntityDataScope {
        final Entity entity;
        final SynchedEntityData.Builder builder;
        final EntityDataScope parent;
        SynchedEntityData view;
        boolean legacy;

        EntityDataScope(Entity entity, SynchedEntityData.Builder builder, EntityDataScope parent) {
            this.entity = entity;
            this.builder = builder;
            this.parent = parent;
        }
    }
    private static volatile HolderLookup.Provider last;
    /** The last full registry access remembered (a later provider that only wraps registry lookups must not hide it). */
    private static volatile RegistryAccess lastAccess;

    static {
        org.kylepitcock.legacyconnector.core.WorldStateReset.onWorldLeft(MigrateContext::reset);
    }

    private MigrateContext() {}

    /** Forgets the registry access of the world that was just left, on every thread (the next world remembers its own). */
    public static void reset() {
        generation++;
        last = null;
        lastAccess = null;
    }

    private static HolderLookup.Provider current() {
        Stamped s = PROVIDER.get();
        if (s == null) return null;
        if (s.generation() != generation) {
            PROVIDER.remove();
            return null;
        }
        return s.provider();
    }

    /** Called first thing by a generated bridge method with the new parameter that carries context. */
    public static void remember(Object o) {
        if (o instanceof HolderLookup.Provider p) {
            // the empty access of a call before any registries existed must not hide the real one remembered earlier
            if (p instanceof RegistryAccess empty && empty.registries().findAny().isEmpty()) return;
            PROVIDER.set(new Stamped(p, generation));
            last = p;
            if (p instanceof RegistryAccess ra) lastAccess = ra;
        } else if (o instanceof SynchedEntityData.Builder builder) {
            EntityDataScope scope = ENTITY_DATA.get();
            if (scope != null && scope.builder == builder) scope.legacy = true;
        }
    }

    /** The registry access to use for NBT and component conversion. Never null once a server or a client level exists; a vanilla-only empty one before. */
    public static HolderLookup.Provider provider() {
        HolderLookup.Provider p = current();
        if (p != null) return p;
        return registries();
    }

    public static RegistryAccess registries() {
        HolderLookup.Provider p = current();
        if (p instanceof RegistryAccess ra) return ra;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) return server.registryAccess();
        if (FMLEnvironment.dist.isClient()) {
            RegistryAccess ra = Client.registries();
            if (ra != null) return ra;
        }
        if (last instanceof RegistryAccess ra) return ra;
        RegistryAccess remembered = lastAccess;
        return remembered != null ? remembered : RegistryAccess.EMPTY;
    }

    public static void withEntityData(Entity entity, SynchedEntityData.Builder builder, Runnable callback) {
        EntityDataScope previous = ENTITY_DATA.get();
        ENTITY_DATA.set(new EntityDataScope(entity, builder, previous));
        try {
            callback.run();
        } finally {
            if (previous == null) ENTITY_DATA.remove();
            else ENTITY_DATA.set(previous);
        }
    }

    /** A live view over the slots already defined by this entity's builder. */
    public static SynchedEntityData getEntityData(Entity entity) {
        return entityData(entity, entity.getEntityData());
    }

    public static SynchedEntityData entityData(Entity entity, SynchedEntityData data) {
        if (data != null || entity == null) return data;
        for (EntityDataScope scope = ENTITY_DATA.get(); scope != null; scope = scope.parent) {
            if (scope.entity != entity || !scope.legacy) continue;
            if (scope.view == null) scope.view = ((OverridableBuilder) scope.builder).legacyconnector$view();
            return scope.view;
        }
        return null;
    }

    static SynchedEntityData.Builder builderForView(SynchedEntityData view) {
        for (EntityDataScope scope = ENTITY_DATA.get(); scope != null; scope = scope.parent) {
            if (scope.view == view) return scope.builder;
        }
        return null;
    }

    public static SynchedEntityData.Builder currentBuilder(Entity entity) {
        for (EntityDataScope scope = ENTITY_DATA.get(); scope != null; scope = scope.parent) {
            if (scope.entity == entity && scope.legacy) return scope.builder;
        }
        return null;
    }

    /** The builder of the innermost {@code defineSynchedData} call on this thread. */
    public static SynchedEntityData.Builder builder() {
        EntityDataScope scope = ENTITY_DATA.get();
        if (scope == null || !scope.legacy) throw new IllegalStateException("No active legacy defineSynchedData callback");
        return scope.builder;
    }

    /** Client-only classes stay out of this class's linkage. */
    private static final class Client {
        static RegistryAccess registries() {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null) return null; // unit tests and early loading: no client instance yet
            return mc.level != null ? mc.level.registryAccess() : (mc.getConnection() != null ? mc.getConnection().registryAccess() : null);
        }
    }
}
