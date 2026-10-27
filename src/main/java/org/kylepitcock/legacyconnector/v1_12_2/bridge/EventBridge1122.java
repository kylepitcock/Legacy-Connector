package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.gameevent.PlayerEvent;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.gameevent.TickEvent;
import legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side;
import net.neoforged.bus.api.IEventBus;

/**
 * Posts 1.12.2's gameplay events on {@code MinecraftForge.EVENT_BUS} (FML's bus is the same one in 1.12) from the modern
 * events: ticks, players logging in and out, respawning and changing dimension, crafting and smelting, entities joining a
 * world, worlds loading, unloading and saving. Events nobody subscribed to are not built.
 */
public final class EventBridge1122 {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/Events1122");
    private static boolean registered;
    private static final java.util.Map<Class<?>, Long> LAST_WARNING = new java.util.concurrent.ConcurrentHashMap<>();

    private EventBridge1122() {}

    private static boolean wants(Class<? extends Event> type) {
        return MinecraftForge.EVENT_BUS.mcc$hasListeners(type);
    }

    /** Posts the event; a failing handler is logged (throttled by type) instead of breaking the modern tick. */
    private static boolean post(Event event) {
        try {
            return MinecraftForge.EVENT_BUS.post(event);
        } catch (RuntimeException | LinkageError e) {
            long now = System.currentTimeMillis();
            Long last = LAST_WARNING.get(event.getClass());
            if (last == null || now - last > 60_000L) {
                LAST_WARNING.put(event.getClass(), now);
                LOG.warn("A 1.12.2 {} handler failed (repeats are logged once a minute): {}", event.getClass().getSimpleName(), e.toString(), e);
            }
            return false;
        }
    }

    private static EntityPlayer player(net.minecraft.world.entity.player.Player p) {
        return EntityBridge1122.wrap(p) instanceof EntityPlayer player ? player : null;
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        IEventBus bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Pre e) -> {
            if (wants(TickEvent.ServerTickEvent.class)) post(new TickEvent.ServerTickEvent(TickEvent.Phase.START));
        });
        bus.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) -> {
            if (wants(TickEvent.ServerTickEvent.class)) post(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
        });
        bus.addListener((net.neoforged.neoforge.event.tick.LevelTickEvent.Pre e) -> worldTick(e.getLevel(), TickEvent.Phase.START));
        bus.addListener((net.neoforged.neoforge.event.tick.LevelTickEvent.Post e) -> worldTick(e.getLevel(), TickEvent.Phase.END));
        bus.addListener((net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre e) -> playerTick(e.getEntity(), TickEvent.Phase.START));
        bus.addListener((net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) -> playerTick(e.getEntity(), TickEvent.Phase.END));

        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) -> {
            if (wants(PlayerEvent.PlayerLoggedInEvent.class) && player(e.getEntity()) != null) post(new PlayerEvent.PlayerLoggedInEvent(player(e.getEntity())));
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (wants(PlayerEvent.PlayerLoggedOutEvent.class) && player(e.getEntity()) != null) post(new PlayerEvent.PlayerLoggedOutEvent(player(e.getEntity())));
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent e) -> {
            if (wants(PlayerEvent.PlayerRespawnEvent.class) && player(e.getEntity()) != null) post(new PlayerEvent.PlayerRespawnEvent(player(e.getEntity()), e.isEndConquered()));
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (!wants(PlayerEvent.PlayerChangedDimensionEvent.class) || player(e.getEntity()) == null) return;
            var server = e.getEntity().getServer();
            int from = dimension(server, e.getFrom()), to = dimension(server, e.getTo());
            post(new PlayerEvent.PlayerChangedDimensionEvent(player(e.getEntity()), from, to));
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent e) -> {
            if (wants(PlayerEvent.ItemCraftedEvent.class) && player(e.getEntity()) != null) {
                post(new PlayerEvent.ItemCraftedEvent(player(e.getEntity()), Stacks1122.fromModern(e.getCrafting()), null));
            }
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemSmeltedEvent e) -> {
            if (wants(PlayerEvent.ItemSmeltedEvent.class) && player(e.getEntity()) != null) {
                post(new PlayerEvent.ItemSmeltedEvent(player(e.getEntity()), Stacks1122.fromModern(e.getSmelting())));
            }
        });

        bus.addListener((net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e) -> {
            if (!wants(legacyconnector.v1_12_2.net.minecraftforge.event.entity.EntityJoinWorldEvent.class)) return;
            var view = EntityBridge1122.wrap(e.getEntity());
            if (view != null && post(new legacyconnector.v1_12_2.net.minecraftforge.event.entity.EntityJoinWorldEvent(view, WorldBridge1122.wrap(e.getLevel())))) {
                e.setCanceled(true);
            }
        });
        bus.addListener((net.neoforged.neoforge.event.level.LevelEvent.Load e) -> {
            if (e.getLevel() instanceof net.minecraft.world.level.Level level && wants(legacyconnector.v1_12_2.net.minecraftforge.event.world.WorldEvent.Load.class)) {
                post(new legacyconnector.v1_12_2.net.minecraftforge.event.world.WorldEvent.Load(WorldBridge1122.wrap(level)));
            }
        });
        bus.addListener((net.neoforged.neoforge.event.level.LevelEvent.Unload e) -> {
            if (e.getLevel() instanceof net.minecraft.world.level.Level level && wants(legacyconnector.v1_12_2.net.minecraftforge.event.world.WorldEvent.Unload.class)) {
                post(new legacyconnector.v1_12_2.net.minecraftforge.event.world.WorldEvent.Unload(WorldBridge1122.wrap(level)));
            }
        });
        bus.addListener((net.neoforged.neoforge.event.level.LevelEvent.Save e) -> {
            if (e.getLevel() instanceof net.minecraft.world.level.Level level && wants(legacyconnector.v1_12_2.net.minecraftforge.event.world.WorldEvent.Save.class)) {
                post(new legacyconnector.v1_12_2.net.minecraftforge.event.world.WorldEvent.Save(WorldBridge1122.wrap(level)));
            }
        });
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) Client.register(bus);
    }

    private static int dimension(net.minecraft.server.MinecraftServer server, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> key) {
        var level = server == null ? null : server.getLevel(key);
        return level == null ? 0 : WorldBridge1122.dimensionId(level);
    }

    /** 1.12.2 posted WorldTickEvent for server worlds (the client had ClientTickEvent). */
    private static void worldTick(net.minecraft.world.level.Level level, TickEvent.Phase phase) {
        if (level.isClientSide() || !wants(TickEvent.WorldTickEvent.class)) return;
        post(new TickEvent.WorldTickEvent(Side.SERVER, phase, WorldBridge1122.wrap(level)));
    }

    private static void playerTick(net.minecraft.world.entity.player.Player p, TickEvent.Phase phase) {
        if (!wants(TickEvent.PlayerTickEvent.class)) return;
        EntityPlayer player = player(p);
        if (player != null) post(new TickEvent.PlayerTickEvent(phase, player));
    }

    private static final class Client {
        static void register(IEventBus bus) {
            bus.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Pre e) -> {
                if (wants(TickEvent.ClientTickEvent.class)) post(new TickEvent.ClientTickEvent(TickEvent.Phase.START));
            });
            bus.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post e) -> {
                if (wants(TickEvent.ClientTickEvent.class)) post(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
            });
            bus.addListener((net.neoforged.neoforge.client.event.RenderFrameEvent.Pre e) -> {
                if (wants(TickEvent.RenderTickEvent.class)) post(new TickEvent.RenderTickEvent(TickEvent.Phase.START, e.getPartialTick().getGameTimeDeltaPartialTick(false)));
            });
            bus.addListener((net.neoforged.neoforge.client.event.RenderFrameEvent.Post e) -> {
                if (wants(TickEvent.RenderTickEvent.class)) post(new TickEvent.RenderTickEvent(TickEvent.Phase.END, e.getPartialTick().getGameTimeDeltaPartialTick(false)));
            });
        }
    }
}
