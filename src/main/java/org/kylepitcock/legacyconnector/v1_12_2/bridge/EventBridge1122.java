package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.EventBus;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.gameevent.PlayerEvent;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.gameevent.TickEvent;
import legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Feeds NeoForge's ticks and FML player events into the 1.12.2 bus ({@code MinecraftForge.EVENT_BUS}, which FML's bus was merged
 * into in 1.12). Legacy events are only built when a 1.12 handler listens for them.
 */
public final class EventBridge1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/1.12.2");
    private static boolean registered;

    private EventBridge1122() {}

    private static EventBus bus() {
        return MinecraftForge.EVENT_BUS;
    }

    private static boolean wants(Class<? extends Event> type) {
        return bus().mcc$hasListeners(type);
    }

    private static void post(Event event) {
        try {
            bus().post(event);
        } catch (RuntimeException | LinkageError e) {
            // the bus logs handler failures itself; a failure building the event must not break the modern tick
            LOG.debug("1.12.2 event {} failed: {}", event.getClass().getSimpleName(), e.toString());
        }
    }

    /** The legacy player standing for a modern one, or null when it has no legacy player view (yet). */
    private static EntityPlayer player(net.minecraft.world.entity.player.Player modern) {
        return EntityBridge1122.wrap(modern) instanceof EntityPlayer p ? p : null;
    }

    /** The 1.12 numeric id of a dimension (the same table the 1.12 network channels use). */
    public static int dimensionId(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> key) {
        return legacyconnector.v1_12_2.net.minecraftforge.common.DimensionManager.mcc$idOf(key);
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Pre e) -> {
            if (wants(TickEvent.ServerTickEvent.class)) post(new TickEvent.ServerTickEvent(TickEvent.Phase.START));
        });
        game.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) -> {
            if (wants(TickEvent.ServerTickEvent.class)) post(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
        });
        game.addListener((net.neoforged.neoforge.event.tick.LevelTickEvent.Pre e) -> worldTick(e.getLevel(), TickEvent.Phase.START));
        game.addListener((net.neoforged.neoforge.event.tick.LevelTickEvent.Post e) -> worldTick(e.getLevel(), TickEvent.Phase.END));
        game.addListener((net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre e) -> playerTick(e.getEntity(), TickEvent.Phase.START));
        game.addListener((net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) -> playerTick(e.getEntity(), TickEvent.Phase.END));

        game.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) -> {
            EntityPlayer p;
            if (wants(PlayerEvent.PlayerLoggedInEvent.class) && (p = player(e.getEntity())) != null) post(new PlayerEvent.PlayerLoggedInEvent(p));
        });
        game.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) -> {
            EntityPlayer p;
            if (wants(PlayerEvent.PlayerLoggedOutEvent.class) && (p = player(e.getEntity())) != null) post(new PlayerEvent.PlayerLoggedOutEvent(p));
        });
        game.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent e) -> {
            EntityPlayer p;
            if (wants(PlayerEvent.PlayerRespawnEvent.class) && (p = player(e.getEntity())) != null) {
                post(new PlayerEvent.PlayerRespawnEvent(p, e.isEndConquered()));
            }
        });
        game.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent e) -> {
            EntityPlayer p;
            if (wants(PlayerEvent.PlayerChangedDimensionEvent.class) && (p = player(e.getEntity())) != null) {
                post(new PlayerEvent.PlayerChangedDimensionEvent(p, dimensionId(e.getFrom()), dimensionId(e.getTo())));
            }
        });
        game.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent e) -> {
            EntityPlayer p;
            if (wants(PlayerEvent.ItemCraftedEvent.class) && (p = player(e.getEntity())) != null) {
                post(new PlayerEvent.ItemCraftedEvent(p, Stacks1122.fromModern(e.getCrafting()), null));
            }
        });
        game.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemSmeltedEvent e) -> {
            EntityPlayer p;
            if (wants(PlayerEvent.ItemSmeltedEvent.class) && (p = player(e.getEntity())) != null) {
                post(new PlayerEvent.ItemSmeltedEvent(p, Stacks1122.fromModern(e.getSmelting())));
            }
        });
        game.addListener((net.neoforged.neoforge.event.AnvilUpdateEvent e) -> {
            if (!wants(legacyconnector.v1_12_2.net.minecraftforge.event.AnvilUpdateEvent.class)) return;
            var legacy = new legacyconnector.v1_12_2.net.minecraftforge.event.AnvilUpdateEvent(Stacks1122.fromModern(e.getLeft()),
                    Stacks1122.fromModern(e.getRight()), e.getName(), (int) e.getCost());
            post(legacy);
            if (legacy.isCanceled()) {
                e.setCanceled(true);
            } else if (legacy.getOutput() != null && !legacy.getOutput().isEmpty()) {
                e.setOutput(Stacks1122.toModern(legacy.getOutput()));
                e.setCost(legacy.getCost());
                e.setMaterialCost(legacy.getMaterialCost());
            }
        });
        game.addListener((net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent e) -> {
            if (e.getItemStack().isEmpty() || !wants(legacyconnector.v1_12_2.net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent.class)) return;
            var legacy = new legacyconnector.v1_12_2.net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent(Stacks1122.fromModern(e.getItemStack()),
                    e.getBurnTime());
            post(legacy);
            if (legacy.isCanceled()) e.setBurnTime(legacy.getBurnTime());
        });
        game.addListener((net.neoforged.neoforge.event.ServerChatEvent e) -> {
            EntityPlayer p;
            if (!wants(legacyconnector.v1_12_2.net.minecraftforge.event.ServerChatEvent.class) || !((p = player(e.getPlayer())) instanceof EntityPlayerMP mp)) return;
            var component = new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation("chat.type.text", p.getName(), e.getRawText());
            var legacy = new legacyconnector.v1_12_2.net.minecraftforge.event.ServerChatEvent(mp, e.getRawText(), component);
            post(legacy);
            if (legacy.isCanceled()) e.setCanceled(true);
            else if (legacy.getComponent() != component && legacy.getComponent() != null) e.setMessage(legacy.getComponent().mcc$modern());
        });
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) Client.register(game);
    }

    private static void worldTick(net.minecraft.world.level.Level level, TickEvent.Phase phase) {
        // like 1.12, server worlds only: the client had ClientTickEvent
        if (level.isClientSide() || !wants(TickEvent.WorldTickEvent.class)) return;
        post(new TickEvent.WorldTickEvent(Side.SERVER, phase, WorldBridge1122.wrap(level)));
    }

    private static void playerTick(net.minecraft.world.entity.player.Player modern, TickEvent.Phase phase) {
        if (!wants(TickEvent.PlayerTickEvent.class)) return;
        EntityPlayer p = player(modern);
        if (p != null) post(new TickEvent.PlayerTickEvent(phase, p));
    }

    /** Client ticks; only loaded on the client. */
    private static final class Client {
        static void register(IEventBus game) {
            game.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Pre e) -> {
                if (wants(TickEvent.ClientTickEvent.class)) post(new TickEvent.ClientTickEvent(TickEvent.Phase.START));
            });
            game.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post e) -> {
                if (wants(TickEvent.ClientTickEvent.class)) post(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
            });
            game.addListener((net.neoforged.neoforge.client.event.RenderFrameEvent.Pre e) -> {
                if (wants(TickEvent.RenderTickEvent.class)) {
                    post(new TickEvent.RenderTickEvent(TickEvent.Phase.START, e.getPartialTick().getGameTimeDeltaPartialTick(false)));
                }
            });
            game.addListener((net.neoforged.neoforge.client.event.RenderFrameEvent.Post e) -> {
                if (wants(TickEvent.RenderTickEvent.class)) {
                    post(new TickEvent.RenderTickEvent(TickEvent.Phase.END, e.getPartialTick().getGameTimeDeltaPartialTick(false)));
                }
            });
        }
    }
}
