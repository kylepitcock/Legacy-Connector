package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import legacyconnector.v1_6_4.cpw.mods.fml.common.ITickHandler;
import legacyconnector.v1_6_4.cpw.mods.fml.common.TickType;
import legacyconnector.v1_7_10.cpw.mods.fml.common.FMLCommonHandler;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.SubscribeEvent;
import legacyconnector.v1_7_10.cpw.mods.fml.common.gameevent.TickEvent;
import legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side;

/**
 * 1.6.4's tick handlers over the 1.7.10 shim's tick events on the FML bus: server, client, render, world and player ticks, START
 * and END phases to {@code tickStart} and {@code tickEnd}. A listener for a kind of tick is only added once a handler wants it (render and
 * player ticks are frequent).
 */
public final class Ticks164 {
    private static final TickDispatch DISPATCH = new TickDispatch();
    private static final AtomicBoolean SERVER = new AtomicBoolean();
    private static final AtomicBoolean CLIENT = new AtomicBoolean();
    private static final AtomicBoolean RENDER = new AtomicBoolean();
    private static final AtomicBoolean WORLD = new AtomicBoolean();
    private static final AtomicBoolean PLAYER = new AtomicBoolean();

    private Ticks164() {}

    /** The registered handlers and how often each fired (see {@link SelfTest164}). */
    public static java.util.List<String> summary() {
        return DISPATCH.summary();
    }

    public static void register(ITickHandler handler, Side side) {
        if (handler == null || side == null) return;
        DISPATCH.add(handler, side);
        for (Side s : Side.values()) {
            if (s != side) continue;
            boolean server = s == Side.SERVER;
            if (DISPATCH.wants(s, server ? TickType.SERVER : TickType.CLIENT) || DISPATCH.wants(s, TickType.GAME) || DISPATCH.wants(s, TickType.GLOBAL) || DISPATCH.wants(s, null)) listen(server ? SERVER : CLIENT, server ? new ServerListener() : new ClientListener());
            if (DISPATCH.wants(s, TickType.RENDER)) listen(RENDER, new RenderListener());
            if (DISPATCH.wants(s, TickType.WORLD) || DISPATCH.wants(s, TickType.WORLDLOAD)) listen(WORLD, new WorldListener());
            if (DISPATCH.wants(s, TickType.PLAYER)) listen(PLAYER, new PlayerListener());
        }
    }

    private static void listen(AtomicBoolean flag, Object listener) {
        if (flag.compareAndSet(false, true)) FMLCommonHandler.instance().bus().register(listener);
    }

    /** {@code GAME} and {@code GLOBAL} ticks: once per game tick of the side (FML fired them from the same loop as the client and server ticks). */
    private static void fireGame(Side side, boolean end) {
        DISPATCH.fire(side, TickType.GAME, end);
        DISPATCH.fire(side, TickType.GLOBAL, end);
    }

    // The listeners are public because the event bus reflects on them.

    public static final class ServerListener {
        @SubscribeEvent
        public void onTick(TickEvent.ServerTickEvent e) {
            boolean end = e.phase == TickEvent.Phase.END;
            if (!end) DISPATCH.beginGameTick(Side.SERVER);
            DISPATCH.fire(Side.SERVER, TickType.SERVER, end);
            fireGame(Side.SERVER, end);
        }
    }

    public static final class ClientListener {
        @SubscribeEvent
        public void onTick(TickEvent.ClientTickEvent e) {
            boolean end = e.phase == TickEvent.Phase.END;
            if (!end) DISPATCH.beginGameTick(Side.CLIENT);
            DISPATCH.fire(Side.CLIENT, TickType.CLIENT, end);
            fireGame(Side.CLIENT, end);
        }
    }

    public static final class RenderListener {
        @SubscribeEvent
        public void onTick(TickEvent.RenderTickEvent e) {
            DISPATCH.fire(Side.CLIENT, TickType.RENDER, e.phase == TickEvent.Phase.END, e.renderTickTime);
        }
    }

    public static final class WorldListener {
        /** Worlds whose first tick has been seen (the 1.6.4 "world load" tick). */
        private final Set<Object> seen = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

        @SubscribeEvent
        public void onTick(TickEvent.WorldTickEvent e) {
            boolean end = e.phase == TickEvent.Phase.END;
            if (!end && seen.add(e.world)) {
                DISPATCH.fire(e.side, TickType.WORLDLOAD, false, e.world);
                DISPATCH.fire(e.side, TickType.WORLDLOAD, true, e.world);
            }
            DISPATCH.fire(e.side, TickType.WORLD, end, e.world);
        }
    }

    public static final class PlayerListener {
        @SubscribeEvent
        public void onTick(TickEvent.PlayerTickEvent e) {
            DISPATCH.fire(e.side, TickType.PLAYER, e.phase == TickEvent.Phase.END, e.player);
        }
    }
}
