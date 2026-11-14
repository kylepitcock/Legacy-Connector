package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Balm 7 (1.20.1) networking over {@link LegacyLibChannel}. Balm 21 only registers and sends {@code CustomPacketPayload}s; a 1.20.1 mod registers
 * {@code (id, class, encoder, decoder, handler)} and sends plain message objects, which is what these helpers accept (the receiver, Balm's
 * {@code BalmNetworking}, is only passed through: it is no compile dependency of the layer).
 */
public final class BalmNetworkCompat {
    private BalmNetworkCompat() {}

    /** {@code Balm.initialize(modId, init)} of Balm 7: Balm 21 wants the load context of the mod (its mod event bus, wrapped by Balm's {@code NeoForgeLoadContext}). */
    public static void initializeBalm(String modId, Runnable init) {
        initialize("net.blay09.mods.balm.api.Balm", modId, init);
    }

    /** {@code BalmClient.initialize(modId, init)}. */
    public static void initializeBalmClient(String modId, Runnable init) {
        initialize("net.blay09.mods.balm.api.client.BalmClient", modId, init);
    }

    private static void initialize(String balmClass, String modId, Runnable init) {
        try {
            ClassLoader loader = BalmNetworkCompat.class.getClassLoader();
            var bus = net.neoforged.fml.ModList.get().getModContainerById(modId).orElseThrow(() -> new IllegalStateException("Mod " + modId + " is not loaded")).getEventBus();
            Class<?> loadContext = Class.forName("net.blay09.mods.balm.api.BalmRuntimeLoadContext", true, loader);
            Object ctx = Class.forName("net.blay09.mods.balm.neoforge.NeoForgeLoadContext", true, loader).getConstructor(net.neoforged.bus.api.IEventBus.class).newInstance(bus);
            Class.forName(balmClass, true, loader).getMethod("initialize", String.class, loadContext, Runnable.class).invoke(null, modId, ctx, init);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException r) throw r;
            throw new IllegalStateException("Balm initialize failed for " + modId, e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Balm 21 is not available to initialize " + modId, e);
        }
    }

    /** {@code DefaultContainer.serialize()} of Balm 7: Balm 21's takes the registry provider (item stacks are written with components). */
    public static net.minecraft.nbt.CompoundTag containerSerialize(Object container) {
        try {
            return (net.minecraft.nbt.CompoundTag) container.getClass().getMethod("serialize", net.minecraft.core.HolderLookup.Provider.class).invoke(container, MigrateContext.provider());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Balm DefaultContainer.serialize is not available", e);
        }
    }

    /** {@code DefaultContainer.deserialize(tag)}. */
    public static void containerDeserialize(Object container, net.minecraft.nbt.CompoundTag tag) {
        try {
            container.getClass().getMethod("deserialize", net.minecraft.nbt.CompoundTag.class, net.minecraft.core.HolderLookup.Provider.class).invoke(container, tag, MigrateContext.provider());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Balm DefaultContainer.deserialize is not available", e);
        }
    }

    public static <T> void registerClientbound(Object networking, ResourceLocation id, Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, T> decoder, BiConsumer<Player, T> handler) {
        LegacyLibChannel.registerClientbound(id, type, encoder, decoder, handler);
    }

    public static <T> void registerServerbound(Object networking, ResourceLocation id, Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, T> decoder, BiConsumer<ServerPlayer, T> handler) {
        LegacyLibChannel.registerServerbound(id, type, encoder, decoder, handler);
    }

    public static void reply(Object networking, Object message) {
        LegacyLibChannel.reply(LegacyLibChannel.wrap(message));
    }

    public static void sendTo(Object networking, Player player, Object message) {
        LegacyLibChannel.sendTo(player, LegacyLibChannel.wrap(message));
    }

    public static void sendToTracking(Object networking, ServerLevel level, BlockPos pos, Object message) {
        LegacyLibChannel.sendToTracking(level, pos, LegacyLibChannel.wrap(message));
    }

    public static void sendToTracking(Object networking, Entity entity, Object message) {
        LegacyLibChannel.sendToTracking(entity, LegacyLibChannel.wrap(message));
    }

    public static void sendToAll(Object networking, MinecraftServer server, Object message) {
        LegacyLibChannel.sendToAll(server, LegacyLibChannel.wrap(message));
    }

    public static void sendToServer(Object networking, Object message) {
        LegacyLibChannel.sendToServer(LegacyLibChannel.wrap(message));
    }
}
