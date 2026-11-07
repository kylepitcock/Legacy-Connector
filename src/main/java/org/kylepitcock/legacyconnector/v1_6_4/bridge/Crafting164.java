package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import legacyconnector.v1_6_4.cpw.mods.fml.common.ICraftingHandler;
import legacyconnector.v1_6_4.cpw.mods.fml.common.IPickupNotifier;
import legacyconnector.v1_7_10.cpw.mods.fml.common.FMLCommonHandler;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.SubscribeEvent;
import legacyconnector.v1_7_10.cpw.mods.fml.common.gameevent.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1.6.4's {@code ICraftingHandler}s and {@code IPickupNotifier}s ({@code GameRegistry.registerCraftingHandler/registerPickupHandler}), driven by the 1.7.10
 * shim's item crafted, smelted and picked-up events on the FML bus. {@code GameRegistry.onItemCrafted/onItemSmelted/onPickupNotification}, which mods
 * called themselves for custom containers, post those events.
 */
public final class Crafting164 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Crafting164");
    private static final List<ICraftingHandler> CRAFTING = new CopyOnWriteArrayList<>();
    private static final List<IPickupNotifier> PICKUP = new CopyOnWriteArrayList<>();
    private static final AtomicBoolean LISTENING = new AtomicBoolean();

    private Crafting164() {}

    public static void registerCraftingHandler(ICraftingHandler handler) {
        if (handler == null) return;
        CRAFTING.add(handler);
        listen();
    }

    public static void registerPickupHandler(IPickupNotifier handler) {
        if (handler == null) return;
        PICKUP.add(handler);
        listen();
    }

    private static void listen() {
        if (LISTENING.compareAndSet(false, true)) FMLCommonHandler.instance().bus().register(new Listener());
    }

    /** Registered handlers, for the self test. */
    public static String summary() {
        return CRAFTING.size() + " crafting handler(s), " + PICKUP.size() + " pickup notifier(s)";
    }

    /** The event bus listener; public because the bus reflects on it. */
    public static final class Listener {
        @SubscribeEvent
        public void onCrafted(PlayerEvent.ItemCraftedEvent e) {
            for (ICraftingHandler h : CRAFTING) guard("onCrafting", () -> h.onCrafting(e.player, e.crafting, e.craftMatrix));
        }

        @SubscribeEvent
        public void onSmelted(PlayerEvent.ItemSmeltedEvent e) {
            for (ICraftingHandler h : CRAFTING) guard("onSmelting", () -> h.onSmelting(e.player, e.smelting));
        }

        @SubscribeEvent
        public void onPickup(PlayerEvent.ItemPickupEvent e) {
            for (IPickupNotifier h : PICKUP) guard("notifyPickup", () -> h.notifyPickup(e.pickedUp, e.player));
        }
    }

    private static void guard(String what, Runnable call) {
        try {
            call.run();
        } catch (RuntimeException | LinkageError e) {
            LOG.error("A 1.6.4 crafting or pickup callback ({}) failed", what, e);
        }
    }
}
