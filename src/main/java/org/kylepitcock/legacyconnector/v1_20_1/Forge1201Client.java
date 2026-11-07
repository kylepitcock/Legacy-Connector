package org.kylepitcock.legacyconnector.v1_20_1;

import legacyconnector.v1_20_1.net.minecraftforge.client.ConfigScreenHandler$ConfigScreenFactory;
import legacyconnector.v1_20_1.net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import legacyconnector.v1_20_1.net.minecraftforge.client.event.RenderGuiOverlayEvent;
import legacyconnector.v1_20_1.net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import legacyconnector.v1_20_1.net.minecraftforge.event.TickEvent;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client-only part of {@link Forge1201Runtime} (kept apart so the server never loads client classes). */
final class Forge1201Client {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Forge1201");

    private Forge1201Client() {
    }

    static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(Forge1201Client::onClientTickPre);
        NeoForge.EVENT_BUS.addListener(Forge1201Client::onClientTickPost);
        modBus.addListener(Forge1201Client::onRegisterLayers);
        modBus.addListener(Forge1201Client::onClientSetup);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, Forge1201Client::onLayerPre);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, Forge1201Client::onLayerPost);
    }

    private static void onClientTickPre(ClientTickEvent.Pre e) {
        NeoForge.EVENT_BUS.post(new TickEvent.ClientTickEvent(TickEvent.Phase.START));
    }

    private static void onClientTickPost(ClientTickEvent.Post e) {
        NeoForge.EVENT_BUS.post(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
    }

    /** Forge's overlay registration event goes to the mods that listen to it, on their own mod buses. */
    private static void onRegisterLayers(RegisterGuiLayersEvent event) {
        for (String id : Forge1201Runtime.modIds()) {
            ModList.get().getModContainerById(id).ifPresent(c -> {
                try {
                    c.getEventBus().post(new RegisterGuiOverlaysEvent(event, id));
                } catch (RuntimeException ex) {
                    LOG.warn("{} failed in its overlay registration: {}", id, ex.toString());
                }
            });
        }
    }

    /** {@code ConfigScreenFactory} extension points become NeoForge's {@code IConfigScreenFactory}. */
    private static void onClientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        for (String id : Forge1201Runtime.modIds()) {
            ModList.get().getModContainerById(id).ifPresent(c -> c.getCustomExtension(ConfigScreenHandler$ConfigScreenFactory.class).ifPresent(f ->
                    c.registerExtensionPoint(IConfigScreenFactory.class, (container, parent) -> f.screenFunction().apply(Minecraft.getInstance(), parent))));
        }
    }

    private static void onLayerPre(RenderGuiLayerEvent.Pre e) {
        VanillaGuiOverlay overlay = VanillaGuiOverlay.byLayer(e.getName());
        if (overlay == null) return;
        var window = Minecraft.getInstance().getWindow();
        var old = new RenderGuiOverlayEvent.Pre(window, e.getGuiGraphics(), e.getPartialTick().getGameTimeDeltaPartialTick(false), overlay.type());
        if (NeoForge.EVENT_BUS.post(old).isCanceled()) e.setCanceled(true);
    }

    private static void onLayerPost(RenderGuiLayerEvent.Post e) {
        VanillaGuiOverlay overlay = VanillaGuiOverlay.byLayer(e.getName());
        if (overlay == null) return;
        NeoForge.EVENT_BUS.post(new RenderGuiOverlayEvent.Post(Minecraft.getInstance().getWindow(), e.getGuiGraphics(), e.getPartialTick().getGameTimeDeltaPartialTick(false), overlay.type()));
    }
}
